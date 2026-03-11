package com.tradeflow.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String SECRET_KEY_STRING = "TradeFlowSuperSecretKeyForJwtAuthentication2026!";
    private final SecretKey secretKey = Keys.hmacShaKeyFor(SECRET_KEY_STRING.getBytes());

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        final List<String> apiEndpointsToBeIgnored = List.of("/auth/register", "/auth/login", "/eureka");

        boolean isApiSecured = apiEndpointsToBeIgnored.stream()
                .noneMatch(uri -> request.getRequestURI().contains(uri));

        if (isApiSecured) {
            String token = request.getHeader("Authorization");

            if (token == null) {
                onError(response, "Authorization header is missing in request");
                return;
            }

            if (!token.startsWith("Bearer ")) {
                onError(response, "Authorization header is invalid");
                return;
            }

            final String jwtToken = token.substring(7);

            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(secretKey)
                        .build()
                        .parseClaimsJws(jwtToken)
                        .getBody();

                String userId = claims.getSubject();

                // 🔥 Phase 5 Part 2: Gateway Security Sub-Filter 🔥
                // Ensure users cannot access other users' specific path resources
                String path = request.getRequestURI();
                if (path.matches(".*/portfolio/\\d+") || path.matches(".*/wallets/user/\\d+")) {
                    String[] pathSegments = path.split("/");
                    String resourceId = pathSegments[pathSegments.length - 1];

                    if (!resourceId.equals(userId)) {
                        onError(response,
                                "JWT Subject mismatch: You are not authorized to access resource ID " + resourceId);
                        return;
                    }
                }

                // Create a request wrapper to add the X-User-Id header dynamically
                HttpServletRequestWrapper modifiedRequest = new HttpServletRequestWrapper(request) {
                    @Override
                    public String getHeader(String name) {
                        if ("X-User-Id".equalsIgnoreCase(name)) {
                            return userId;
                        }
                        return super.getHeader(name);
                    }

                    @Override
                    public Enumeration<String> getHeaders(String name) {
                        if ("X-User-Id".equalsIgnoreCase(name)) {
                            return Collections.enumeration(Collections.singletonList(userId));
                        }
                        return super.getHeaders(name);
                    }

                    @Override
                    public Enumeration<String> getHeaderNames() {
                        List<String> names = Collections.list(super.getHeaderNames());
                        names.add("X-User-Id");
                        return Collections.enumeration(names);
                    }
                };

                System.out.println(
                        "✅ JWT Validated successfully for UserId: " + userId + ". Forwarding request downstream.");
                filterChain.doFilter(modifiedRequest, response);
                return;

            } catch (Exception e) {
                onError(response, "Authorization header is invalid: " + e.getMessage());
                return;
            }
        }

        System.out.println("⚠️ Bypassing JWT Check for public endpoint: " + request.getRequestURI());
        filterChain.doFilter(request, response);
    }

    private void onError(HttpServletResponse response, String err) throws IOException {
        System.err.println("🚨 Unauthorized Access! " + err);
        response.sendError(HttpStatus.UNAUTHORIZED.value(), err);
    }
}
