package com.tradeflow.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final SecretKey secretKey;
    private final StringRedisTemplate redisTemplate;

    public JwtAuthenticationFilter(
            @Value("${jwt.secret:TradeFlowSuperSecretKeyForJwtAuthentication2026!}") String secretKeyString,
            StringRedisTemplate redisTemplate) {
        if (secretKeyString == null || secretKeyString.trim().length() < 32) {
            throw new IllegalArgumentException("JWT secret key must be at least 256 bits (32 characters) long.");
        }
        this.secretKey = Keys.hmacShaKeyFor(secretKeyString.getBytes());
        this.redisTemplate = redisTemplate;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            filterChain.doFilter(request, response);
            return;
        }

        String rawPath = request.getRequestURI();
        // Normalize path
        String normalizedPath = rawPath.replaceAll("/+", "/");
        if (normalizedPath.length() > 1 && normalizedPath.endsWith("/")) {
            normalizedPath = normalizedPath.substring(0, normalizedPath.length() - 1);
        }

        final List<String> publicPathPrefixes = List.of(
                "/auth",
                "/api/v1/market/ws",
                "/eureka",
                "/actuator"
        );

        final String pathToCheck = normalizedPath;
        boolean isPublicEndpoint = publicPathPrefixes.stream().anyMatch(prefix ->
                pathToCheck.equals(prefix) || pathToCheck.startsWith(prefix + "/"));

        if (!isPublicEndpoint) {
            String token = request.getHeader("Authorization");

            if (token == null) {
                onError(response, "Authorization header is missing in request");
                return;
            }

            if (!token.startsWith("Bearer ")) {
                onError(response, "Authorization header is invalid");
                return;
            }

            final String jwtToken = token.substring(7).trim();

            // Check if token is blacklisted in Redis
            String redisKey = "jwt_blacklist:" + jwtToken;
            if (Boolean.TRUE.equals(redisTemplate.hasKey(redisKey))) {
                onError(response, "Token has been invalidated (logged out)");
                return;
            }

            String userId = null;
            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(secretKey)
                        .build()
                        .parseClaimsJws(jwtToken)
                        .getBody();

                userId = claims.getSubject();

                // Block bulk export endpoints from direct client access
                if (pathToCheck.equals("/api/v1/wallets/all") || pathToCheck.equals("/api/v1/users/all")) {
                    onForbidden(response, "Bulk enumeration endpoints are restricted from public API access.");
                    return;
                }

                // IDOR Protection: Prevent users from requesting other users' resources
                java.util.regex.Pattern walletUserPattern = java.util.regex.Pattern.compile(".*/wallets/user/(\\d+)(/.*)?");
                java.util.regex.Matcher walletMatcher = walletUserPattern.matcher(pathToCheck);
                if (walletMatcher.matches()) {
                    String resourceId = walletMatcher.group(1);
                    if (!resourceId.equals(userId)) {
                        onForbidden(response, "JWT Subject mismatch: You are not authorized to access resource ID " + resourceId);
                        return;
                    }
                }

                java.util.regex.Pattern portfolioPattern = java.util.regex.Pattern.compile(".*/portfolio/(\\d+)(/.*)?");
                java.util.regex.Matcher portfolioMatcher = portfolioPattern.matcher(pathToCheck);
                if (portfolioMatcher.matches()) {
                    String resourceId = portfolioMatcher.group(1);
                    if (!resourceId.equals(userId)) {
                        onForbidden(response, "JWT Subject mismatch: You are not authorized to access resource ID " + resourceId);
                        return;
                    }
                }

            } catch (Exception e) {
                onError(response, "Authorization header is invalid: " + e.getMessage());
                return;
            }

            final String userIdFinal = userId;

            // Strip any incoming client X-User-Id header and inject the verified JWT userId
            HttpServletRequestWrapper modifiedRequest = new HttpServletRequestWrapper(request) {
                @Override
                public String getHeader(String name) {
                    if ("X-User-Id".equalsIgnoreCase(name)) {
                        return userIdFinal;
                    }
                    return super.getHeader(name);
                }

                @Override
                public Enumeration<String> getHeaders(String name) {
                    if ("X-User-Id".equalsIgnoreCase(name)) {
                        return Collections.enumeration(Collections.singletonList(userIdFinal));
                    }
                    return super.getHeaders(name);
                }

                @Override
                public Enumeration<String> getHeaderNames() {
                    List<String> names = Collections.list(super.getHeaderNames());
                    names.removeIf(n -> "X-User-Id".equalsIgnoreCase(n));
                    names.add("X-User-Id");
                    return Collections.enumeration(names);
                }
            };

            filterChain.doFilter(modifiedRequest, response);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void onError(HttpServletResponse response, String err) throws IOException {
        System.err.println("🚨 Unauthorized Access! " + err);
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Unauthorized\", \"message\": \"" + err + "\"}");
    }

    private void onForbidden(HttpServletResponse response, String err) throws IOException {
        System.err.println("🚨 Forbidden Access! " + err);
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.getWriter().write("{\"error\": \"Forbidden\", \"message\": \"" + err + "\"}");
    }
}
