package com.smartlogix.usuarios.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResourceNotFound_deberiaRetornar404() {
        ResponseEntity<Map<String, Object>> response = handler.handleResourceNotFound(
                new ResourceNotFoundException("No encontrado")
        );

        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertEquals(404, response.getBody().get("status"));
        assertEquals("No encontrado", response.getBody().get("mensaje"));
    }

    @Test
    void handleUsuarioYaExiste_deberiaRetornar409() {
        ResponseEntity<Map<String, Object>> response = handler.handleUsuarioYaExiste(
                new UsuarioYaExisteException("Duplicado")
        );

        assertEquals(HttpStatus.CONFLICT, response.getStatusCode());
        assertEquals("Duplicado", response.getBody().get("mensaje"));
    }

    @Test
    void handleBadCredentials_deberiaRetornar401() {
        ResponseEntity<Map<String, Object>> response = handler.handleBadCredentials(
                new BadCredentialsException("Credenciales inválidas")
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("Credenciales inválidas", response.getBody().get("mensaje"));
    }

    @Test
    void handleAccessDenied_deberiaRetornar403ConMensajeEstandar() {
        ResponseEntity<Map<String, Object>> response = handler.handleAccessDenied(
                new AccessDeniedException("forbidden")
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("No tienes permisos para realizar esta operación", response.getBody().get("mensaje"));
    }

    @Test
    void handleValidationErrors_deberiaRetornarErroresPorCampo() {
        MethodArgumentNotValidException ex = org.mockito.Mockito.mock(MethodArgumentNotValidException.class);
        BindingResult bindingResult = org.mockito.Mockito.mock(BindingResult.class);

        when(ex.getBindingResult()).thenReturn(bindingResult);
        when(bindingResult.getFieldErrors()).thenReturn(java.util.List.of(
                new FieldError("usuarioRequest", "email", "Email inválido"),
                new FieldError("usuarioRequest", "email", "No puede estar vacío"),
                new FieldError("usuarioRequest", "nombre", "Nombre obligatorio")
        ));

        ResponseEntity<Map<String, Object>> response = handler.handleValidationErrors(ex);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Errores de validación", response.getBody().get("mensaje"));
        @SuppressWarnings("unchecked")
        Map<String, String> errors = (Map<String, String>) response.getBody().get("errors");
        assertEquals("Email inválido, No puede estar vacío", errors.get("email"));
        assertEquals("Nombre obligatorio", errors.get("nombre"));
    }

    @Test
    void handleGeneral_deberiaRetornar500() {
        ResponseEntity<Map<String, Object>> response = handler.handleGeneral(new RuntimeException("boom"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertEquals(500, response.getBody().get("status"));
        assertEquals("Ha ocurrido un error interno", response.getBody().get("mensaje"));
        assertTrue(response.getBody().containsKey("timestamp"));
    }
}
