package com.smartlogix.usuarios.dto;

import com.smartlogix.usuarios.model.EstadoIntento;
import com.smartlogix.usuarios.model.TipoEvento;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuditLogResponse {
    private Long id;
    private String usuarioAutenticado;
    private TipoEvento tipoEvento;
    private EstadoIntento estado;
    private String descripcion;
    private LocalDateTime timestamp;
    private Long idUsuarioAfectado;
    private String emailUsuarioAfectado;
}
