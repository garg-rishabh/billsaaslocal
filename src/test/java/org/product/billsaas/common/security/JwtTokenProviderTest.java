package org.product.billsaas.common.security;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class JwtTokenProviderTest {

    private JwtTokenProvider provider;

    @BeforeEach
    void setup() {
        provider = new JwtTokenProvider();

        // Set secret manually
        String secret = Base64.getEncoder()
                .encodeToString("my-super-secret-key-which-is-32bytes!!".getBytes());

        provider.setJwtSecret(secret);
        provider.init();
    }

    @Test
    void testValidToken() {
        String token = provider.generateAccessToken("user1", "tenant_test", "test@mail.com", "ADMIN");

        assertTrue(provider.validateToken(token));
        assertEquals("user1", provider.getUserId(token));
        assertEquals("tenant_test", provider.getTenantSchema(token));
    }

    @Test
    void testExpiredToken() throws InterruptedException {
        provider = new JwtTokenProvider() {
            {
                setJwtSecret(Base64.getEncoder()
                        .encodeToString("my-super-secret-key-which-is-32bytes!!".getBytes()));
                init();
            }

            @Override
            public String generateAccessToken(String u, String t, String e, String r) {
                return Jwts.builder()
                        .setSubject(u)
                        .setExpiration(new java.util.Date(System.currentTimeMillis() - 1000))
                        .signWith(provider.getKey())
                        .compact();
            }
        };

        String token = provider.generateAccessToken("u", "t", "e", "r");

        assertFalse(provider.validateToken(token));
    }

    @Test
    void testTamperedToken() {
        String token = provider.generateAccessToken("user1", "tenant_test", "test@mail.com", "ADMIN");

        String tampered = token + "abc";

        assertFalse(provider.validateToken(tampered));
    }

    @Test
    void testMissingClaim() {
        String token = provider.generateRefreshToken("user1");

        assertNull(provider.getTenantSchema(token));
    }
}