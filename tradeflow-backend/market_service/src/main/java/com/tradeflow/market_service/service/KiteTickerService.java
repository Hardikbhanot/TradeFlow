package com.tradeflow.market_service.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.zip.GZIPInputStream;

@Service
@Slf4j
public class KiteTickerService implements WebSocket.Listener {

    public static final List<String> SUBSCRIBED_SYMBOLS = List.of(
            "RELIANCE", "TCS", "HDFCBANK", "ICICIBANK", "INFY", "ITC",
            "SBIN", "BHARTIARTL", "KOTAKBANK", "LT");

    private static final String TOKEN_KEY = "kite:access_token";
    private static final String INSTRUMENTS_URL = "https://api.kite.trade/instruments/NSE";
    private static final Set<String> WANTED_SYMBOLS = Set.copyOf(SUBSCRIBED_SYMBOLS);

    @Value("${kite.api.key:}")
    private String apiKey;

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final Map<Integer, String> symbolByInstrumentToken = new ConcurrentHashMap<>();
    private final AtomicReference<WebSocket> socket = new AtomicReference<>();
    private final AtomicReference<String> connectionToken = new AtomicReference<>();
    private final AtomicReference<String> connectingToken = new AtomicReference<>();
    private final ByteArrayOutputStream binaryMessage = new ByteArrayOutputStream();
    private volatile LocalDate instrumentMasterDate;
    private volatile Instant lastInstrumentMasterAttempt = Instant.EPOCH;

    public KiteTickerService(
            StringRedisTemplate stringRedisTemplate,
            RedisTemplate<String, Object> redisTemplate,
            SimpMessagingTemplate messagingTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
    }

    @Scheduled(fixedDelay = 30_000, initialDelay = 1_000)
    public void maintainConnection() {
        if (apiKey.isBlank()) {
            return;
        }

        String accessToken = stringRedisTemplate.opsForValue().get(TOKEN_KEY);
        if (accessToken == null || accessToken.isBlank()) {
            disconnect();
            return;
        }

        if (accessToken.equals(connectionToken.get()) && socket.get() != null) {
            return;
        }
        if (accessToken.equals(connectingToken.get())) {
            return;
        }

        disconnect();
        if (!ensureInstrumentMaster(accessToken)) {
            return;
        }

        String encodedApiKey = URLEncoder.encode(apiKey, StandardCharsets.UTF_8);
        String encodedToken = URLEncoder.encode(accessToken, StandardCharsets.UTF_8);
        URI uri = URI.create("wss://ws.kite.trade?api_key=" + encodedApiKey + "&access_token=" + encodedToken);
        connectingToken.set(accessToken);

        httpClient.newWebSocketBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .buildAsync(uri, this)
                .whenComplete((webSocket, error) -> {
                    connectingToken.compareAndSet(accessToken, null);
                    if (error != null) {
                        log.error("Kite WebSocket connection failed: {}", error.getMessage());
                    } else if (webSocket != socket.get()) {
                        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Connection superseded");
                    }
                });
    }

    public boolean isConnected() {
        return socket.get() != null && connectionToken.get() != null;
    }

