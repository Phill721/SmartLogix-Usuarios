package com.smartlogix.usuarios.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import com.smartlogix.usuarios.service.AuditService;
import com.smartlogix.usuarios.model.TipoEvento;
import com.smartlogix.usuarios.model.EstadoIntento;
import com.smartlogix.usuarios.repository.UsuarioRepository;
import com.smartlogix.usuarios.model.Usuario;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final AuditService auditService;
    private final UsuarioRepository usuarioRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            // Rechazar tokens manipulados con header inválido (por ejemplo alg=none)
            try {
                String[] parts = token.split("\\.");
                if (parts.length != 3) {
                    String nombre = "ANONIMO";
                    try { nombre = jwtUtil.extraerNombre(token); } catch (Exception ignored) {}
                    auditService.registrarIntentoFallido(nombre, TipoEvento.INTENTO_ACCESO_NO_AUTORIZADO, EstadoIntento.FALLIDO, "Token inválido o malformado", null, null);
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido");
                    return;
                }
                String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
                if (!headerJson.contains("\"alg\":\"HS256\"")) {
                    String nombre = "ANONIMO";
                    try { nombre = jwtUtil.extraerNombre(token); } catch (Exception ignored) {}
                    auditService.registrarIntentoFallido(nombre, TipoEvento.INTENTO_ACCESO_NO_AUTORIZADO, EstadoIntento.FALLIDO, "Algoritmo de firma no permitido en token", null, null);
                    response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Algoritmo de firma no permitido");
                    return;
                }
            } catch (IllegalArgumentException e) {
                String nombre = "ANONIMO";
                try { nombre = jwtUtil.extraerNombre(token); } catch (Exception ignored) {}
                auditService.registrarIntentoFallido(nombre, TipoEvento.INTENTO_ACCESO_NO_AUTORIZADO, EstadoIntento.FALLIDO, "Token inválido (decodificación)", null, null);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido");
                return;
            }

            if (jwtUtil.validarToken(token) && SecurityContextHolder.getContext().getAuthentication() == null) {
                String nombre = jwtUtil.extraerNombre(token);
                Usuario usuario = usuarioRepository.findByNombre(nombre).orElse(null);
                if (usuario != null) {
                    if (Boolean.FALSE.equals(usuario.getEsActivo())) {
                        auditService.registrarIntentoFallido(nombre, TipoEvento.INTENTO_ACCESO_NO_AUTORIZADO, EstadoIntento.BLOQUEADO, "Token rechazado porque el usuario está desactivado", usuario.getId(), usuario.getEmail());
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Usuario desactivado");
                        return;
                    }

                    java.util.Date issuedAt = jwtUtil.extraerIssuedAt(token);
                    if (issuedAt != null && usuario.getTokensInvalidosDesde() != null && issuedAt.toInstant().isBefore(usuario.getTokensInvalidosDesde().atZone(java.time.ZoneId.systemDefault()).toInstant())) {
                        auditService.registrarIntentoFallido(nombre, TipoEvento.INTENTO_ACCESO_NO_AUTORIZADO, EstadoIntento.BLOQUEADO, "Token emitido antes de la desactivación del usuario", usuario.getId(), usuario.getEmail());
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token inválido por desactivación del usuario");
                        return;
                    }
                }
                String rol = jwtUtil.extraerRol(token);
                String permiso = jwtUtil.extraerPermiso(token);
                List<String> permisos = new ArrayList<>(jwtUtil.extraerPermisos(token));
                if (permiso != null && permisos.isEmpty()) {
                    permisos.add(permiso);
                }
                List<SimpleGrantedAuthority> authorities = new ArrayList<>();
                if (rol != null) {
                    authorities.add(new SimpleGrantedAuthority("ROLE_" + rol));
                }
                for (String permisoActual : permisos) {
                    authorities.add(new SimpleGrantedAuthority(permisoActual));
                }
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        nombre,
                        null,
                        authorities
                );
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        }

        filterChain.doFilter(request, response);
    }
}
