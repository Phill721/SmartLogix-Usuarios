package com.smartlogix.usuarios.controller;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.smartlogix.usuarios.dto.LoginRequest;
import com.smartlogix.usuarios.dto.LoginResponse;
import com.smartlogix.usuarios.dto.PageResponse;
import com.smartlogix.usuarios.dto.UsuarioRequest;
import com.smartlogix.usuarios.dto.UsuarioResponse;
import com.smartlogix.usuarios.model.Rol;
import com.smartlogix.usuarios.service.LoginAttemptService;
import com.smartlogix.usuarios.service.UsuarioService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final LoginAttemptService loginAttemptService;

    @PostMapping("/register")
    public ResponseEntity<UsuarioResponse> agregarUsuario(@Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(usuarioService.agregarUsuario(request));
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(usuarioService.login(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('ADMINISTRACION')")
    public ResponseEntity<PageResponse<UsuarioResponse>> listarUsuarios(
            @RequestParam(required = false) String nombre,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) Rol rol,
            @RequestParam(required = false) Boolean esActivo,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Page<UsuarioResponse> resultado = usuarioService.listarUsuarios(
                nombre, email, rol, esActivo, page, size, sortBy, sortDir
        );
        return ResponseEntity.ok(PageResponse.from(resultado));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMINISTRACION')")
    public ResponseEntity<UsuarioResponse> listarUsuario(@PathVariable Long id) {
        return ResponseEntity.ok(usuarioService.listarUsuario(id));
    }

    @GetMapping("/rol/{rol}")
    @PreAuthorize("hasAuthority('ADMINISTRACION')")
    public ResponseEntity<PageResponse<UsuarioResponse>> listarUsuarioPorRol(
            @PathVariable Rol rol,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir
    ) {
        Page<UsuarioResponse> resultado = usuarioService.listarUsuarioPorRol(rol, page, size, sortBy, sortDir);
        return ResponseEntity.ok(PageResponse.from(resultado));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMINISTRACION') or @usuarioService.esUsuarioActual(#id)")
    public ResponseEntity<UsuarioResponse> actualizarUsuario(@PathVariable Long id, @Valid @RequestBody UsuarioRequest request) {
        return ResponseEntity.ok(usuarioService.actualizarUsuario(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ADMINISTRACION')")
    public ResponseEntity<Void> eliminarUsuario(@PathVariable Long id) {
        usuarioService.eliminarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('ADMINISTRACION')")
    public ResponseEntity<Void> desactivarUsuario(@PathVariable Long id) {
        usuarioService.desactivarUsuario(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/desbloquear")
    @PreAuthorize("hasAuthority('ADMINISTRACION')")
    public ResponseEntity<Void> desbloquearCuenta(@PathVariable Long id) {
        usuarioService.desbloquearCuenta(id);
        return ResponseEntity.noContent().build();
    }
    }

