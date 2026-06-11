package com.smartlogix.usuarios.config;

import com.smartlogix.usuarios.model.Rol;
import com.smartlogix.usuarios.model.Usuario;
import com.smartlogix.usuarios.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdminSeedConfig {

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    @Bean
    CommandLineRunner seedAdminUsuario(
            @Value("${app.seed.admin.nombre:admin}") String adminNombre,
            @Value("${app.seed.admin.email:admin@smartlogix.local}") String adminEmail,
            @Value("${app.seed.admin.contrasena:Admin123*}") String adminContrasena
    ) {
        return args -> {
            if (usuarioRepository.existsByRol(Rol.ADMINISTRADOR)) {
                return;
            }

            if (usuarioRepository.existsByNombre(adminNombre)) {
                log.warn("No se creó el admin por defecto porque el nombre '{}' ya existe con otro rol.", adminNombre);
                return;
            }

            if (usuarioRepository.existsByEmail(adminEmail)) {
                log.warn("No se creó el admin por defecto porque el email '{}' ya existe.", adminEmail);
                return;
            }

            Usuario admin = Usuario.builder()
                    .nombre(adminNombre)
                    .email(adminEmail)
                    .contrasena(passwordEncoder.encode(adminContrasena))
                    .rol(Rol.ADMINISTRADOR)
                    .adminBase(true)
                    .esActivo(true)
                    .build();

            usuarioRepository.save(admin);
            log.info("Usuario administrador por defecto creado: {}", adminNombre);
        };
    }
}
