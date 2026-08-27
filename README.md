# Ticketflow API

![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.5-brightgreen?logo=springboot)
![Maven](https://img.shields.io/badge/Maven-3.8%2B-blue?logo=apachemaven)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-9.3-336791?logo=postgresql)
![H2](https://img.shields.io/badge/H2-dev-lightgrey)
![License](https://img.shields.io/badge/license-MIT-green)

API REST de **mesa de ayuda (helpdesk)**: gestión de tickets con SLA por prioridad, máquina de estados, asignación de agentes, comentarios y dashboard de estadísticas. Actúa como *resource server* validando tokens JWT emitidos por `auth-service-jwt`.

## Stack

| Componente | Versión |
|---|---|
| Java | 17 |
| Spring Boot | 3.2.5 |
| Spring Data JPA + Bean Validation + Spring Security | (incluidos en Boot) |
| Lombok | (incluido en Boot) |
| SpringDoc OpenAPI (Swagger UI) | 2.5.0 |
| jjwt (io.jsonwebtoken) | 0.12.5 |
| PostgreSQL (local) | 9.3 |
| H2 (solo perfil `dev` y tests) | (incluida en Boot) |
| Tests | JUnit 5 + Mockito + MockMvc (sin TestContainers) |

## Ejecución

### Perfil por defecto (PostgreSQL 9.3 local)

1. Crea la base y el usuario (como superusuario `postgres`):

```sql
CREATE DATABASE ticketflow;
CREATE USER auth_user WITH PASSWORD 'auth_pass';
```

2. Crea el esquema a mano (ver sección [Compatibilidad PostgreSQL 9.3](#compatibilidad-postgresql-93)):

```bash
psql -U postgres -d ticketflow -f src/main/resources/db/manual/schema_pg93.sql
```

3. Arranca la app:

```bash
mvn spring-boot:run
```

Variables de entorno soportadas (con defaults locales): `DB_HOST` (localhost), `DB_PORT` (5432), `DB_NAME` (ticketflow), `DB_USER` (auth_user), `DB_PASS` (auth_pass), `JWT_SECRET`.

La API queda en **http://localhost:8081** (el 8080 lo usa otro servicio).

### Perfil dev (H2 en memoria, sin PostgreSQL)

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

H2 arranca en memoria (`jdbc:h2:mem:ticketflowdb;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE`) con `ddl-auto=create-drop`, y la consola H2 queda disponible en `http://localhost:8081/h2-console`.

### Tests

```bash
mvn test
```

### Documentación interactiva

Swagger UI (público, sin token): **http://localhost:8081/swagger-ui.html**

## Endpoints

| Método | Ruta | Descripción | Rol |
|---|---|---|---|
| POST | `/api/tickets` | Crear ticket (status=OPEN, calcula dueAt según SLA) | autenticado |
| GET | `/api/tickets` | Listar con filtros `?status=&priority=&assigneeEmail=` | autenticado |
| GET | `/api/tickets/{id}` | Obtener un ticket | autenticado |
| PUT | `/api/tickets/{id}` | Editar title/description/priority | autenticado |
| PATCH | `/api/tickets/{id}/status` | Cambiar estado (valida transición) | **ROLE_ADMIN** |
| PATCH | `/api/tickets/{id}/assign` | Asignar agente (`assigneeEmail`) | **ROLE_ADMIN** |
| POST | `/api/tickets/{id}/comments` | Agregar comentario | autenticado |
| GET | `/api/tickets/{id}/comments` | Listar comentarios del ticket | autenticado |
| GET | `/api/dashboard/stats` | Conteos por estado/prioridad + overdue | **ROLE_ADMIN** |

Todos los endpoints requieren header `Authorization: Bearer <token>` (HS256, emitido por `auth-service-jwt`, claim `type=access` y claim `roles`). Swagger (`/swagger-ui.html`, `/v3/api-docs/**`) es público. Los errores 401/403 se devuelven en JSON con el mismo formato que el resto de errores de la API.

## Ejemplos curl

```bash
# Crear ticket (calcula dueAt según prioridad: CRITICAL=4h, HIGH=24h, MEDIUM=72h, LOW=7d)
curl -X POST http://localhost:8081/api/tickets \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
        "title": "No funciona el correo",
        "description": "El cliente de correo no sincroniza desde ayer",
        "priority": "HIGH",
        "reporterEmail": "usuario@tucv.com"
      }'

# Listar con filtros
curl "http://localhost:8081/api/tickets?status=OPEN&priority=HIGH" \
  -H "Authorization: Bearer $TOKEN"

# Editar
curl -X PUT http://localhost:8081/api/tickets/1 \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"title": "Correo caído", "description": "Sigue sin sincronizar", "priority": "CRITICAL"}'

# Cambiar estado (ROLE_ADMIN)
curl -X PATCH http://localhost:8081/api/tickets/1/status \
  -H "Authorization: Bearer $TOKEN_ADMIN" \
  -H "Content-Type: application/json" \
  -d '{"status": "IN_PROGRESS"}'

# Asignar agente (ROLE_ADMIN)
curl -X PATCH http://localhost:8081/api/tickets/1/assign \
  -H "Authorization: Bearer $TOKEN_ADMIN" \
  -H "Content-Type: application/json" \
  -d '{"assigneeEmail": "agente@tucv.com"}'

# Agregar y listar comentarios
curl -X POST http://localhost:8081/api/tickets/1/comments \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"authorEmail": "agente@tucv.com", "message": "Estoy revisando el caso"}'

curl http://localhost:8081/api/tickets/1/comments -H "Authorization: Bearer $TOKEN"

# Dashboard (ROLE_ADMIN)
curl http://localhost:8081/api/dashboard/stats -H "Authorization: Bearer $TOKEN_ADMIN"
```

## Máquina de estados

```
            ┌──────────────────────────────────────┐
            │                                      │
            ▼                                      │
        ┌──────┐        ┌─────────────┐        ┌──┴───────┐        ┌────────┐
        │ OPEN │ ─────► │ IN_PROGRESS │ ─────► │ RESOLVED │ ─────► │ CLOSED │
        └──────┘        └─────────────┘        └──────────┘        └────────┘
                                                    ▲  │
                                                    └──┘   (reopen: limpia resolvedAt)
```

- Flujo principal: `OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED` (CLOSED es terminal).
- Reapertura permitida: `RESOLVED -> IN_PROGRESS` (limpia `resolvedAt`).
- Al pasar a `RESOLVED` se guarda `resolvedAt`.
- Cualquier otra transición devuelve **409 Conflict** con mensaje claro.

**Indicador overdue**: un ticket está vencido si su estado es `OPEN` o `IN_PROGRESS` y su `dueAt` ya pasó. Se expone por ticket (campo `overdue` en la respuesta) y agregado en `/api/dashboard/stats` (`overdueCount`).

## Formato de errores

Todas las respuestas de error siguen el mismo contrato JSON:

```json
{
  "timestamp": "2024-06-01T12:34:56",
  "status": 400,
  "error": "Bad Request",
  "message": "Error de validación en los datos enviados",
  "path": "/api/tickets",
  "fieldErrors": {
    "title": "El título es obligatorio"
  }
}
```

`fieldErrors` solo aparece en errores 400 de validación. Códigos: 400 (validación / JSON mal formado), 401 (sin token o inválido), 403 (sin ROLE_ADMIN), 404 (recurso no encontrado), 409 (transición de estado inválida), 500 (error interno).

## Compatibilidad PostgreSQL 9.3

Esta instancia local usa **PostgreSQL 9.3**, lo que impone restricciones importantes:

1. **Sin Flyway ni Liquibase.** Ninguna versión Community de Flyway soporta PostgreSQL 9.3 (ni siquiera la 6.5.7; exige la edición de pago). Por eso el `pom.xml` no incluye ninguna herramienta de migraciones.
2. **Esquema creado a mano.** Todo el DDL vive en `src/main/resources/db/manual/schema_pg93.sql`, en SQL estándar compatible con 9.3 (`BIGSERIAL`, `TIMESTAMP`, `VARCHAR`, `BOOLEAN`; sin identity columns, sin `ON CONFLICT`, sin `JSONB`). Se ejecuta una sola vez con el superusuario `postgres`:

   ```bash
   psql -U postgres -d ticketflow -f src/main/resources/db/manual/schema_pg93.sql
   ```

3. **Permisos al final del script.** La aplicación conecta con `auth_user`, pero las tablas las crea `postgres`, así que el script termina siempre con:

   ```sql
   GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA public TO auth_user;
   GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA public TO auth_user;
   ```

   (Los `GRANT ... ON ALL SEQUENCES` son necesarios porque `BIGSERIAL` crea secuencias que `auth_user` debe poder leer en los `INSERT`.)

4. **Hibernate solo valida.** En `application.yml` se usa `spring.jpa.hibernate.ddl-auto: validate`: al arrancar, Hibernate comprueba que el esquema creado a mano coincide con las entidades y **no ejecuta DDL**. Si cambias una entidad, actualiza también `schema_pg93.sql`. En el perfil `dev` (H2) se usa `create-drop`, así que puedes desarrollar sin PostgreSQL.

5. **Driver JDBC.** Spring Boot 3.2.5 gestiona el driver `postgresql` 42.7.3, que sigue siendo compatible con servidores 9.1+ (el propio pgJDBC solo deja de *garantizar* compatibilidad con servidores anteriores a 9.1 a partir de la 42.7.4), así que funciona con PostgreSQL 9.3 sin cambiar nada del `pom.xml`.

## Estructura del proyecto

```
ticketflow-api
├── pom.xml
├── .gitignore
├── README.md
└── src
    ├── main
    │   ├── java/com/tucv/ticketflow
    │   │   ├── TicketflowApplication.java
    │   │   ├── config/          (JpaAuditingConfig, OpenApiConfig)
    │   │   ├── enums/           (TicketStatus, TicketPriority)
    │   │   ├── entity/          (BaseEntity, Ticket, TicketComment)
    │   │   ├── repository/      (TicketRepository, TicketCommentRepository)
    │   │   ├── dto/             (request/, response/, mapper/)
    │   │   ├── exception/       (ResourceNotFoundException,
    │   │   │                     InvalidStateTransitionException, GlobalExceptionHandler)
    │   │   ├── security/        (JwtService, JwtAuthenticationFilter, SecurityConfig,
    │   │   │                     RestAuthenticationEntryPoint, RestAccessDeniedHandler)
    │   │   ├── service/         (interfaces) + service/impl/
    │   │   └── controller/      (TicketController, CommentController, DashboardController)
    │   └── resources
    │       ├── application.yml
    │       ├── application-dev.yml
    │       └── db/manual/schema_pg93.sql
    └── test/java/com/tucv/ticketflow
        ├── service/TicketServiceImplTest.java        (Mockito: SLA y transiciones)
        ├── controller/TicketControllerTest.java      (@WebMvcTest + MockMvc)
        └── repository/TicketRepositoryTest.java      (@DataJpaTest sobre H2)
```
