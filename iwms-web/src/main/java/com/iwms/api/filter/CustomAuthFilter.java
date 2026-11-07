package com.iwms.api.filter;

import com.iwms.auth.SessionManager;
import com.iwms.model.User;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

/**
 * Custom Lightweight Authentication Filter.
 * Enforces session token verification without Spring Security overhead.
 * Intercepts protected /api/* endpoints, extracts Bearer token, validates against SessionManager,
 * and binds the User entity to the request context.
 */
@Component
public class CustomAuthFilter extends OncePerRequestFilter {

    public static final String AUTH_USER_ATTR = "AUTHENTICATED_USER";
    private final SessionManager sessionManager;

    public CustomAuthFilter(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String path = request.getRequestURI();

        // 1. Whitelist unauthenticated endpoints
        if (isWhitelisted(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. Extract Bearer token from Authorization header
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            sendUnauthorized(response, "Missing or invalid Authorization header");
            return;
        }

        String token = authHeader.substring(7).trim();
        Optional<User> userOpt = sessionManager.validateSession(token);

        if (userOpt.isEmpty()) {
            sendUnauthorized(response, "Session token is invalid or has expired");
            return;
        }

        // 3. Attach authenticated user to request attributes
        request.setAttribute(AUTH_USER_ATTR, userOpt.get());
        filterChain.doFilter(request, response);
    }

    private boolean isWhitelisted(String path) {
        return path.equals("/api/auth/login") ||
                path.equals("/api/health") ||
                path.equals("/error") ||
                !path.startsWith("/api/");
    }

    private void sendUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\": \"Unauthorized\", \"message\": \"%s\"}", message));
    }
}
