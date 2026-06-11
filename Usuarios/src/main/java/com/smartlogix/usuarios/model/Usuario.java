package com.smartlogix.usuarios.model;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "usuarios", indexes = {
    @Index(name = "idx_usuarios_nombre", columnList = "nombre"),
    @Index(name = "idx_usuarios_email", columnList = "email"),
    @Index(name = "idx_usuarios_rol", columnList = "rol"),
    @Index(name = "idx_usuarios_es_activo", columnList = "esActivo"),
    @Index(name = "idx_usuarios_rol_activo", columnList = "rol,esActivo")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nombre;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String contrasena;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rol rol;

    @Builder.Default
    private Boolean adminBase = false;

    @Builder.Default
    private Boolean esActivo = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EstadoBloqueo estadoBloqueo = EstadoBloqueo.ACTIVO;

    @Column
    private LocalDateTime fechaBloqueoTemporal;

    @Column
    @Builder.Default
    private Integer intentosFallidos = 0;

    @Column
    private LocalDateTime tokensInvalidosDesde;
}
