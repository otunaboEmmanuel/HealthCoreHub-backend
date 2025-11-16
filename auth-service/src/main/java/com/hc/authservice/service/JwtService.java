package com.hc.authservice.service;

import com.hc.authservice.entity.AuthUser;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    @Value("${jwt.refresh-expiration}")
    private Long refreshExpiration;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateToken(AuthUser user, String tenantRole, String status, Integer tenantId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", user.getId().toString());
        claims.put("email", user.getEmail());
        claims.put("hospital_id", user.getHospitalId());
        claims.put("tenant_db", user.getTenantDb());
        claims.put("global_role", user.getGlobalRole());
        claims.put("tenant_role", tenantRole);
        claims.put("tenant_user_id", tenantId);
        claims.put("status", status);
        claims.put("token_type", "access");

        return Jwts.builder()
                .claims(claims)
                .subject(user.getEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    public String generateRefreshToken(AuthUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("user_id", user.getId().toString());
       claims.put("email", user.getEmail());
        claims.put("token_type", "refresh");
        return Jwts.builder()
                .subject(user.getEmail())
                .issuedAt(new Date())
                .claims(claims)
                .expiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSigningKey())
                .compact();
    }

    public Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractEmail(String token) {
        return extractClaims(token).getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            extractClaims(token);
            return true;
        } catch (JwtException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            return extractClaims(token).getExpiration().before(new Date());
        } catch (JwtException e) {
            return true;
        }
    }
    public Claims validateToken(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
    public String extractUserId(String token) {
        Claims claims = validateToken(token);
        return claims.get("user_id", String.class);
    }
    public boolean isRefreshToken(String token) {
        try {
            Claims claims = validateToken(token);
            return "refresh".equals(claims.get("token_type", String.class));
        } catch (Exception e) {
            return false;
        }
    }
    /**
     * Check if token is an access token
     */
    public boolean isAccessToken(String token) {
        try {
            Claims claims = validateToken(token);
            return "access".equals(claims.get("token_type", String.class));
        } catch (Exception e) {
            return false;
        }
    }
}