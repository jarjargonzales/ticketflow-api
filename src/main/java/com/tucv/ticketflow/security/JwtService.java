package com.tucv.ticketflow.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.List;

/**
 * Validación de tokens emitidos por auth-service-jwt:
 * HS256, secreto en Base64 (propiedad jwt.secret), claim "roles"
 * (ROLE_USER / ROLE_ADMIN) y claim "type" = "access".
 */
@Service
public class JwtService {

    private static final String ROLES_CLAIM = "roles";
    private static final String TYPE_CLAIM = "type";
    private static final String ACCESS_TOKEN_TYPE = "access";

    private final SecretKey signingKey;

    public JwtService(@Value("${jwt.secret}") String base64Secret) {
        this.signingKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(base64Secret));
    }

    /**
     * Parsea y valida la firma del token. Lanza JwtException si es inválido.
     */
    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Devuelve el usuario autenticado a partir del token, o null si el token
     * no es un access token válido para esta API.
     */
    public AuthenticatedUser toAuthenticatedUser(String token) {
        try {
            Claims claims = parse(token);
            if (!ACCESS_TOKEN_TYPE.equals(claims.get(TYPE_CLAIM, String.class))) {
                return null;
            }
            String username = claims.getSubject();
            List<?> roles = claims.get(ROLES_CLAIM, List.class);
            List<SimpleGrantedAuthority> authorities = roles == null
                    ? List.of()
                    : roles.stream()
                            .map(String::valueOf)
                            .map(SimpleGrantedAuthority::new)
                            .toList();
            return new AuthenticatedUser(username, authorities);
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    public record AuthenticatedUser(String username, List<SimpleGrantedAuthority> authorities) {
    }
}
