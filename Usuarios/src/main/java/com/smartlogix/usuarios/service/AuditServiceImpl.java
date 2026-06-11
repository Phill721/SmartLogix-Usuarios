package com.smartlogix.usuarios.service;

import com.smartlogix.usuarios.dto.AuditLogResponse;
import com.smartlogix.usuarios.model.AuditLog;
import com.smartlogix.usuarios.model.EstadoIntento;
import com.smartlogix.usuarios.model.TipoEvento;
import com.smartlogix.usuarios.repository.AuditLogRepository;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {

    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 50;
    private static final int MAX_PAGE_SIZE = 200;

    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public void registrarIntentoFallido(String usuarioAutenticado, TipoEvento tipoEvento,
                                        EstadoIntento estado, String descripcion,
                                        Long idUsuarioAfectado, String emailUsuarioAfectado) {
        AuditLog auditLog = AuditLog.builder()
                .usuarioAutenticado(usuarioAutenticado)
                .tipoEvento(tipoEvento)
                .estado(estado)
                .descripcion(descripcion)
                .timestamp(LocalDateTime.now())
                .idUsuarioAfectado(idUsuarioAfectado)
                .emailUsuarioAfectado(emailUsuarioAfectado)
                .build();

        auditLogRepository.save(auditLog);
        System.out.printf("[SmartLogix - Audit] INTENTO FALLIDO | Usuario: %s | Evento: %s | Descripción: %s%n",
                usuarioAutenticado, tipoEvento, descripcion);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> listarAuditorias(int page, int size) {
        int validPage = Math.max(0, page);
        int validSize = Math.min(size, MAX_PAGE_SIZE);
        validSize = Math.max(1, validSize);

        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by("timestamp").descending());
        return auditLogRepository.findAll(pageable).map(this::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AuditLogResponse> listarAuditoriasPorUsuario(String usuarioAutenticado, int page, int size) {
        int validPage = Math.max(0, page);
        int validSize = Math.min(size, MAX_PAGE_SIZE);
        validSize = Math.max(1, validSize);

        Pageable pageable = PageRequest.of(validPage, validSize, Sort.by("timestamp").descending());
        return auditLogRepository.findByUsuarioAutenticado(usuarioAutenticado, pageable).map(this::toResponse);
    }

    private AuditLogResponse toResponse(AuditLog auditLog) {
        return AuditLogResponse.builder()
                .id(auditLog.getId())
                .usuarioAutenticado(auditLog.getUsuarioAutenticado())
                .tipoEvento(auditLog.getTipoEvento())
                .estado(auditLog.getEstado())
                .descripcion(auditLog.getDescripcion())
                .timestamp(auditLog.getTimestamp())
                .idUsuarioAfectado(auditLog.getIdUsuarioAfectado())
                .emailUsuarioAfectado(auditLog.getEmailUsuarioAfectado())
                .build();
    }
}
