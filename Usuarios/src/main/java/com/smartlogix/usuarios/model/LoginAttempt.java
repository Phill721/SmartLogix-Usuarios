package com.smartlogix.usuarios.model;

import jakarta.persistence.Entity;
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
@Table(name = "login_attempts", indexes = {
    @Index(name = "idx_login_attempts_usuario_id", columnList = "usuarioId"),
    @Index(name = "idx_login_attempts_timestamp", columnList = "timestamp"),
    @Index(name = "idx_login_attempts_exitoso", columnList = "exitoso")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long usuarioId;

    @Column(nullable = false)
    private String nombreUsuario;

    @Column(nullable = false)
    private Boolean exitoso;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column
    private String razonFallo;
}
