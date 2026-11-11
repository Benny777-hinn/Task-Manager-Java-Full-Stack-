package com.taskmanager.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * Creates and verifies JSON Web Tokens (JWTs).
 *
 * A JWT has three parts: header.payload.signature
 *  - the "payload" holds claims (here: subject = username, role).
 *  - the "signature" is created with our secret key, so the server can prove
 *    the token was not tampered with, without storing sessions.
 *
 * Because the secret is read from a config property (which itself reads an
 * environment variable), we never hardcode the secret in source code.
 */
@Service
public class JwtService {

    // HMAC-SHA key used to sign and verify tokens.
    private final SecretKey signingKey;

    // How long a token stays valid (milliseconds).
    private final long expirationMs;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.expiration-ms}") long expirationMs) {
        // Keys.hmacShaKeyFor needs at least 256 bits (32 bytes) of secret.
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    /** Build a signed token for a user. */
    public String generateToken(String username, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(username)           // "who" the token is about
                .claim("role", role)         // extra claim, handy for debugging
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey)        // signs with our secret
                .compact();
    }

    /** Read the username ("subject") from a token. Throws if invalid/expired. */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();
    }

    /** True if the token belongs to this username and is not expired. */
    public boolean isTokenValid(String token, String username) {
        try {
            Claims claims = parseClaims(token);
            return claims.getSubject().equals(username)
                    && claims.getExpiration().after(new Date());
        } catch (Exception e) {
            // Any parsing/signature/expiry error means "not valid".
            return false;
        }
    }

    /** Verify the signature and return the claims; throws if the token is bad. */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)      // checks the signature with our secret
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
