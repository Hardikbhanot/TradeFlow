package com.tradeflow.wallet_service.interceptor;

import com.tradeflow.wallet_service.service.IdempotencyService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Optional;

@Component
public class IdempotencyInterceptor implements HandlerInterceptor {

    @Autowired
    private IdempotencyService idempotencyService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        // Only apply idempotency to POST requests (mutating operations)
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String idempotencyKey = request.getHeader("Idempotency-Key");
        if (idempotencyKey == null || idempotencyKey.isEmpty()) {
            // Depending on strictness, we might reject the request if the key is missing.
            // For now, we'll allow it if they don't provide a key, or you can change this
            // to 400 Bad Request.
            return true;
        }

        Optional<String> cachedResponse = idempotencyService.getResponse(idempotencyKey);
        if (cachedResponse.isPresent()) {
            // We've seen this key before! Return the cached response instead of processing
            // again.
            response.setStatus(HttpStatus.OK.value());
            response.setContentType("application/json");
            response.getWriter().write(cachedResponse.get());

            // Return false to stop the request from reaching the controller
            return false;
        }

        // Key is new, let the request proceed to the controller
        return true;
    }
}
