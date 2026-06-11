package com.smartlogix.usuarios.dto;

import com.smartlogix.usuarios.model.Rol;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioResponse {
    private Long id;
    private String nombre;
    private String email;
    private Boolean esActivo;
    private Rol rol;
    private String permiso;
    private List<String> permisos;
}
