package com.iwms.auth;

import com.iwms.model.User;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe in-memory session manager for authenticated HTTP sessions.
 * Generates cryptographically secure session tokens, enforces TTL expiration, and supports revocation.
 */
public class SessionManager {

    public record Session(
            String token,
            User user,
            Instant createdAt,
            Instant expiresAt
    ) {
        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final Duration sessionTtl;
    private final Map<String, Session> activeSessions = new ConcurrentHashMap<>();
    private final SecureRandom secureRandom = new SecureRandom();

    public SessionManager(Duration sessionTtl) {
        this.sessionTtl = Objects.requireNonNull(sessionTtl, "Session TTL cannot be null");
    }

    public Session createSession(User user) {
        Objects.requireNonNull(user, "User cannot be null");

        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);

        Instant now = Instant.now();
        Instant expiresAt = now.plus(sessionTtl);
        Session session = new Session(token, user, now, expiresAt);

        activeSessions.put(token, session);
        return session;
    }

    public Optional<User> validateSession(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        Session session = activeSessions.get(token);
        if (session == null) {
            return Optional.empty();
        }

        if (session.isExpired()) {
            activeSessions.remove(token);
            return Optional.empty();
        }

        return Optional.of(session.user());
    }

    public Optional<Session> getSession(String token) {
        if (token == null) return Optional.empty();
        Session session = activeSessions.get(token);
        if (session != null && session.isExpired()) {
            activeSessions.remove(token);
            return Optional.empty();
        }
        return Optional.ofNullable(session);
    }

    public void invalidateSession(String token) {
        if (token != null) {
            activeSessions.remove(token);
        }
    }

    public void purgeExpiredSessions() {
        activeSessions.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    public int getActiveSessionCount() {
        return activeSessions.size();
    }
}
