package com.smartlogix.usuarios.model;

import java.util.List;

public enum Rol {
    USUARIO(List.of("VISTA_TIENDA")),
    VENDEDOR(List.of("VENTAS")),
    ADMINISTRADOR(List.of("ADMINISTRACION", "VENTAS", "VISTA_TIENDA"));

    private final List<String> permisos;

    Rol(List<String> permisos) {
        this.permisos = permisos;
    }

    public String getPermiso() {
        return permisos.get(0);
    }

    public List<String> getPermisos() {
        return permisos;
    }
}
