package com.smartlogix.usuarios.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtUtil {

    private final Key key;
    private final long expiration;

    public JwtUtil(@Value("${jwt.secret}") String secret,
                   @Value("${jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiration = expiration;
    }

    public String generarToken(String nombre, String rol, String permiso) {
        return generarToken(nombre, rol, permiso, permiso != null ? List.of(permiso) : List.of());
    }

    public String generarToken(Long usuarioId, String nombre, String rol, String permiso, List<String> permisos) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + expiration);

        return Jwts.builder()
                .setSubject(nombre)
                .claim("usuarioId", usuarioId)
                .claim("rol", rol)
                .claim("role", rol)
                .claim("permiso", permiso)
                .claim("permisos", permisos)
                .setIssuedAt(ahora)
                .setExpiration(expiracion)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generarToken(String nombre, String rol, String permiso, List<String> permisos) {
        return generarToken(null, nombre, rol, permiso, permisos);
    }

    public String generarRefreshToken(String nombre) {
        Date ahora = new Date();
        Date expiracion = new Date(ahora.getTime() + 604800000L);

        return Jwts.builder()
                .setSubject(nombre)
                .setIssuedAt(ahora)
                .setExpiration(expiracion)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extraerClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean validarToken(String token) {
        try {
            Claims claims = extraerClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception ex) {
            return false;
        }
    }

    public String extraerNombre(String token) {
        return extraerClaims(token).getSubject();
    }

    public Date extraerIssuedAt(String token) {
        return extraerClaims(token).getIssuedAt();
    }

    public String extraerRol(String token) {
        Object rol = extraerClaims(token).get("rol");
        if (rol != null) {
            return rol.toString();
        }
        Object role = extraerClaims(token).get("role");
        return role != null ? role.toString() : null;
    }

    public String extraerPermiso(String token) {
        Object permiso = extraerClaims(token).get("permiso");
        return permiso != null ? permiso.toString() : null;
    }

    public List<String> extraerPermisos(String token) {
        Object permisos = extraerClaims(token).get("permisos");
        if (permisos instanceof List<?> lista) {
            return lista.stream().map(Object::toString).toList();
        }

        String permiso = extraerPermiso(token);
        return permiso != null ? List.of(permiso) : List.of();
    }
}
