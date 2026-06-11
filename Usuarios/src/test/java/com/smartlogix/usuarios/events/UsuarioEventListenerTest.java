package com.smartlogix.usuarios.events;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class UsuarioEventListenerTest {

    @Test
    void manejarEventoUsuario_noDebeLanzarExcepcion() {
        UsuarioEventListener listener = new UsuarioEventListener();
        UsuarioEvent event = UsuarioEvent.builder()
                .tipoOperacion("CREAR")
                .mensaje("Usuario creado")
                .timestamp(LocalDateTime.now())
                .build();

        assertDoesNotThrow(() -> listener.manejarEventoUsuario(event));
    }
}
