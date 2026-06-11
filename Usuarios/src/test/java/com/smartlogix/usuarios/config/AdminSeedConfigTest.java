package com.smartlogix.usuarios.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartlogix.usuarios.model.Rol;
import com.smartlogix.usuarios.model.Usuario;
import com.smartlogix.usuarios.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AdminSeedConfigTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @InjectMocks
    private AdminSeedConfig adminSeedConfig;

    @Test
    void seedAdminUsuario_noDebeCrearSiYaExisteAdmin() throws Exception {
        when(usuarioRepository.existsByRol(Rol.ADMINISTRADOR)).thenReturn(true);

        CommandLineRunner runner = adminSeedConfig.seedAdminUsuario("admin", "admin@smartlogix.local", "Admin123*");
        runner.run();

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void seedAdminUsuario_noDebeCrearSiNombreYaExiste() throws Exception {
        when(usuarioRepository.existsByRol(Rol.ADMINISTRADOR)).thenReturn(false);
        when(usuarioRepository.existsByNombre("admin")).thenReturn(true);

        CommandLineRunner runner = adminSeedConfig.seedAdminUsuario("admin", "admin@smartlogix.local", "Admin123*");
        runner.run();

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void seedAdminUsuario_noDebeCrearSiEmailYaExiste() throws Exception {
        when(usuarioRepository.existsByRol(Rol.ADMINISTRADOR)).thenReturn(false);
        when(usuarioRepository.existsByNombre("admin")).thenReturn(false);
        when(usuarioRepository.existsByEmail("admin@smartlogix.local")).thenReturn(true);

        CommandLineRunner runner = adminSeedConfig.seedAdminUsuario("admin", "admin@smartlogix.local", "Admin123*");
        runner.run();

        verify(usuarioRepository, never()).save(any(Usuario.class));
    }

    @Test
    void seedAdminUsuario_debeCrearAdminCuandoNoExiste() throws Exception {
        when(usuarioRepository.existsByRol(Rol.ADMINISTRADOR)).thenReturn(false);
        when(usuarioRepository.existsByNombre("admin")).thenReturn(false);
        when(usuarioRepository.existsByEmail("admin@smartlogix.local")).thenReturn(false);
        when(passwordEncoder.encode("Admin123*")).thenReturn("hash-admin");

        CommandLineRunner runner = adminSeedConfig.seedAdminUsuario("admin", "admin@smartlogix.local", "Admin123*");
        runner.run();

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        Usuario adminGuardado = captor.getValue();

        assertEquals("admin", adminGuardado.getNombre());
        assertEquals("admin@smartlogix.local", adminGuardado.getEmail());
        assertEquals("hash-admin", adminGuardado.getContrasena());
        assertEquals(Rol.ADMINISTRADOR, adminGuardado.getRol());
        assertTrue(adminGuardado.getAdminBase());
        assertTrue(adminGuardado.getEsActivo());
    }
}
