package com.tradeflow.market_service.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.server.ResponseStatusException;
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
import java.net.http.WebSocketHandshakeException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
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
    private static final String TOKEN_EXPIRY_KEY = "kite:access_token:expires_at";
    private static final String INSTRUMENTS_URL = "https://api.kite.trade/instruments/NSE";

    @Value("${kite.api.key:}")
    private String apiKey;

    private final StringRedisTemplate stringRedisTemplate;
    private final RedisTemplate<String, Object> redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final Map<Integer, String> symbolByInstrumentToken = new ConcurrentHashMap<>();
    private final Map<String, Integer> instrumentTokenBySymbol = new ConcurrentHashMap<>();
    private final Set<String> requestedSymbols = ConcurrentHashMap.newKeySet();
    private final Set<Integer> subscribedInstrumentTokens = ConcurrentHashMap.newKeySet();
    private final Object subscriptionLock = new Object();
    private final AtomicReference<WebSocket> socket = new AtomicReference<>();
    private final AtomicReference<String> connectionToken = new AtomicReference<>();
    private final AtomicReference<String> connectingToken = new AtomicReference<>();
    private final ByteArrayOutputStream binaryMessage = new ByteArrayOutputStream();
    private volatile LocalDate instrumentMasterDate;
    private volatile Instant lastInstrumentMasterAttempt = Instant.EPOCH;
    private volatile Instant lastTickAt;

    public KiteTickerService(
            StringRedisTemplate stringRedisTemplate,
            RedisTemplate<String, Object> redisTemplate,
            SimpMessagingTemplate messagingTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
        requestedSymbols.addAll(SUBSCRIBED_SYMBOLS);
    }

    @Scheduled(fixedDelay = 5_000, initialDelay = 1_000)
    public void maintainConnection() {
        if (apiKey.isBlank()) {
            return;
        }

        String accessToken = stringRedisTemplate.opsForValue().get(TOKEN_KEY);
        if (accessToken == null || accessToken.isBlank()) {
            disconnect();
            return;
        }
        String expiryTimestamp = stringRedisTemplate.opsForValue().get(TOKEN_EXPIRY_KEY);
        if (expiryTimestamp == null || Long.parseLong(expiryTimestamp) <= System.currentTimeMillis()) {
            stringRedisTemplate.delete(TOKEN_KEY);
            stringRedisTemplate.delete(TOKEN_EXPIRY_KEY);
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
                        Throwable cause = error;
                        while (cause.getCause() != null && cause != cause.getCause()) {
                            cause = cause.getCause();
                        }
                        if (cause instanceof WebSocketHandshakeException handshakeException) {
                            log.error("Kite WebSocket handshake rejected with HTTP {}.",
                                    handshakeException.getResponse().statusCode());
                        } else {
                            log.error("Kite WebSocket connection failed: {}", cause.toString());
                        }
                    } else if (socket.get() == webSocket
                            || socket.compareAndSet(null, webSocket)) {
                        connectionToken.set(accessToken);
                    } else {
                        webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "Connection superseded");
                    }
                });
    }

    public boolean isConnected() {
        return socket.get() != null && connectionToken.get() != null;
    }

    public Map<String, Object> getStreamStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("connected", isConnected());
        status.put("subscribedSymbols", requestedSymbols.size());
        status.put("mappedInstruments", instrumentTokenBySymbol.size());
        status.put("lastTickAt", lastTickAt == null ? null : lastTickAt.toString());
        return status;
    }

    public List<String> getRequestedSymbols() {
        return requestedSymbols.stream().sorted().toList();
    }

    public List<String> subscribeSymbols(List<String> symbols) {
        if (symbols == null || symbols.isEmpty()) {
            return List.of();
        }

        Set<String> normalizedSymbols = symbols.stream()
                .filter(symbol -> symbol != null && !symbol.isBlank())
                .map(symbol -> symbol.trim().toUpperCase(Locale.ROOT))
                .collect(java.util.stream.Collectors.toSet());
        synchronized (subscriptionLock) {
            Set<String> combinedSymbols = ConcurrentHashMap.newKeySet();
            combinedSymbols.addAll(requestedSymbols);
            combinedSymbols.addAll(normalizedSymbols);
            if (combinedSymbols.size() > 3000) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Kite allows at most 3000 subscribed instruments per connection.");
            }

            requestedSymbols.addAll(normalizedSymbols);
            List<Integer> newTokens = normalizedSymbols.stream()
                    .map(instrumentTokenBySymbol::get)
                    .filter(java.util.Objects::nonNull)
                    .filter(subscribedInstrumentTokens::add)
                    .toList();

            WebSocket currentSocket = socket.get();
            if (currentSocket != null && !newTokens.isEmpty()) {
                currentSocket.sendText("{\"a\":\"subscribe\",\"v\":" + newTokens + "}", true);
                currentSocket.sendText("{\"a\":\"mode\",\"v\":[\"full\"," + newTokens + "]}", true);
            }
        }

        return normalizedSymbols.stream()
                .filter(instrumentTokenBySymbol::containsKey)
                .sorted()
                .toList();
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
                Map<String, Integer> loadedSymbols = new HashMap<>();
                for (CSVRecord record : parser) {
                    if (!"NSE".equals(record.get("exchange"))
                            || !"EQ".equals(record.get("instrument_type"))) {
                        continue;
                    }
                    String symbol = record.get("tradingsymbol").trim().toUpperCase();
                    int token = Integer.parseInt(record.get("instrument_token"));
                    loadedTokens.put(token, symbol);
                    loadedSymbols.put(symbol, token);
                }

                if (loadedTokens.isEmpty()) {
                    log.warn("Kite instrument master did not contain any NSE equity symbols.");
                    return false;
                }
                symbolByInstrumentToken.clear();
                symbolByInstrumentToken.putAll(loadedTokens);
                instrumentTokenBySymbol.clear();
                instrumentTokenBySymbol.putAll(loadedSymbols);
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
        webSocket.request(1);

        List<Integer> instrumentTokens;
        synchronized (subscriptionLock) {
            subscribedInstrumentTokens.clear();
            instrumentTokens = requestedSymbols.stream()
                    .map(instrumentTokenBySymbol::get)
                    .filter(java.util.Objects::nonNull)
                    .filter(subscribedInstrumentTokens::add)
                    .toList();
            if (!instrumentTokens.isEmpty()) {
                webSocket.sendText("{\"a\":\"subscribe\",\"v\":" + instrumentTokens + "}", true);
                webSocket.sendText("{\"a\":\"mode\",\"v\":[\"full\"," + instrumentTokens + "]}", true);
            }
        }
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
            lastTickAt = Instant.now();
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
