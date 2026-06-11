package com.smartlogix.usuarios.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;

class JwtUtilTest {

    private static final String SECRET = "smartlogix-super-secret-key-1234567890";

    @Test
    void generarYExtraerTokenCompleto_deberiaFuncionar() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000L);

        String token = jwtUtil.generarToken(
                "juan",
                "ADMINISTRADOR",
                "ADMINISTRACION",
                List.of("ADMINISTRACION", "VENTAS")
        );

        assertNotNull(token);
        assertTrue(jwtUtil.validarToken(token));
        assertEquals("juan", jwtUtil.extraerNombre(token));
        assertEquals("ADMINISTRADOR", jwtUtil.extraerRol(token));
        assertEquals("ADMINISTRACION", jwtUtil.extraerPermiso(token));
        assertEquals(List.of("ADMINISTRACION", "VENTAS"), jwtUtil.extraerPermisos(token));
    }

    @Test
    void generarTokenConSobrecargaDePermiso_deberiaRetornarListaConUnPermiso() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000L);

        String token = jwtUtil.generarToken("ana", "USUARIO", "VISTA_TIENDA");

        assertEquals(List.of("VISTA_TIENDA"), jwtUtil.extraerPermisos(token));
    }

    @Test
    void extraerPermisos_deberiaCaerEnFallbackCuandoNoExisteClaimPermisos() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000L);

        Key key = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
        String token = Jwts.builder()
                .setSubject("mario")
                .claim("rol", "VENDEDOR")
                .claim("permiso", "VENTAS")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 60_000L))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();

        assertEquals(List.of("VENTAS"), jwtUtil.extraerPermisos(token));
    }

    @Test
    void validarToken_deberiaRetornarFalseParaTokenInvalido() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000L);

        assertFalse(jwtUtil.validarToken("token.invalido"));
    }

    @Test
    void validarToken_deberiaRetornarFalseParaTokenExpirado() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, -1L);
        String token = jwtUtil.generarToken("exp", "USUARIO", "VISTA_TIENDA");

        assertFalse(jwtUtil.validarToken(token));
    }

    @Test
    void generarRefreshToken_deberiaSerValidoYContenerSubject() {
        JwtUtil jwtUtil = new JwtUtil(SECRET, 60_000L);

        String refresh = jwtUtil.generarRefreshToken("carla");

        assertNotNull(refresh);
        assertTrue(jwtUtil.validarToken(refresh));
        assertEquals("carla", jwtUtil.extraerNombre(refresh));
    }
}
