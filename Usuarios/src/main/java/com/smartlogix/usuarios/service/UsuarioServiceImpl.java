package com.smartlogix.usuarios.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.smartlogix.usuarios.dto.LoginRequest;
import com.smartlogix.usuarios.dto.LoginResponse;
import com.smartlogix.usuarios.dto.UsuarioRequest;
import com.smartlogix.usuarios.dto.UsuarioResponse;
import com.smartlogix.usuarios.events.UsuarioEvent;
import com.smartlogix.usuarios.exception.ResourceNotFoundException;
import com.smartlogix.usuarios.exception.UsuarioYaExisteException;
import com.smartlogix.usuarios.model.EstadoIntento;
import com.smartlogix.usuarios.model.Rol;
import com.smartlogix.usuarios.model.TipoEvento;
import com.smartlogix.usuarios.model.Usuario;
import com.smartlogix.usuarios.repository.UsuarioRepository;
import com.smartlogix.usuarios.security.JwtUtil;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class UsuarioServiceImpl implements UsuarioService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of("id", "nombre", "email", "rol", "esActivo");

    private final UsuarioRepository usuarioRepository;
    private final BCryptPasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher eventPublisher;
    private final JwtUtil jwtUtil;
    private final AuditService auditService;
    private final LoginAttemptService loginAttemptService;

    @Override
    public UsuarioResponse agregarUsuario(UsuarioRequest request) {
        if (request.getRol() == Rol.ADMINISTRADOR && !esAdminBaseAutenticado()) {
            String usuario = obtenerUsuarioAutenticado();
            auditService.registrarIntentoFallido(
                usuario,
                TipoEvento.INTENTO_CAMBIAR_ROL_ADMIN,
                EstadoIntento.BLOQUEADO,
                "Intento de crear usuario con rol ADMINISTRADOR sin ser el admin base",
                null,
                request.getEmail()
            );
            throw new AccessDeniedException("Solo el administrador base puede crear usuarios con rol ADMINISTRADOR");
        }

        if (usuarioRepository.existsByNombre(request.getNombre())) {
            throw new UsuarioYaExisteException("El nombre de usuario ya está en uso");
        }

        if (usuarioRepository.existsByEmail(request.getEmail())) {
            throw new UsuarioYaExisteException("El correo electrónico ya está registrado");
        }

        Usuario usuario = Usuario.builder()
                .nombre(request.getNombre())
                .email(request.getEmail())
                .contrasena(passwordEncoder.encode(request.getContrasena()))
                .rol(request.getRol())
                .esActivo(true)
                .build();

        Usuario guardado = usuarioRepository.save(usuario);
        publicarEvento("CREAR", "Usuario creado con ID: " + guardado.getId());
        return toResponse(guardado);
    }

    @Override
    public void eliminarUsuario(Long id) {
        Usuario usuario = obtenerUsuarioPorId(id);
        if (Boolean.TRUE.equals(usuario.getAdminBase())) {
            throw new AccessDeniedException("No se puede eliminar el administrador base del sistema");
        }
        usuarioRepository.delete(usuario);
        publicarEvento("ELIMINAR", "Usuario eliminado con ID: " + id);
    }

    @Override
    public void desactivarUsuario(Long id) {
        Usuario usuario = obtenerUsuarioPorId(id);
        if (Boolean.TRUE.equals(usuario.getAdminBase())) {
            String usuarioAutenticado = obtenerUsuarioAutenticado();
            auditService.registrarIntentoFallido(
                usuarioAutenticado,
                TipoEvento.INTENTO_MODIFICAR_ADMIN_BASE,
                EstadoIntento.BLOQUEADO,
                "Intento de desactivar el administrador base del sistema",
                id,
                usuario.getEmail()
            );
            throw new AccessDeniedException("No se puede desactivar el administrador base del sistema");
        }

        if (Boolean.FALSE.equals(usuario.getEsActivo())) {
            return;
        }

        usuario.setEsActivo(false);
        usuario.setTokensInvalidosDesde(LocalDateTime.now());
        usuarioRepository.save(usuario);
        publicarEvento("DESACTIVAR", "Usuario desactivado con ID: " + id);
    }

    @Override
    public UsuarioResponse actualizarUsuario(Long id, UsuarioRequest request) {
        Usuario usuario = obtenerUsuarioPorId(id);

        if (Boolean.TRUE.equals(usuario.getAdminBase())) {
            String usuarioAutenticado = obtenerUsuarioAutenticado();
            auditService.registrarIntentoFallido(
                usuarioAutenticado,
                TipoEvento.INTENTO_MODIFICAR_ADMIN_BASE,
                EstadoIntento.BLOQUEADO,
                "Intento de modificar el administrador base del sistema",
                id,
                usuario.getEmail()
            );
            throw new AccessDeniedException("El administrador base del sistema es inmutable y no puede modificarse");
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authName = authentication != null ? authentication.getName() : null;
        boolean isAdmin = esAdminAutenticado();

        // Permitir actualización si es administrador o el propio usuario
        if (!isAdmin && (authName == null || !usuario.getNombre().equals(authName))) {
            String usuarioAutenticado = obtenerUsuarioAutenticado();
            auditService.registrarIntentoFallido(
                usuarioAutenticado,
                TipoEvento.INTENTO_ACTUALIZAR_OTRO_USUARIO,
                EstadoIntento.BLOQUEADO,
                "Intento de actualizar usuario diferente sin permisos de admin",
                id,
                usuario.getEmail()
            );
            throw new AccessDeniedException("Solo el propio usuario o un administrador puede modificar este usuario");
        }

        // Sólo el admin base puede cambiar roles (no cualquier admin)
        boolean esAdminBase = esAdminBaseAutenticado();
        if (request.getRol() != null && request.getRol() != usuario.getRol()) {
            // Si intenta cambiar rol sin ser admin base
            if (!esAdminBase) {
                String usuarioAutenticado = obtenerUsuarioAutenticado();
                auditService.registrarIntentoFallido(
                    usuarioAutenticado,
                    TipoEvento.INTENTO_CAMBIAR_ROL_ADMIN,
                    EstadoIntento.BLOQUEADO,
                    "Intento de cambiar rol sin ser el administrador base",
                    id,
                    usuario.getEmail()
                );
                throw new AccessDeniedException("Solo el administrador base del sistema puede cambiar roles");
            }
            
            // Si intenta degradar a un admin siendo admin (no base)
            if (usuario.getRol() == Rol.ADMINISTRADOR && !esAdminBase) {
                String usuarioAutenticado = obtenerUsuarioAutenticado();
                auditService.registrarIntentoFallido(
                    usuarioAutenticado,
                    TipoEvento.INTENTO_CAMBIAR_ROL_ADMIN,
                    EstadoIntento.BLOQUEADO,
                    "Intento de degradar un administrador sin ser el admin base",
                    id,
                    usuario.getEmail()
                );
                throw new AccessDeniedException("Solo el administrador base puede cambiar roles de otros administradores");
            }
        }

        if (!usuario.getNombre().equals(request.getNombre()) && usuarioRepository.existsByNombre(request.getNombre())) {
            throw new UsuarioYaExisteException("El nombre de usuario ya está en uso");
        }

        if (!usuario.getEmail().equals(request.getEmail()) && usuarioRepository.existsByEmail(request.getEmail())) {
            throw new UsuarioYaExisteException("El correo electrónico ya está registrado");
        }

        boolean esPropioUsuario = authName != null && usuario.getNombre().equals(authName);
        boolean cambiaContrasena = request.getContrasena() != null && !request.getContrasena().isBlank();
        if (esPropioUsuario && cambiaContrasena) {
            if (request.getContrasenaActual() == null || request.getContrasenaActual().isBlank()) {
                throw new AccessDeniedException("Debe confirmar la contraseña actual para cambiarla");
            }

            if (!passwordEncoder.matches(request.getContrasenaActual(), usuario.getContrasena())) {
                auditService.registrarIntentoFallido(
                    obtenerUsuarioAutenticado(),
                    TipoEvento.INTENTO_ACCESO_NO_AUTORIZADO,
                    EstadoIntento.FALLIDO,
                    "Intento de cambio de contraseña con contraseña actual incorrecta",
                    id,
                    usuario.getEmail()
                );
                throw new AccessDeniedException("La contraseña actual no coincide");
            }
        }

        usuario.setNombre(request.getNombre());
        usuario.setEmail(request.getEmail());
        usuario.setContrasena(passwordEncoder.encode(request.getContrasena()));
        // Solo setear rol si es admin base (ya validado más arriba)
        if (esAdminBase && request.getRol() != null) {
            usuario.setRol(request.getRol());
        }

        Usuario actualizado = usuarioRepository.save(usuario);
        publicarEvento("ACTUALIZAR", "Usuario actualizado con ID: " + actualizado.getId());
        return toResponse(actualizado);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listarUsuarios(String nombre, String email, Rol rol, Boolean esActivo,
                                                int page, int size, String sortBy, String sortDir) {
        Pageable pageable = construirPageable(page, size, sortBy, sortDir);
        Specification<Usuario> specification = (root, query, cb) -> cb.conjunction();

        if (nombre != null && !nombre.isBlank()) {
            String nombreFiltro = nombre.trim().toLowerCase();
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("nombre")), "%" + nombreFiltro + "%"));
        }

        if (email != null && !email.isBlank()) {
            String emailFiltro = email.trim().toLowerCase();
            specification = specification.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("email")), "%" + emailFiltro + "%"));
        }

        if (rol != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("rol"), rol));
        }

        if (esActivo != null) {
            specification = specification.and((root, query, cb) -> cb.equal(root.get("esActivo"), esActivo));
        }

        Page<UsuarioResponse> resultado = usuarioRepository.findAll(specification, pageable).map(this::toResponse);
        publicarEvento("CONSULTAR", "Listado paginado de usuarios: page=" + resultado.getNumber()
                + ", size=" + resultado.getSize() + ", total=" + resultado.getTotalElements());
        return resultado;
    }

    @Override
    @Transactional(readOnly = true)
    public UsuarioResponse listarUsuario(Long id) {
        Usuario usuario = obtenerUsuarioPorId(id);
        publicarEvento("CONSULTAR", "Consulta de usuario con ID: " + id);
        return toResponse(usuario);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listarUsuarioPorRol(Rol rol, int page, int size, String sortBy, String sortDir) {
        return listarUsuarios(null, null, rol, null, page, size, sortBy, sortDir);
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByNombre(request.getNombre())
                .orElseThrow(() -> {
                    auditService.registrarIntentoFallido(
                        request.getNombre(),
                        TipoEvento.USUARIO_NO_ENCONTRADO,
                        EstadoIntento.FALLIDO,
                        "Intento de login con usuario inexistente",
                        null,
                        null
                    );
                    return new BadCredentialsException("Credenciales inválidas");
                });

        // Verificar si la cuenta está bloqueada temporalmente
        loginAttemptService.desbloquearAutomaticamente(usuario);
        if (loginAttemptService.estaBloqueadoTemporalmente(usuario)) {
            auditService.registrarIntentoFallido(
                request.getNombre(),
                TipoEvento.LOGIN_FALLIDO,
                EstadoIntento.BLOQUEADO,
                "Intento de login con cuenta bloqueada temporalmente por intentos fallidos",
                usuario.getId(),
                usuario.getEmail()
            );
            throw new BadCredentialsException("Cuenta bloqueada temporalmente. Intente más tarde.");
        }

        if (Boolean.FALSE.equals(usuario.getEsActivo())) {
            auditService.registrarIntentoFallido(
                request.getNombre(),
                TipoEvento.LOGIN_FALLIDO,
                EstadoIntento.BLOQUEADO,
                "Intento de login con usuario desactivado",
                usuario.getId(),
                usuario.getEmail()
            );
            throw new BadCredentialsException("Usuario desactivado");
        }

        if (!passwordEncoder.matches(request.getContrasena(), usuario.getContrasena())) {
            loginAttemptService.registrarIntentoFallido(usuario, "Contraseña incorrecta");
            auditService.registrarIntentoFallido(
                request.getNombre(),
                TipoEvento.LOGIN_FALLIDO,
                EstadoIntento.FALLIDO,
                "Intento de login con contraseña incorrecta (Intentos: " + (usuario.getIntentosFallidos() + 1) + "/3)",
                usuario.getId(),
                usuario.getEmail()
            );
            throw new BadCredentialsException("Credenciales inválidas");
        }

        // Login exitoso - limpiar intentos fallidos
        loginAttemptService.registrarIntentoExitoso(usuario);

        String token = jwtUtil.generarToken(
            usuario.getId(),
            usuario.getNombre(),
            usuario.getRol().name(),
            usuario.getRol().getPermiso(),
            usuario.getRol().getPermisos()
        );
        return LoginResponse.builder()
                .token(token)
                .nombre(usuario.getNombre())
                .rol(usuario.getRol())
                .permiso(usuario.getRol() != null ? usuario.getRol().getPermiso() : null)
            .permisos(usuario.getRol() != null ? usuario.getRol().getPermisos() : List.of())
                .build();
    }

    private Usuario obtenerUsuarioPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado con ID: " + id));
    }

    @Override
    @Transactional
    public void desbloquearCuenta(Long id) {
        Usuario usuario = obtenerUsuarioPorId(id);
        loginAttemptService.desbloquearCuenta(usuario);
        publicarEvento("DESBLOQUEAR", "Cuenta desbloqueada para usuario con ID: " + id);
    }

    private UsuarioResponse toResponse(Usuario usuario) {
        return UsuarioResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .esActivo(usuario.getEsActivo())
                .rol(usuario.getRol())
                .permiso(usuario.getRol() != null ? usuario.getRol().getPermiso() : null)
                .permisos(usuario.getRol() != null ? usuario.getRol().getPermisos() : List.of())
                .build();
    }

    private void publicarEvento(String tipoOperacion, String mensaje) {
        String usuarioAutenticado = obtenerUsuarioAutenticado();
        eventPublisher.publishEvent(UsuarioEvent.builder()
                .tipoOperacion(tipoOperacion)
                .mensaje(mensaje)
                .timestamp(LocalDateTime.now())
                .usuarioAutenticado(usuarioAutenticado)
                .build());
    }

    private String obtenerUsuarioAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            String nombre = authentication.getName();
            return (nombre != null && !nombre.isBlank()) ? nombre : "ANONIMO";
        }
        return "ANONIMO";
    }

    private boolean esAdminAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMINISTRADOR".equals(authority.getAuthority()));
    }

    private boolean esAdminBaseAutenticado() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String nombreUsuario = authentication.getName();
        return usuarioRepository.findByNombre(nombreUsuario)
                .map(usuario -> Boolean.TRUE.equals(usuario.getAdminBase()))
                .orElse(false);
    }

    private Pageable construirPageable(int page, int size, String sortBy, String sortDir) {
        int pagina = Math.max(page, DEFAULT_PAGE);
        int tamanio = size <= 0 ? DEFAULT_SIZE : Math.min(size, MAX_PAGE_SIZE);
        String orden = ALLOWED_SORT_FIELDS.contains(sortBy) ? sortBy : "id";
        Sort.Direction direccion = "desc".equalsIgnoreCase(sortDir) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return PageRequest.of(pagina, tamanio, Sort.by(direccion, orden));
    }

    @Override
    public boolean esUsuarioActual(Long id) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String email = authentication.getName();
        Usuario usuarioActual = usuarioRepository.findByEmail(email)
                .orElse(null);

        return usuarioActual != null && usuarioActual.getId().equals(id);
    }
}
