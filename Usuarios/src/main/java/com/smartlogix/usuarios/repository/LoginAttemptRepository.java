package com.smartlogix.usuarios.repository;

import com.smartlogix.usuarios.model.LoginAttempt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {
    
    List<LoginAttempt> findByUsuarioIdOrderByTimestampDesc(Long usuarioId);
    
    List<LoginAttempt> findByNombreUsuarioOrderByTimestampDesc(String nombreUsuario);
    
    @Query("SELECT l FROM LoginAttempt l WHERE l.usuarioId = :usuarioId " +
           "AND l.exitoso = false ORDER BY l.timestamp DESC LIMIT 3")
    List<LoginAttempt> findUltimosIntentosFallidos(@Param("usuarioId") Long usuarioId);
    
    @Query("SELECT COUNT(l) FROM LoginAttempt l WHERE l.usuarioId = :usuarioId " +
           "AND l.exitoso = false AND l.timestamp > :desde")
    Integer countIntentosFallidosRecientes(@Param("usuarioId") Long usuarioId, 
                                           @Param("desde") LocalDateTime desde);
    
    Page<LoginAttempt> findByUsuarioIdOrderByTimestampDesc(Long usuarioId, Pageable pageable);
}
