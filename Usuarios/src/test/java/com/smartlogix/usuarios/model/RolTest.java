package com.smartlogix.usuarios.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class RolTest {

    @Test
    void getPermiso_deberiaRetornarPrimerPermiso() {
        assertEquals("VISTA_TIENDA", Rol.USUARIO.getPermiso());
        assertEquals("VENTAS", Rol.VENDEDOR.getPermiso());
        assertEquals("ADMINISTRACION", Rol.ADMINISTRADOR.getPermiso());
    }

    @Test
    void getPermisos_deberiaRetornarListaCompleta() {
        assertEquals(1, Rol.USUARIO.getPermisos().size());
        assertTrue(Rol.ADMINISTRADOR.getPermisos().contains("VENTAS"));
        assertFalse(Rol.VENDEDOR.getPermisos().contains("ADMINISTRACION"));
    }
}
