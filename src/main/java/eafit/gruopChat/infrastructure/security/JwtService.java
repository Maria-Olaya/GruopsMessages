package eafit.gruopChat.infrastructure.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import eafit.gruopChat.user.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    // ===== Generar token =====
    public String generateToken(User user) {
        return Jwts.builder()
                .subject(String.valueOf(user.getUserId()))
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(getKey())
                .compact();
    }

    // ===== Extraer userId del token =====
    public Long extractUserId(String token) {
        String subject = getClaims(token).getSubject();
        return Long.valueOf(subject);
    }

    // ===== Extraer email del token =====
    public String extractEmail(String token) {
        return getClaims(token).get("email", String.class);
    }

    // ===== Validar token =====
    public boolean isTokenValid(String token) {
        try {
            getClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ===== Tiempo de expiración en segundos (para AuthResponseDTO) =====
    public long getExpirationSeconds() {
        return expirationMs / 1000;
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}