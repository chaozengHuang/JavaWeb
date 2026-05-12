package com.hcz.nexusbackend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

public class JwtUtils {

    private static final String SECRET = "nexus-bbs-jwt-secret-key-2024-please-change-in-production!!";
    private static final long EXPIRE_SECONDS = 7 * 24 * 3600;

    public static String generate(Long userId, String globalRole) {
        Date now = new Date();
        Date expire = new Date(now.getTime() + EXPIRE_SECONDS * 1000);

        return Jwts.builder()
                .header().type("JWT").and()
                .claim("userId", userId)
                .claim("globalRole", globalRole)
                .issuedAt(now)
                .expiration(expire)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }

    public static Map<String, Object> parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId", claims.get("userId", Long.class));
        map.put("globalRole", claims.get("globalRole", String.class));
        return map;
    }
}
