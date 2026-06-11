package com.smartlogix.usuarios.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartlogix.usuarios.dto.LoginRequest;
import com.smartlogix.usuarios.dto.LoginResponse;
import com.smartlogix.usuarios.dto.UsuarioRequest;
import com.smartlogix.usuarios.dto.UsuarioResponse;
import com.smartlogix.usuarios.events.UsuarioEvent;
import com.smartlogix.usuarios.exception.ResourceNotFoundException;
import com.smartlogix.usuarios.exception.UsuarioYaExisteException;
import com.smartlogix.usuarios.model.Rol;
import com.smartlogix.usuarios.model.Usuario;
import com.smartlogix.usuarios.repository.UsuarioRepository;
import com.smartlogix.usuarios.security.JwtUtil;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceImplTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private BCryptPasswordEncoder passwordEncoder;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private JwtUtil jwtUtil;

    @InjectMocks
    private UsuarioServiceImpl usuarioService;

    private UsuarioRequest usuarioRequest;

    @BeforeEach
    void setUp() {
        usuarioRequest = UsuarioRequest.builder()
                .nombre("juan")
                .email("juan@smartlogix.com")
                .contrasena("Password123")
                .rol(Rol.USUARIO)
                .build();
    }

    @AfterEach
    void cleanSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void agregarUsuario_deberiaLanzarErrorCuandoSolicitanAdminSinPermisos() {
        UsuarioRequest adminRequest = UsuarioRequest.builder()
                .nombre("admin2")
                .email("admin2@smartlogix.com")
                .contrasena("Password123")
                .rol(Rol.ADMINISTRADOR)
                .build();

        assertThrows(AccessDeniedException.class, () -> usuarioService.agregarUsuario(adminRequest));
    }

    @Test
    void agregarUsuario_deberiaLanzarErrorSiNombreExiste() {
        when(usuarioRepository.existsByNombre("juan")).thenReturn(true);

        assertThrows(UsuarioYaExisteException.class, () -> usuarioService.agregarUsuario(usuarioRequest));
    }

    @Test
    void agregarUsuario_deberiaLanzarErrorSiEmailExiste() {
        when(usuarioRepository.existsByNombre("juan")).thenReturn(false);
        when(usuarioRepository.existsByEmail("juan@smartlogix.com")).thenReturn(true);

        assertThrows(UsuarioYaExisteException.class, () -> usuarioService.agregarUsuario(usuarioRequest));
    }

    @Test
    void agregarUsuario_deberiaGuardarYRetornarUsuario() {
        when(usuarioRepository.existsByNombre("juan")).thenReturn(false);
        when(usuarioRepository.existsByEmail("juan@smartlogix.com")).thenReturn(false);
        when(passwordEncoder.encode("Password123")).thenReturn("hash123");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> {
            Usuario u = invocation.getArgument(0);
            u.setId(10L);
            return u;
        });

        UsuarioResponse response = usuarioService.agregarUsuario(usuarioRequest);

        assertEquals(10L, response.getId());
        assertEquals("juan", response.getNombre());
        assertEquals(Rol.USUARIO, response.getRol());
        verify(eventPublisher).publishEvent(any(UsuarioEvent.class));
    }

    @Test
    void eliminarUsuario_deberiaLanzarNotFoundCuandoNoExiste() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> usuarioService.eliminarUsuario(999L));
    }

    @Test
    void eliminarUsuario_deberiaLanzarErrorSiEsAdminBase() {
        Usuario usuario = buildUsuario(1L, "admin", Rol.ADMINISTRADOR, true, true);
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));

        assertThrows(AccessDeniedException.class, () -> usuarioService.eliminarUsuario(1L));
    }

    @Test
    void eliminarUsuario_deberiaEliminarUsuarioComun() {
        Usuario usuario = buildUsuario(2L, "maria", Rol.USUARIO, false, true);
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(usuario));

        usuarioService.eliminarUsuario(2L);

        verify(usuarioRepository).delete(usuario);
        verify(eventPublisher).publishEvent(any(UsuarioEvent.class));
    }

    @Test
    void desactivarUsuario_deberiaLanzarErrorSiEsAdminBase() {
        Usuario usuario = buildUsuario(3L, "admin", Rol.ADMINISTRADOR, true, true);
        when(usuarioRepository.findById(3L)).thenReturn(Optional.of(usuario));

        assertThrows(AccessDeniedException.class, () -> usuarioService.desactivarUsuario(3L));
    }

    @Test
    void desactivarUsuario_noHaceNadaSiYaEstaInactivo() {
        Usuario usuario = buildUsuario(4L, "ana", Rol.USUARIO, false, false);
        when(usuarioRepository.findById(4L)).thenReturn(Optional.of(usuario));

        usuarioService.desactivarUsuario(4L);

        verify(usuarioRepository, never()).save(any(Usuario.class));
        verify(eventPublisher, never()).publishEvent(any(UsuarioEvent.class));
    }

    @Test
    void desactivarUsuario_deberiaCambiarEstadoYGuardar() {
        Usuario usuario = buildUsuario(5L, "ana", Rol.USUARIO, false, true);
        when(usuarioRepository.findById(5L)).thenReturn(Optional.of(usuario));

        usuarioService.desactivarUsuario(5L);

        assertFalse(usuario.getEsActivo());
        verify(usuarioRepository).save(usuario);
        verify(eventPublisher).publishEvent(any(UsuarioEvent.class));
    }

    @Test
    void actualizarUsuario_deberiaLanzarErrorSiEsAdminBase() {
        Usuario usuario = buildUsuario(6L, "admin", Rol.ADMINISTRADOR, true, true);
        when(usuarioRepository.findById(6L)).thenReturn(Optional.of(usuario));

        assertThrows(AccessDeniedException.class, () -> usuarioService.actualizarUsuario(6L, usuarioRequest));
    }

    @Test
    void actualizarUsuario_deberiaLanzarErrorSiNoEsAdminNiPropietario() {
        setUserAuth("otro", false);
        Usuario usuario = buildUsuario(7L, "juan", Rol.USUARIO, false, true);
        when(usuarioRepository.findById(7L)).thenReturn(Optional.of(usuario));

        assertThrows(AccessDeniedException.class, () -> usuarioService.actualizarUsuario(7L, usuarioRequest));
    }

    @Test
    void actualizarUsuario_deberiaLanzarErrorSiNoAdminIntentaCambiarRol() {
        setUserAuth("juan", false);
        Usuario usuario = buildUsuario(8L, "juan", Rol.USUARIO, false, true);
        when(usuarioRepository.findById(8L)).thenReturn(Optional.of(usuario));

        UsuarioRequest requestCambioRol = UsuarioRequest.builder()
                .nombre("juan")
                .email("juan@smartlogix.com")
                .contrasena("Password123")
                .rol(Rol.ADMINISTRADOR)
                .build();

        assertThrows(AccessDeniedException.class, () -> usuarioService.actualizarUsuario(8L, requestCambioRol));
    }

    @Test
    void actualizarUsuario_deberiaLanzarErrorSiNombreYaEnUso() {
        setUserAuth("juan", false);
        Usuario usuario = buildUsuario(9L, "juan", Rol.USUARIO, false, true);
        when(usuarioRepository.findById(9L)).thenReturn(Optional.of(usuario));

        UsuarioRequest request = UsuarioRequest.builder()
                .nombre("nuevo")
                .email("juan@smartlogix.com")
                .contrasena("Password123")
                .rol(Rol.USUARIO)
                .build();

        when(usuarioRepository.existsByNombre("nuevo")).thenReturn(true);

        assertThrows(UsuarioYaExisteException.class, () -> usuarioService.actualizarUsuario(9L, request));
    }

    @Test
    void actualizarUsuario_deberiaLanzarErrorSiEmailYaEnUso() {
        setUserAuth("juan", false);
        Usuario usuario = buildUsuario(10L, "juan", Rol.USUARIO, false, true);
        when(usuarioRepository.findById(10L)).thenReturn(Optional.of(usuario));

        UsuarioRequest request = UsuarioRequest.builder()
                .nombre("juan")
                .email("nuevo@smartlogix.com")
                .contrasena("Password123")
                .rol(Rol.USUARIO)
                .build();

        when(usuarioRepository.existsByEmail("nuevo@smartlogix.com")).thenReturn(true);

        assertThrows(UsuarioYaExisteException.class, () -> usuarioService.actualizarUsuario(10L, request));
    }

    @Test
    void actualizarUsuario_ownerNoAdminActualizaDatosSinCambiarRol() {
        setUserAuth("juan", false);
        Usuario usuario = buildUsuario(11L, "juan", Rol.USUARIO, false, true);
        when(usuarioRepository.findById(11L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("Password123")).thenReturn("hash-pass");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioRequest request = UsuarioRequest.builder()
                .nombre("juan")
                .email("juan.nuevo@smartlogix.com")
                .contrasena("Password123")
            .rol(Rol.USUARIO)
                .build();

        UsuarioResponse response = usuarioService.actualizarUsuario(11L, request);

        assertEquals("juan.nuevo@smartlogix.com", response.getEmail());
        assertEquals(Rol.USUARIO, response.getRol());
        verify(eventPublisher).publishEvent(any(UsuarioEvent.class));
    }

    @Test
    void actualizarUsuario_adminPuedeCambiarRol() {
        setUserAuth("admin", true);
        Usuario usuario = buildUsuario(12L, "juan", Rol.USUARIO, false, true);
        when(usuarioRepository.findById(12L)).thenReturn(Optional.of(usuario));
        when(passwordEncoder.encode("Password123")).thenReturn("hash-pass");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UsuarioRequest request = UsuarioRequest.builder()
                .nombre("juan")
                .email("juan@smartlogix.com")
                .contrasena("Password123")
                .rol(Rol.ADMINISTRADOR)
                .build();

        UsuarioResponse response = usuarioService.actualizarUsuario(12L, request);

        assertEquals(Rol.ADMINISTRADOR, response.getRol());
        verify(usuarioRepository).save(any(Usuario.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void listarUsuarios_deberiaAplicarPaginacionYOrdenDefaultCuandoSortNoValido() {
        Usuario usuario = buildUsuario(13L, "pepe", Rol.VENDEDOR, false, true);
        Page<Usuario> page = new PageImpl<>(List.of(usuario));

        when(usuarioRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<UsuarioResponse> result = usuarioService.listarUsuarios(
                "pe", "mail", Rol.VENDEDOR, true, -1, 500, "campoInvalido", "desc"
        );

        ArgumentCaptor<Pageable> pageableCaptor = ArgumentCaptor.forClass(Pageable.class);
        verify(usuarioRepository).findAll(any(Specification.class), pageableCaptor.capture());
        Pageable pageable = pageableCaptor.getValue();

        assertEquals(0, pageable.getPageNumber());
        assertEquals(100, pageable.getPageSize());
        assertEquals(Sort.Direction.DESC, pageable.getSort().stream().findFirst().orElseThrow().getDirection());
        assertEquals("id", pageable.getSort().stream().findFirst().orElseThrow().getProperty());

        assertEquals(1, result.getContent().size());
        assertEquals("pepe", result.getContent().get(0).getNombre());
        verify(eventPublisher).publishEvent(any(UsuarioEvent.class));
    }

    @SuppressWarnings("unchecked")
    @Test
    void listarUsuarioPorRol_deberiaDelegarEnListarUsuarios() {
        Page<Usuario> page = new PageImpl<>(List.of(buildUsuario(14L, "u1", Rol.USUARIO, false, true)));
        when(usuarioRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(page);

        Page<UsuarioResponse> result = usuarioService.listarUsuarioPorRol(Rol.USUARIO, 0, 20, "id", "asc");

        assertEquals(1, result.getTotalElements());
        verify(usuarioRepository).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    void listarUsuario_deberiaLanzarNotFoundSiNoExiste() {
        when(usuarioRepository.findById(111L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> usuarioService.listarUsuario(111L));
    }

    @Test
    void listarUsuario_deberiaRetornarUsuarioSiExiste() {
        Usuario usuario = buildUsuario(15L, "cami", Rol.USUARIO, false, true);
        when(usuarioRepository.findById(15L)).thenReturn(Optional.of(usuario));

        UsuarioResponse response = usuarioService.listarUsuario(15L);

        assertEquals(15L, response.getId());
        assertEquals("cami", response.getNombre());
        verify(eventPublisher).publishEvent(any(UsuarioEvent.class));
    }

    @Test
    void login_deberiaLanzarErrorSiUsuarioNoExiste() {
        LoginRequest request = LoginRequest.builder().nombre("noexiste").contrasena("abc").build();
        when(usuarioRepository.findByNombre("noexiste")).thenReturn(Optional.empty());

        assertThrows(BadCredentialsException.class, () -> usuarioService.login(request));
    }

    @Test
    void login_deberiaLanzarErrorSiPasswordNoCoincide() {
        Usuario usuario = buildUsuario(16L, "juan", Rol.USUARIO, false, true);
        usuario.setContrasena("hash");
        LoginRequest request = LoginRequest.builder().nombre("juan").contrasena("bad").build();

        when(usuarioRepository.findByNombre("juan")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("bad", "hash")).thenReturn(false);

        assertThrows(BadCredentialsException.class, () -> usuarioService.login(request));
    }

    @Test
    void login_deberiaLanzarErrorSiUsuarioInactivo() {
        Usuario usuario = buildUsuario(17L, "juan", Rol.USUARIO, false, false);
        usuario.setContrasena("hash");
        LoginRequest request = LoginRequest.builder().nombre("juan").contrasena("ok").build();

        when(usuarioRepository.findByNombre("juan")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("ok", "hash")).thenReturn(true);

        assertThrows(BadCredentialsException.class, () -> usuarioService.login(request));
    }

    @Test
    void login_deberiaRetornarTokenYPermisos() {
        Usuario usuario = buildUsuario(18L, "juan", Rol.ADMINISTRADOR, false, true);
        usuario.setContrasena("hash");

        LoginRequest request = LoginRequest.builder().nombre("juan").contrasena("ok").build();

        when(usuarioRepository.findByNombre("juan")).thenReturn(Optional.of(usuario));
        when(passwordEncoder.matches("ok", "hash")).thenReturn(true);
        when(jwtUtil.generarToken(eq("juan"), eq("ADMINISTRADOR"), anyString(), any(List.class)))
                .thenReturn("jwt-token");

        LoginResponse response = usuarioService.login(request);

        assertEquals("jwt-token", response.getToken());
        assertEquals("juan", response.getNombre());
        assertEquals(Rol.ADMINISTRADOR, response.getRol());
        assertTrue(response.getPermisos().contains("ADMINISTRACION"));
    }

    private Usuario buildUsuario(Long id, String nombre, Rol rol, boolean adminBase, boolean activo) {
        return Usuario.builder()
                .id(id)
                .nombre(nombre)
                .email(nombre + "@mail.com")
                .contrasena("hash")
                .rol(rol)
                .adminBase(adminBase)
                .esActivo(activo)
                .build();
    }

    private void setUserAuth(String username, boolean admin) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                username,
                "N/A",
                admin
                        ? List.of(new SimpleGrantedAuthority("ROLE_ADMINISTRADOR"))
                        : List.of(new SimpleGrantedAuthority("ROLE_USUARIO"))
        );
        SecurityContextHolder.getContext().setAuthentication(authenticationToken);
    }
}
