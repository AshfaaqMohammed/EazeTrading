package com.eaze.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

@Component
public class JwtProvider {

    private static final String BEARER_PREFIX = "Bearer ";
    private static final long EXPIRATION_MS = 8_64_00_000L;

    @Value("${jwt.secret}")
    private String secret;

    private SecretKey key;

    @PostConstruct
    void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Authentication auth) {
        String roles = populateAuthorities(auth.getAuthorities());

        return Jwts.builder()
                .issuedAt(new Date())
                .expiration(new Date(new Date().getTime() + EXPIRATION_MS))
                .claim("email", auth.getName())
                .claim("authorities", roles)
                .signWith(key)
                .compact();
    }

    /**
     * Parses and verifies a raw (prefix-stripped) JWT and returns its claims.
     * Throws JwtException / IllegalArgumentException on invalid tokens.
     */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String getEmailFromToken(String token) {
        Claims claims = parseClaims(stripBearer(token));
        return String.valueOf(claims.get("email"));
    }

    /**
     * Safely removes the "Bearer " prefix if present. Returns the trimmed token,
     * or the original value when no prefix is present.
     */
    public static String stripBearer(String header) {
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            return header.substring(BEARER_PREFIX.length()).trim();
        }
        return header;
    }

    private static String populateAuthorities(Collection<? extends GrantedAuthority> authorities) {
        Set<String> auth = new HashSet<>();
        for (GrantedAuthority ga : authorities) {
            auth.add(ga.getAuthority());
        }
        return String.join(",", auth);
    }
}
