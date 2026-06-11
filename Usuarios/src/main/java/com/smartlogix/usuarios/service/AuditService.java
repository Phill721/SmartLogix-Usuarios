package com.smartlogix.usuarios.service;

import com.smartlogix.usuarios.dto.AuditLogResponse;
import com.smartlogix.usuarios.model.EstadoIntento;
import com.smartlogix.usuarios.model.TipoEvento;
import org.springframework.data.domain.Page;

public interface AuditService {
    
    void registrarIntentoFallido(String usuarioAutenticado, TipoEvento tipoEvento, 
                                 EstadoIntento estado, String descripcion, 
                                 Long idUsuarioAfectado, String emailUsuarioAfectado);
    
    Page<AuditLogResponse> listarAuditorias(int page, int size);
    
    Page<AuditLogResponse> listarAuditoriasPorUsuario(String usuarioAutenticado, int page, int size);
}
