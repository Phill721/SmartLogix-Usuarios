package com.smartlogix.usuarios.events;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioEvent {
    private String tipoOperacion;
    private String mensaje;
    private LocalDateTime timestamp;
    private String usuarioAutenticado;
}
