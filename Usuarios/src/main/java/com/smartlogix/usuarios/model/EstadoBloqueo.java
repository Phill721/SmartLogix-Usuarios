package com.smartlogix.usuarios.model;

public enum EstadoBloqueo {
    ACTIVO("Cuenta activa"),
    BLOQUEADO_TEMPORAL("Bloqueado temporalmente por intentos fallidos"),
    BLOQUEADO_PERMANENTE("Bloqueado permanentemente por admin");

    private final String descripcion;

    EstadoBloqueo(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
