package com.smartlogix.usuarios.repository;

import com.smartlogix.usuarios.model.AuditLog;
import com.smartlogix.usuarios.model.TipoEvento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Page<AuditLog> findByUsuarioAutenticado(String usuarioAutenticado, Pageable pageable);
    
    Page<AuditLog> findByTipoEvento(TipoEvento tipoEvento, Pageable pageable);
    
    Page<AuditLog> findByTimestampBetween(LocalDateTime inicio, LocalDateTime fin, Pageable pageable);
    
    Page<AuditLog> findAll(Pageable pageable);
}
