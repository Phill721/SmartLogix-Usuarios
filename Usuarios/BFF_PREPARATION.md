# Preparación del microservicio `Usuarios` antes de construir el BFF

Este documento recoge y organiza las tareas y cambios recomendados para dejar el micro `Usuarios` listo para que el BFF lo consuma de forma segura y estable.

## Propósito
- Centralizar identidad y permisos.
- Exponer contratos claros para el BFF (perfil, validación de token, refresh tokens, documentación).
- Facilitar la integración segura (JWKS/introspección) y la sincronización de estados (carrito, perfil).

## Resumen ejecutivo (qué hacer primero)
1. Añadir `GET /api/usuarios/me` (perfil del usuario autenticado).
2. Definir y aplicar estrategia de validación de JWT entre servicios (HMAC compartido, RS256+JWKS o endpoint de introspección).
3. Implementar refresh tokens (endpoint `/auth/refresh`).
4. Ajustar CORS y políticas para permitir llamadas desde el BFF (o preferir que sólo el BFF hable con los micros).
5. Publicar OpenAPI/Swagger para contratos claros.

## Checklist (acciones concretas)
- [ ] Añadir `GET /api/usuarios/me` en `UsuarioController`.
- [ ] Añadir método en `UsuarioService` para obtener usuario por nombre/id.
- [ ] Decidir estrategia JWT (HMAC vs RS256 vs introspección) y documentarla.
- [ ] Implementar JWKS o endpoint `/api/usuarios/introspect` si se elige introspección.
- [ ] Crear endpoints para `refresh tokens` y su persistencia/invalidación.
- [ ] Configurar CORS (permitir BFF, bloquear llamadas directas desde frontend si se desea).
- [ ] Añadir `springdoc-openapi` y exponer `/v3/api-docs` y Swagger UI.
- [ ] Añadir pruebas básicas (curl/postman) para validar los flujos.

## Detalle por prioridad

### 1) `GET /api/usuarios/me`
- Objetivo: devolver `UsuarioResponse` del usuario autenticado (nombre, email, rol, permisos, esActivo).
- Por qué: evita duplicar lógica JWT en el BFF y centraliza la fuente de verdad.
- Cambios mínimos:
  - Archivo: `Usuarios/src/main/java/com/smartlogix/usuarios/controller/UsuarioController.java`
  - Añadir método (ejemplo):

```java
    @GetMapping("/me")
    public ResponseEntity<UsuarioResponse> me(Authentication authentication) {
        String nombre = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(usuarioService.obtenerPorNombre(nombre));
    }
```

  - Implementar `obtenerPorNombre(String nombre)` en `UsuarioService` / `UsuarioServiceImpl` si no existe (reusar `usuarioRepository.findByNombre`).

### 2) Estrategia de validación de JWT entre servicios
- Opciones:
  - Compartir secreto HMAC (fácil para MVP, requiere gestión segura del secret).
  - RS256 + JWKS (recomendado para producción): el issuer firma con la privada; los consumidores obtienen la pública desde `/.well-known/jwks.json`.
  - Endpoint de introspección (`/api/usuarios/introspect`) que valida el token y devuelve claims (más simple de implementar sin compartir secret, con latencia adicional).
- Pasos:
  - Elegir opción según nivel de seguridad.
  - Implementar en `JwtUtil` y `JwtFilter` en `Usuarios`.
  - Si RS256, añadir endpoint JWKS y actualizar `JwtUtil` para firmar con clave privada.
  - Documentar en `application.properties` y en el README de infra/ops.

### 3) Refresh tokens
- Objetivo: mejorar UX evitando re-login frecuente.
- Implementación mínima:
  - Endpoint POST `/auth/refresh` que recibe refresh token y devuelve nuevo access token (+nuevo refresh opcional).
  - Persistir/invalidar refresh tokens (BD o store). Guardar token + usuario + expiración.
  - Considerar rotating refresh tokens para más seguridad.

### 4) CORS y llamadas desde BFF
- Recomendación: preferir que **solo el BFF** sea el punto público; evita exponer micros externos.
- Si es necesario, en `SecurityConfig` habilitar `http.cors()` y crear `CorsConfigurationSource` o `WebMvcConfigurer` con orígenes permitidos.

Ejemplo mínimo (WebMvcConfigurer):

```java
@Configuration
public class CorsConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:3000") // o dominio del BFF
                .allowedMethods("GET","POST","PUT","DELETE","OPTIONS")
                .allowCredentials(true);
    }
}
```

### 5) OpenAPI / Swagger
- Añadir dependencia `org.springdoc:springdoc-openapi-starter-webmvc-ui` al `pom.xml`.
- Configurar `OpenApiConfig` si quieres metadata (título, versión, contact).
- Verificar que `/v3/api-docs` y `/swagger-ui/index.html` estén disponibles tras arrancar.

## Pruebas y comandos rápidos
- Ejecutar el micro `Usuarios`:

```powershell
cd Usuarios
.\mvnw.cmd spring-boot:run
```

- Probar login y obtener token (usar tu endpoint `/api/usuarios/login`).
- Probar `/me`:

```bash
curl -H "Authorization: Bearer <ACCESS_TOKEN>" http://localhost:8081/api/usuarios/me
```

## Notas operativas y futuras integraciones
- Evento: documentar eventos (user.created, user.updated) para que otros micros (ej. notificaciones) se suscriban.
- Carrito: sincronizar desde frontend al login usando un endpoint opcional (`/api/usuarios/{id}/carrito/sync`) o manejar en micro de `carrito` aparte.
- Seguridad: para producción migrar a RS256+JWKS y usar un vault (Azure KeyVault/HashiCorp) para claves.

## Guardar y compartir este archivo
- Puedes mantener este archivo en `Usuarios/BFF_PREPARATION.md` y actualizarlo a medida que avances.
- Cuando tengas cambios, mándame el archivo y continúo con la implementación de la siguiente prioridad.

---
Documento generado automáticamente como guía práctica para preparar `Usuarios` antes del BFF.
