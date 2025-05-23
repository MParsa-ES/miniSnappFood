package util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.UUID; // ۱. این را import کنید

public class JwtUtil {

    private static final Key key = Keys.secretKeyFor(SignatureAlgorithm.HS256);

    private static final long EXPIRATION_TIME = 7 * 24 * 60 * 60 * 1000;

    public static String generateToken(String phone) {
        return Jwts.builder()
                .setSubject(phone)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .setId(UUID.randomUUID().toString())
                .signWith(key)
                .compact();
    }

    private static Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public static String validateToken(String token) {
        try {
            // استفاده از متد کمکی برای خوانایی بهتر
            return extractAllClaims(token).getSubject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String getJtiFromToken(String token) {
        try {
            return extractAllClaims(token).getId();
        } catch (Exception e) {
            return null;
        }
    }

    public static Date getExpirationDateFromToken(String token) {
        try {
            return extractAllClaims(token).getExpiration();
        } catch (Exception e) {
            return null;
        }
    }
}