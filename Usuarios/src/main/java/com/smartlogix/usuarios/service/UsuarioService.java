package com.smartlogix.usuarios.service;

import org.springframework.data.domain.Page;

import com.smartlogix.usuarios.dto.LoginRequest;
import com.smartlogix.usuarios.dto.LoginResponse;
import com.smartlogix.usuarios.dto.UsuarioRequest;
import com.smartlogix.usuarios.dto.UsuarioResponse;
import com.smartlogix.usuarios.model.Rol;

public interface UsuarioService {

    UsuarioResponse agregarUsuario(UsuarioRequest request);

    void eliminarUsuario(Long id);

    void desactivarUsuario(Long id);

    UsuarioResponse actualizarUsuario(Long id, UsuarioRequest request);

    Page<UsuarioResponse> listarUsuarios(String nombre, String email, Rol rol, Boolean esActivo,
                                         int page, int size, String sortBy, String sortDir);

    UsuarioResponse listarUsuario(Long id);

    Page<UsuarioResponse> listarUsuarioPorRol(Rol rol, int page, int size, String sortBy, String sortDir);

    LoginResponse login(LoginRequest request);

    void desbloquearCuenta(Long id);

    boolean esUsuarioActual(Long id);
}
