# Microservicio Usuarios — Documentación

## Propósito
Este microservicio gestiona la autenticación, administración y auditoría de usuarios del ecosistema SmartLogix. Proporciona:

- Registro y login (JWT).
- CRUD de usuarios con controles de roles y permisos.
- Auditoría de acciones (eventos publicados internamente).
- Protección contra intentos de login masivos y bloqueo temporal.

## Características principales
- Autenticación basada en JWT. Los endpoints `/api/usuarios/login` y `/api/usuarios/register` son públicos; el resto requiere token.
- Roles: `ADMINISTRADOR`, `VENDEDOR`, `USUARIO` (definidos en `model/Rol.java`).
- Auditoría: todas las operaciones relevantes publican eventos `UsuarioEvent` y se guardan logs a través de `AuditService`.
- Seguridad adicional: validaciones para proteger al "admin base" (no puede eliminarse ni desactivarse desde API ordinaria).

## Puertos y configuración
- Puerto por defecto: 8081 (`server.port` en `src/main/resources/application.properties`).
- Variables relevantes (en `application.properties`):
  - `spring.datasource.*` (MySQL)
  - `jwt.secret` (clave HMAC para tokens)
  - `jwt.expiration` (ms)
  - `app.seed.admin.*` (admin seeded en arranque)
  - `app.security.max-login-attempts` y `app.security.lock-duration-minutes`

## Cómo ejecutar
Desde la carpeta `Usuarios`:

```powershell
# Compilar
.\mvnw.cmd clean package

# Ejecutar (desarrollo)
.\mvnw.cmd spring-boot:run
```

La app levantará en `http://localhost:8081` por defecto.

## Endpoints principales

Base: `/api/usuarios`

- POST /register
  - Público
  - Body: `UsuarioRequest` (nombre, email, contrasena, rol opcional)
  - Respuesta: `UsuarioResponse` (201)

- POST /login
  - Público
  - Body: `LoginRequest` (nombre, contrasena)
  - Respuesta: `LoginResponse` con `token` JWT

- GET /
  - Requiere permiso `ADMINISTRACION`
  - Parámetros: filtros `nombre`, `email`, `rol`, `esActivo`, paginación `page`, `size`, `sortBy`, `sortDir`
  - Respuesta: `PageResponse<UsuarioResponse>`

- GET /{id}
  - Requiere `ADMINISTRACION`
  - Respuesta: `UsuarioResponse`

- GET /rol/{rol}
  - Requiere `ADMINISTRACION`
  - Listado por rol

- PUT /{id}
  - Requiere `ADMINISTRACION` o que el propio usuario haga la petición (expresado en `@PreAuthorize` y `esUsuarioActual`)
  - Body: `UsuarioRequest` (para modificar datos)
  - Respuesta: `UsuarioResponse`

- DELETE /{id}
  - Requiere `ADMINISTRACION`
  - No permite eliminar al `admin base`

- PATCH /{id}/desactivar
  - Requiere `ADMINISTRACION`
  - Desactiva la cuenta (marca tokens inválidos desde la desactivación)

- PATCH /{id}/desbloquear
  - Requiere `ADMINISTRACION`
  - Quita bloqueo por intentos fallidos

## Endpoints de auditoría
Base: `/api/auditoria` (controlador `AuditController`)

- GET /
  - Requiere `ADMINISTRACION`
  - Lista logs de auditoría (paginado)

- GET /usuario/{usuarioAutenticado}
  - Requiere `ADMINISTRACION`
  - Lista auditoría filtrada por usuario

## DTOs importantes (resumen)
- `UsuarioRequest`:
  - `nombre` (String, requerido)
  - `email` (String, requerido, email)
  - `contrasena` (String, requerido, min 8)
  - `contrasenaActual` (opcional, para cambiar contraseña)
  - `rol` (opcional)

- `UsuarioResponse`:
  - `id`, `nombre`, `email`, `esActivo`, `rol`, `permiso`, `permisos`

- `LoginRequest`:
  - `nombre`, `contrasena`

- `LoginResponse`:
  - `token`, `nombre`, `rol`, `permiso`, `permisos`

- `PageResponse<T>`: envoltorio para respuestas paginadas (mapea `Page<T>` a un objeto serializable).

## Seguridad y tokens
- Implementado en `security/JwtFilter.java` y `security/SecurityConfig.java`.
- El filtro valida:
  - Formato y firma HS256 del token.
  - Si el usuario existe y está activo.
  - Si el token fue emitido antes de una desactivación (se invalida).
- Para invocar endpoints protegidos, incluir header:

```
Authorization: Bearer {token}
```

- Endpoints `/api/usuarios/login` y `/api/usuarios/register` están permitidos sin token.

## Reglas de negocio relevantes
- El `admin base` (seeded en arranque) no puede eliminarse ni desactivarse por la API.
- Solo el `admin base` puede asignar o cambiar el rol `ADMINISTRADOR` de otros usuarios.
- Al hacer login se registra auditoría y se gestiona bloqueo por intentos fallidos en `LoginAttemptService`.

## Ejemplos de uso (curl)

1) Registrar usuario:

```bash
curl -X POST http://localhost:8081/api/usuarios/register \
  -H "Content-Type: application/json" \
  -d '{"nombre":"cliente_demo","email":"cliente.demo@smartlogix.local","contrasena":"Cliente123*","rol":"USUARIO"}'
```

2) Login y obtención de token:

```bash
curl -X POST http://localhost:8081/api/usuarios/login \
  -H "Content-Type: application/json" \
  -d '{"nombre":"cliente_demo","contrasena":"Cliente123*"}'
```

Respuesta (ejemplo):

```json
{ "token": "eyJhbGciOiJIUzI1NiJ9...", "nombre":"cliente_demo", "rol":"USUARIO", "permisos":[] }
```

3) Llamada protegida (listar usuarios):

```bash
curl -H "Authorization: Bearer {token}" http://localhost:8081/api/usuarios
```

## Observaciones técnicas
- Implementa `ApplicationEventPublisher` para publicar `UsuarioEvent` en operaciones CRUD. No es necesario tener un broker externo; los listeners locales (`UsuarioEventListener`) manejan la persistencia de auditoría.
- Passwords se guardan con `BCryptPasswordEncoder`.
- Validaciones de requests usan `jakarta.validation`.

## Archivos clave (ubicación)
- Controladores: `src/main/java/com/smartlogix/usuarios/controller/UsuarioController.java`, `AuditController.java`
- Servicios: `src/main/java/com/smartlogix/usuarios/service/UsuarioServiceImpl.java`, `AuditServiceImpl.java`
- Seguridad: `src/main/java/com/smartlogix/usuarios/security/JwtFilter.java`, `SecurityConfig.java`, `JwtUtil.java`
- DTOs: `src/main/java/com/smartlogix/usuarios/dto/` (LoginRequest/Response, UsuarioRequest/Response)
- Configuración: `src/main/resources/application.properties`

## Pasos recomendados para integración con otros servicios
- Usar el endpoint de `login` para obtener JWT y pasarlo en `Authorization` para el resto de servicios.
- Si existe un BFF, exponer `register` y `login` a clientes públicos; para llamadas administrativas usar roles y permisos.

---

Si quieres, puedo:
- Añadir ejemplos Postman/collection.json.
- Generar una versión resumida en README.
- Documentar cada DTO con ejemplos de payload más detallados.

Documento generado: `Usuarios/USERS_SERVICE_DOC.md`
