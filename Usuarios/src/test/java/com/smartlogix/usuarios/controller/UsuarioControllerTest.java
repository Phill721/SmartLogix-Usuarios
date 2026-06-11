package com.smartlogix.usuarios.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.smartlogix.usuarios.dto.LoginRequest;
import com.smartlogix.usuarios.dto.LoginResponse;
import com.smartlogix.usuarios.dto.PageResponse;
import com.smartlogix.usuarios.dto.UsuarioRequest;
import com.smartlogix.usuarios.dto.UsuarioResponse;
import com.smartlogix.usuarios.model.Rol;
import com.smartlogix.usuarios.service.UsuarioService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class UsuarioControllerTest {

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private UsuarioController controller;

    @Test
    void agregarUsuario_deberiaRetornar201() {
        UsuarioRequest request = UsuarioRequest.builder()
                .nombre("juan")
                .email("juan@correo.com")
                .contrasena("Password123")
                .rol(Rol.USUARIO)
                .build();

        UsuarioResponse response = UsuarioResponse.builder()
                .id(1L)
                .nombre("juan")
                .rol(Rol.USUARIO)
                .build();

        when(usuarioService.agregarUsuario(request)).thenReturn(response);

        ResponseEntity<UsuarioResponse> resultado = controller.agregarUsuario(request);

        assertEquals(HttpStatus.CREATED, resultado.getStatusCode());
        assertEquals(1L, resultado.getBody().getId());
    }

    @Test
    void login_deberiaRetornar200() {
        LoginRequest request = LoginRequest.builder().nombre("juan").contrasena("123").build();
        LoginResponse loginResponse = LoginResponse.builder().token("jwt").nombre("juan").rol(Rol.USUARIO).build();

        when(usuarioService.login(request)).thenReturn(loginResponse);

        ResponseEntity<LoginResponse> resultado = controller.login(request);

        assertEquals(HttpStatus.OK, resultado.getStatusCode());
        assertEquals("jwt", resultado.getBody().getToken());
    }

    @Test
    void listarUsuarios_deberiaRetornarPageResponse() {
        UsuarioResponse usuario = UsuarioResponse.builder().id(2L).nombre("ana").rol(Rol.VENDEDOR).build();
        Page<UsuarioResponse> page = new PageImpl<>(
                List.of(usuario),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id")),
                1
        );

        when(usuarioService.listarUsuarios("ana", "ana@mail.com", Rol.VENDEDOR, true, 0, 20, "id", "asc"))
                .thenReturn(page);

        ResponseEntity<PageResponse<UsuarioResponse>> response = controller.listarUsuarios(
                "ana", "ana@mail.com", Rol.VENDEDOR, true, 0, 20, "id", "asc"
        );

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
        assertEquals("ana", response.getBody().getContent().get(0).getNombre());
        assertEquals("id", response.getBody().getSortBy());
    }

    @Test
    void listarUsuario_deberiaRetornar200() {
        UsuarioResponse usuario = UsuarioResponse.builder().id(3L).nombre("carlos").rol(Rol.USUARIO).build();
        when(usuarioService.listarUsuario(3L)).thenReturn(usuario);

        ResponseEntity<UsuarioResponse> response = controller.listarUsuario(3L);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("carlos", response.getBody().getNombre());
    }

    @Test
    void listarUsuarioPorRol_deberiaRetornarPageResponse() {
        UsuarioResponse usuario = UsuarioResponse.builder().id(4L).nombre("admin").rol(Rol.ADMINISTRADOR).build();
        Page<UsuarioResponse> page = new PageImpl<>(
                List.of(usuario),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.ASC, "id")),
                1
        );

        when(usuarioService.listarUsuarioPorRol(Rol.ADMINISTRADOR, 0, 20, "id", "asc")).thenReturn(page);

        ResponseEntity<PageResponse<UsuarioResponse>> response =
                controller.listarUsuarioPorRol(Rol.ADMINISTRADOR, 0, 20, "id", "asc");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(1, response.getBody().getTotalElements());
    }

    @Test
    void actualizarUsuario_deberiaRetornar200() {
        UsuarioRequest request = UsuarioRequest.builder()
                .nombre("nuevo")
                .email("nuevo@mail.com")
                .contrasena("Password123")
                .rol(Rol.USUARIO)
                .build();

        UsuarioResponse responseMock = UsuarioResponse.builder().id(5L).nombre("nuevo").build();

        when(usuarioService.actualizarUsuario(5L, request)).thenReturn(responseMock);

        ResponseEntity<UsuarioResponse> response = controller.actualizarUsuario(5L, request);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("nuevo", response.getBody().getNombre());
    }

    @Test
    void eliminarUsuario_deberiaRetornar204() {
        ResponseEntity<Void> response = controller.eliminarUsuario(6L);

        verify(usuarioService).eliminarUsuario(6L);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }

    @Test
    void desactivarUsuario_deberiaRetornar204() {
        ResponseEntity<Void> response = controller.desactivarUsuario(7L);

        verify(usuarioService).desactivarUsuario(7L);
        assertEquals(HttpStatus.NO_CONTENT, response.getStatusCode());
    }
}
