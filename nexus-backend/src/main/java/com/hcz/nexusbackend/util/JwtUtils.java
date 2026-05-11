package com.hcz.nexusbackend.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
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
                .setHeaderParam("typ", "JWT")
                .claim("userId", userId)
                .claim("globalRole", globalRole)
                .setIssuedAt(now)
                .setExpiration(expire)
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)), SignatureAlgorithm.HS256)
                .compact();
    }

    public static Map<String, Object> parse(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .build()
                .parseClaimsJws(token)
                .getBody();

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("userId", claims.get("userId", Long.class));
        map.put("globalRole", claims.get("globalRole", String.class));
        return map;
    }
}