    private boolean ensureInstrumentMaster(String accessToken) {
        LocalDate today = LocalDate.now();
        if (today.equals(instrumentMasterDate) && !symbolByInstrumentToken.isEmpty()) {
            return true;
        }
        if (Instant.now().isBefore(lastInstrumentMasterAttempt.plus(Duration.ofMinutes(5)))) {
            return !symbolByInstrumentToken.isEmpty();
        }
        lastInstrumentMasterAttempt = Instant.now();

        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(INSTRUMENTS_URL))
                    .header("Authorization", "token " + apiKey + ":" + accessToken)
                    .header("X-Kite-Version", "3")
                    .header("Accept-Encoding", "gzip")
                    .GET()
                    .build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200) {
                log.warn("Kite instrument master request returned HTTP {}", response.statusCode());
                return !symbolByInstrumentToken.isEmpty();
            }

            byte[] content = response.body();
            try (InputStream raw = new ByteArrayInputStream(content);
                    InputStream decoded = isGzip(content) ? new GZIPInputStream(raw) : raw;
                    Reader reader = new InputStreamReader(decoded, StandardCharsets.UTF_8);
                    CSVParser parser = CSVFormat.DEFAULT.builder()
                            .setHeader()
                            .setSkipHeaderRecord(true)
                            .build()
                            .parse(reader)) {
                Map<Integer, String> loadedTokens = new HashMap<>();
                for (CSVRecord record : parser) {
                    if (!"NSE".equals(record.get("exchange"))
                            || !"EQ".equals(record.get("instrument_type"))) {
                        continue;
                    }
                    String symbol = record.get("tradingsymbol").trim().toUpperCase();
                    if (!WANTED_SYMBOLS.contains(symbol)) {
                        continue;
                    }
                    loadedTokens.put(Integer.parseInt(record.get("instrument_token")), symbol);
                }

                if (loadedTokens.isEmpty()) {
                    log.warn("Kite instrument master did not contain any subscribed NSE equity symbols.");
                    return false;
                }
                symbolByInstrumentToken.clear();
                symbolByInstrumentToken.putAll(loadedTokens);
                instrumentMasterDate = today;
                log.info("Loaded {} Kite NSE equity instrument tokens.", loadedTokens.size());
                return true;
            }
        } catch (Exception e) {
            log.error("Unable to load the Kite NSE instrument master: {}", e.getMessage());
            return !symbolByInstrumentToken.isEmpty();
        }
    }

    private boolean isGzip(byte[] bytes) {
        return bytes.length > 1 && (bytes[0] & 0xff) == 0x1f && (bytes[1] & 0xff) == 0x8b;
    }

    private void disconnect() {
        WebSocket current = socket.getAndSet(null);
        connectionToken.set(null);
        if (current != null && !current.isOutputClosed()) {
            current.sendClose(WebSocket.NORMAL_CLOSURE, "Kite session changed or expired");
        }
    }

    @Override
    public void onOpen(WebSocket webSocket) {
        socket.set(webSocket);
        connectionToken.set(connectingToken.get());
        webSocket.request(1);

        List<Integer> instrumentTokens = new ArrayList<>(symbolByInstrumentToken.keySet());
        webSocket.sendText("{\"a\":\"subscribe\",\"v\":" + instrumentTokens + "}", true);
        webSocket.sendText("{\"a\":\"mode\",\"v\":[\"full\"," + instrumentTokens + "]}", true);
        log.info("Connected to Kite ticker and subscribed to {} NSE equities.", instrumentTokens.size());
    }

    @Override
    public CompletionStage<?> onBinary(WebSocket webSocket, ByteBuffer data, boolean last) {
        synchronized (binaryMessage) {
            byte[] bytes = new byte[data.remaining()];
            data.get(bytes);
            binaryMessage.write(bytes, 0, bytes.length);
            if (last) {
                publishQuotes(binaryMessage.toByteArray());
                binaryMessage.reset();
            }
        }
        webSocket.request(1);
        return null;
    }

    private void publishQuotes(byte[] message) {
        ByteBuffer buffer = ByteBuffer.wrap(message).order(ByteOrder.BIG_ENDIAN);
        if (buffer.remaining() < Short.BYTES) {
            return;
        }

        int packetCount = Short.toUnsignedInt(buffer.getShort());
        Map<String, java.math.BigDecimal> updates = new HashMap<>();
        for (int i = 0; i < packetCount; i++) {
            if (buffer.remaining() < Short.BYTES) {
                return;
            }
            int packetLength = Short.toUnsignedInt(buffer.getShort());
            if (packetLength < 2 * Integer.BYTES || packetLength > buffer.remaining()) {
                return;
            }

            ByteBuffer packet = buffer.slice().order(ByteOrder.BIG_ENDIAN);
            packet.limit(packetLength);
            int instrumentToken = packet.getInt();
            int lastPricePaise = packet.getInt();
            buffer.position(buffer.position() + packetLength);

            String symbol = symbolByInstrumentToken.get(instrumentToken);
            if (symbol == null || lastPricePaise <= 0) {
                continue;
            }

            java.math.BigDecimal price = java.math.BigDecimal.valueOf(lastPricePaise, 2);
            redisTemplate.opsForValue().set("price:" + symbol, price.toPlainString(), Duration.ofSeconds(30));
            updates.put(symbol, price);
        }

        if (!updates.isEmpty()) {
            messagingTemplate.convertAndSend("/topic/market-data", updates);
        }
    }

    @Override
    public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
        log.debug("Received a Kite ticker text message.");
        webSocket.request(1);
        return null;
    }

    @Override
    public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
        webSocket.request(1);
        return webSocket.sendPong(message);
    }

    @Override
    public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
        if (socket.compareAndSet(webSocket, null)) {
            connectionToken.set(null);
            log.warn("Kite ticker disconnected with status {}.", statusCode);
        }
        return null;
    }

    @Override
    public void onError(WebSocket webSocket, Throwable error) {
        if (socket.compareAndSet(webSocket, null)) {
            connectionToken.set(null);
        }
        log.error("Kite ticker connection error: {}", error.getMessage());
    }
}
