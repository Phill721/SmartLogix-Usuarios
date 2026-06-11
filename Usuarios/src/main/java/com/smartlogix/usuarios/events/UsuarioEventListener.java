package com.smartlogix.usuarios.events;

import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UsuarioEventListener {

    @EventListener
    public void manejarEventoUsuario(UsuarioEvent event) {
        System.out.printf("[SmartLogix - Usuarios] | %s | Realizado por: %s | Operación: %s | %s%n",
                event.getTimestamp(), event.getUsuarioAutenticado(), event.getTipoOperacion(), event.getMensaje());
    }
}
