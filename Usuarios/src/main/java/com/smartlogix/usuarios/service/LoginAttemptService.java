package com.smartlogix.usuarios.service;

import com.smartlogix.usuarios.model.EstadoBloqueo;
import com.smartlogix.usuarios.model.LoginAttempt;
import com.smartlogix.usuarios.model.Usuario;
import com.smartlogix.usuarios.repository.LoginAttemptRepository;
import com.smartlogix.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;
    private final UsuarioRepository usuarioRepository;

    @Value("${app.security.max-login-attempts:3}")
    private int maxIntentos;

    @Value("${app.security.lock-duration-minutes:15}")
    private int duracionBloqueoMinutos;

    @Transactional
    public void registrarIntentoExitoso(Usuario usuario) {
        // Registrar intento exitoso
        LoginAttempt intento = LoginAttempt.builder()
                .usuarioId(usuario.getId())
                .nombreUsuario(usuario.getNombre())
                .exitoso(true)
                .timestamp(LocalDateTime.now())
                .build();
        loginAttemptRepository.save(intento);

        // Limpiar contadores
        usuario.setIntentosFallidos(0);
        usuario.setEstadoBloqueo(EstadoBloqueo.ACTIVO);
        usuario.setFechaBloqueoTemporal(null);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void registrarIntentoFallido(Usuario usuario, String razonFallo) {
        // Registrar intento fallido
        LoginAttempt intento = LoginAttempt.builder()
                .usuarioId(usuario.getId())
                .nombreUsuario(usuario.getNombre())
                .exitoso(false)
                .timestamp(LocalDateTime.now())
                .razonFallo(razonFallo)
                .build();
        loginAttemptRepository.save(intento);

        // Incrementar contador de intentos fallidos
        usuario.setIntentosFallidos(usuario.getIntentosFallidos() + 1);

        // Bloquear si alcanza el máximo de intentos
        if (usuario.getIntentosFallidos() >= maxIntentos) {
            usuario.setEstadoBloqueo(EstadoBloqueo.BLOQUEADO_TEMPORAL);
            usuario.setFechaBloqueoTemporal(LocalDateTime.now());
        }

        usuarioRepository.save(usuario);
    }

    @Transactional(readOnly = true)
    public boolean estaBloqueadoTemporalmente(Usuario usuario) {
        if (usuario.getEstadoBloqueo() != EstadoBloqueo.BLOQUEADO_TEMPORAL) {
            return false;
        }

        LocalDateTime fechaBloqueo = usuario.getFechaBloqueoTemporal();
        if (fechaBloqueo == null) {
            return false;
        }

        LocalDateTime ahora = LocalDateTime.now();
        LocalDateTime fechaDesbloqueo = fechaBloqueo.plusMinutes(duracionBloqueoMinutos);

        return ahora.isBefore(fechaDesbloqueo);
    }

    @Transactional
    public void desbloquearCuenta(Usuario usuario) {
        usuario.setEstadoBloqueo(EstadoBloqueo.ACTIVO);
        usuario.setIntentosFallidos(0);
        usuario.setFechaBloqueoTemporal(null);
        usuarioRepository.save(usuario);
    }

    @Transactional
    public void desbloquearAutomaticamente(Usuario usuario) {
        if (usuario.getEstadoBloqueo() == EstadoBloqueo.BLOQUEADO_TEMPORAL 
            && !estaBloqueadoTemporalmente(usuario)) {
            desbloquearCuenta(usuario);
        }
    }
}
