package com.eaze.config;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtProviderTest {

    private JwtProvider jwtProvider;

    @BeforeEach
    void setUp() {
        jwtProvider = new JwtProvider();
        // HS256 requires a >= 32-byte secret
        ReflectionTestUtils.setField(jwtProvider, "secret", "test-secret-key-that-is-long-enough-1234567890");
        // invoke @PostConstruct init() manually (no Spring context in a unit test)
        ReflectionTestUtils.invokeMethod(jwtProvider, "init");
    }

    private Authentication auth(String email, String... roles) {
        return new UsernamePasswordAuthenticationToken(
                email, null, AuthorityUtils.createAuthorityList(roles));
    }

    @Test
    void generateAndParse_roundTrip_preservesEmailAndAuthorities() {
        String token = jwtProvider.generateToken(auth("user@eaze.com", "ROLE_ADMIN"));

        Claims claims = jwtProvider.parseClaims(token);
        assertEquals("user@eaze.com", claims.get("email"));
        assertTrue(String.valueOf(claims.get("authorities")).contains("ROLE_ADMIN"));
    }

    @Test
    void getEmailFromToken_handlesBearerPrefix() {
        String token = jwtProvider.generateToken(auth("user@eaze.com", "ROLE_CUSTOMER"));

        // with and without the Bearer prefix should both resolve
        assertEquals("user@eaze.com", jwtProvider.getEmailFromToken("Bearer " + token));
        assertEquals("user@eaze.com", jwtProvider.getEmailFromToken(token));
    }

    @Test
    void stripBearer_variants() {
        assertEquals("abc", JwtProvider.stripBearer("Bearer abc"));
        assertEquals("abc", JwtProvider.stripBearer("abc"));      // no prefix -> unchanged
        assertNull(JwtProvider.stripBearer(null));                // null-safe, no exception
    }

    @Test
    void parseClaims_throws_onTamperedToken() {
        String token = jwtProvider.generateToken(auth("user@eaze.com", "ROLE_CUSTOMER"));
        String tampered = token.substring(0, token.length() - 3) + "xyz";

        assertThrows(Exception.class, () -> jwtProvider.parseClaims(tampered));
    }

    @Test
    void parseClaims_throws_onTokenSignedWithDifferentSecret() {
        // token from a provider with a different secret must not verify
        JwtProvider other = new JwtProvider();
        ReflectionTestUtils.setField(other, "secret", "a-completely-different-secret-key-0987654321");
        ReflectionTestUtils.invokeMethod(other, "init");
        String foreignToken = other.generateToken(auth("attacker@eaze.com", "ROLE_ADMIN"));

        assertThrows(Exception.class, () -> jwtProvider.parseClaims(foreignToken));
    }
}
