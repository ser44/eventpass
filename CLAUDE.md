# EventPass — Guía de desarrollo para Claude Code

> Ubicá este archivo en la raíz del repositorio como `CLAUDE.md`.
> Es la fuente de verdad para construir el prototipo del Trabajo Final Grupal de **Programación de Vanguardia** (Universidad de la Ciudad de Buenos Aires, 2º cuatrimestre 2026). El documento técnico del grupo (`docs/TP_Final_EventPass_Programacion_de_Vanguardia.docx`) describe el diseño; este archivo lo traduce a instrucciones de implementación. Si algo de acá contradice al documento, **frená y preguntá** antes de decidir.

---

## 0. Reglas de trabajo para Claude Code

1. **Trabajá por fases** (sección 12). Al terminar cada fase: corré los tests, verificá el checklist de la fase, hacé un commit con mensaje convencional (`feat:`, `fix:`, `test:`, `chore:`, `docs:`) y reportá un resumen breve antes de seguir.
2. **No debilites tests para que pasen.** Si un test falla, arreglá el código. Si creés que el test está mal, explicá por qué y pedí confirmación antes de cambiar una aserción.
3. **No agregues alcance** que no esté en este archivo (pagos, butacas numeradas, reventa, multi-idioma, app nativa). Si ves una mejora valiosa, proponela al final del reporte de la fase, no la implementes.
4. **No cambies decisiones de arquitectura** (sección 3) sin preguntar: monolito modular, UPDATE condicional para el cupo, outbox + worker, JWT stateless, Flyway.
5. **Nada de secretos en el repo.** Todo por variables de entorno; mantené `.env.example` actualizado.
6. **El entorno de desarrollo es Windows** (ver sección 13). Todo debe poder levantarse con `docker compose` sin depender de herramientas instaladas localmente más allá de Docker Desktop y Git.
7. **Código y nombres de dominio en español** (Evento, Reserva, Entrada), **rutas de la API en inglés** (`/events`, `/reservations`) tal como figura en el documento. Comentarios breves, en español.
8. Antes de dar una fase por terminada, **revisá el responsive** de las pantallas nuevas en 360 px, 768 px y 1280 px de ancho (sección 7.4).

---

## 1. Qué es EventPass

Plataforma web para publicar eventos con cupo limitado, reservar entradas con código QR y validar el ingreso escaneando el QR desde el celular.

**Objetivos técnicos que la demo tiene que mostrar:**

- **Nunca se vende más que el cupo**, aunque muchos usuarios reserven en simultáneo.
- **Cada entrada se usa una sola vez.**
- Las notificaciones no frenan ni rompen la reserva (outbox + worker).
- Todo es observable: métricas de negocio en Grafana.

### Roles

| Rol | Puede |
|---|---|
| `ASISTENTE` | Ver eventos publicados, reservar (1–4), confirmar, cancelar, ver "Mis entradas" con QR. |
| `ORGANIZADOR` | Todo lo anterior + crear/editar/publicar/cancelar **sus** eventos, ver estadísticas, hacer check-in en **sus** eventos. |
| `ADMIN` | Gestionar usuarios y roles, dar de baja cualquier evento, ver métricas globales. |

Usuarios no autenticados pueden ver el catálogo y el detalle de eventos publicados.

### Reglas de negocio (implementarlas todas y cubrirlas con tests)

| ID | Regla |
|---|---|
| RN1 | Solo se reserva sobre eventos `PUBLICADO` con `fechaInicio` futura. |
| RN2 | Una reserva tiene entre 1 y 4 entradas. |
| RN3 | Al reservar, el cupo queda retenido **10 minutos** (`app.reserva.ttl-minutos`, configurable). Si no se confirma, expira y el cupo vuelve. |
| RN4 | `cupo_disponible` nunca es negativo (invariante; protegido también por `CHECK` en la base). |
| RN5 | Una reserva confirmada puede cancelarse hasta **48 h** antes del evento (`app.reserva.cancelacion-horas`); sus entradas quedan `ANULADA`. |
| RN6 | Cada entrada se valida una sola vez; el segundo escaneo se rechaza informando `usadaEn`. |
| RN7 | El organizador no puede bajar `cupoTotal` por debajo de las entradas ya retenidas/emitidas. |

### Fuera de alcance

Pagos reales (la confirmación simula el checkout), butacas numeradas, reventa/transferencia, multi-idioma, multi-moneda, app nativa, recuperación de contraseña por mail.

---

## 2. Stack y versiones

| Capa | Tecnología |
|---|---|
| Frontend | **Angular 18** (standalone components, signals, nuevo control flow `@if/@for`), Angular Material 18, SCSS, `angularx-qrcode`, `@zxing/ngx-scanner` (verificar versión compatible con Angular 18) |
| Backend | **Java 21**, **Spring Boot 3.3.x**, Maven (con wrapper `mvnw`), Spring Web, Spring Security + OAuth2 Resource Server (JWT HS256), Spring Data JPA, Bean Validation, Flyway, Actuator, Micrometer Prometheus, MapStruct, springdoc-openapi, logstash-logback-encoder |
| Base de datos | **PostgreSQL 16** |
| Tests | JUnit 5, AssertJ, Mockito, Spring Boot Test, MockMvc, **Testcontainers**, JaCoCo · Jasmine/Karma (ChromeHeadless) · **Playwright** · **k6** |
| Infra | Docker, Docker Compose, Nginx 1.27, Mailpit, Prometheus, Grafana, Loki + Promtail |
| CI/CD | GitHub Actions, GitHub Container Registry (GHCR) |

No uses Lombok en las entidades de dominio (queremos encapsulamiento explícito); en DTOs usá `record`.

---

## 3. Arquitectura (no cambiar sin preguntar)

```
Navegador (Angular SPA) ──HTTPS──> nginx (frontend) ──/api──> api (Spring Boot) ──JDBC──> PostgreSQL
                                                                worker (misma imagen, perfil worker) ──> PostgreSQL / SMTP
```

- **Monolito modular en capas**, organizado por funcionalidad: `controller → service → dominio → repository`.
- **Un solo código backend, dos perfiles**: `api` (expone HTTP, no corre jobs) y `worker` (sin endpoints de negocio; corre el job de expiración y el despachador de outbox; expone solo Actuator).
- **Nginx** sirve la SPA y hace proxy de `/api/` hacia `api:8080`. El navegador ve un único origen: **no configurar CORS** salvo para `ng serve` local (perfil `dev`, origen `http://localhost:4200`).

### 3.1 Control de concurrencia del cupo (decisión central)

Nunca leer-comparar-guardar el cupo en Java. Usar **una sola sentencia atómica**:

```java
@Modifying(clearAutomatically = true, flushAutomatically = true)
@Query("""
  update Evento e set e.cupoDisponible = e.cupoDisponible - :cantidad
  where e.id = :id and e.estado = ar.com.eventpass.evento.EstadoEvento.PUBLICADO
    and e.fechaInicio > CURRENT_TIMESTAMP and e.cupoDisponible >= :cantidad
""")
int descontarCupo(UUID id, int cantidad); // 0 filas => sin cupo / no reservable
```

- Si devuelve 0: distinguir causa con una lectura posterior (evento inexistente → 404, no publicado o pasado → 422, sin cupo → 409 `CUPO_AGOTADO` con `cupoDisponible` actual).
- La restitución de cupo (`expirar`, `cancelar`) también es un UPDATE atómico `+ :cantidad`, con guarda `cupo_disponible + :cantidad <= cupo_total`.
- Migración: `CHECK (cupo_disponible >= 0)` y `CHECK (cupo_disponible <= cupo_total)`.
- El campo `version` (`@Version`) de `Evento` se usa para el bloqueo optimista en **ediciones del organizador**, no para el descuento de cupo. Ojo: el UPDATE JPQL no incrementa `version`; está bien así.

### 3.2 Outbox

- `ReservaService.confirmar()` publica `ReservaConfirmadaEvent` (también `ReservaCanceladaEvent`).
- Un listener con `@TransactionalEventListener(phase = BEFORE_COMMIT)` inserta una fila en `mensaje_outbox` **en la misma transacción**.
- El worker (`DespachadorOutbox`, `@Scheduled(fixedDelay = 5s)`) toma lotes con `SELECT … FOR UPDATE SKIP LOCKED LIMIT 20`, envía por el `CanalNotificacion` correspondiente y marca `ENVIADO` o incrementa `intentos` con backoff (`proximo_intento`). Tras 5 intentos → `FALLIDO` + log ERROR.

### 3.3 Expiración

- `ExpiracionReservasJob` (worker, `@Scheduled(fixedDelay = 15s)`): busca reservas `PENDIENTE` con `expira_en < now()` usando `FOR UPDATE SKIP LOCKED`, las pasa a `EXPIRADA` y restituye cupo en la misma transacción.
- Inyectar `Clock` en servicios y dominio (bean configurable) para poder testear tiempos.

---

## 4. Estructura del repositorio

```
eventpass/
├─ CLAUDE.md
├─ README.md                    # cómo levantar, usuarios semilla, URLs, guion de demo
├─ .env.example
├─ .gitattributes               # * text=auto eol=lf  (+ *.cmd eol=crlf, *.ps1 eol=crlf)
├─ .editorconfig
├─ docker-compose.yml           # base: frontend, api, worker, db
├─ docker-compose.override.yml  # dev: mailpit, puertos de debug, db expuesta en 5432
├─ docker-compose.obs.yml       # prometheus, grafana, loki, promtail
├─ docs/                        # documento técnico, diagramas
├─ backend/
│  ├─ Dockerfile
│  ├─ mvnw, mvnw.cmd, pom.xml
│  └─ src/main/java/ar/com/eventpass/
│     ├─ evento/         EventoController, EventoService, Evento, EstadoEvento, EventoRepository, dto/
│     ├─ reserva/        ReservaController, ReservaService, Reserva, EstadoReserva, ReservaRepository, ExpiracionReservasJob, dto/
│     ├─ entrada/        CheckInController, CheckInService, Entrada, EstadoEntrada, EntradaRepository, GeneradorCodigo, dto/
│     ├─ usuario/        AuthController, UsuarioService, Usuario, Rol, UsuarioRepository, JwtService, AdminController, dto/
│     ├─ notificacion/   CanalNotificacion, EmailNotificador, LogNotificador, OutboxListener, DespachadorOutbox, MensajeOutbox
│     ├─ idempotencia/   IdempotencyKey (entidad), IdempotenciaService
│     └─ comun/          GlobalExceptionHandler, DominioException y subclases, SecurityConfig, ClockConfig, MetricasNegocio, DataSeeder
├─ frontend/
│  ├─ Dockerfile
│  ├─ nginx.conf
│  └─ src/app/ core/ shared/ features/{catalogo,reservas,mis-entradas,organizador,admin,auth}
├─ e2e/                         # Playwright
├─ load/                        # k6: apertura-venta.js
├─ ops/                         # prometheus.yml, grafana provisioning (datasources + dashboards JSON), promtail.yml, nginx certs dev
└─ .github/workflows/           # ci.yml, cd.yml
```

Cada módulo expone servicios y DTOs; entidades y repositorios no se usan desde otros módulos (si `reserva` necesita algo de `evento`, lo pide a `EventoService` o usa el repositorio solo para el UPDATE atómico, documentándolo).

---

## 5. Modelo de datos (Flyway `V1__esquema.sql` en adelante)

Todas las PK `uuid` (generadas en Java con `UUID.randomUUID()`), fechas `timestamptz`.

- **usuario**: `id, email (unique, lower), nombre, password_hash (BCrypt), rol (ASISTENTE|ORGANIZADOR|ADMIN), activo, creado_en`
- **evento**: `id, organizador_id FK, titulo, descripcion, lugar, fecha_inicio, cupo_total, cupo_disponible, estado (BORRADOR|PUBLICADO|CANCELADO|FINALIZADO), imagen_url null, version, creado_en, actualizado_en` · CHECKs de cupo · índice `(estado, fecha_inicio)`
- **reserva**: `id, evento_id FK, usuario_id FK, cantidad CHECK 1..4, estado (PENDIENTE|CONFIRMADA|EXPIRADA|CANCELADA), expira_en, creada_en, confirmada_en, cancelada_en` · índice parcial `(expira_en) WHERE estado='PENDIENTE'`
- **entrada**: `id, reserva_id FK, evento_id FK, codigo (unique), estado (VALIDA|USADA|ANULADA), usada_en, validada_por FK usuario null`
- **mensaje_outbox**: `id, tipo, payload jsonb, estado (PENDIENTE|ENVIADO|FALLIDO), intentos, proximo_intento, creado_en, enviado_en, ultimo_error` · índice `(estado, proximo_intento)`
- **idempotency_key**: `clave, usuario_id, endpoint, status_code, respuesta jsonb, creada_en` · PK `(clave, usuario_id)` · limpiar > 24 h

**Código de entrada**: `EVP-XXXX-XXXX-XXXX` con alfabeto sin caracteres ambiguos (sin 0/O, 1/I/L), generado con `SecureRandom`. Es lo que se codifica en el QR.

---

## 6. Backend

### 6.1 Dominio rico (patrón State)

- `Evento`: sin setter público de cupo. Métodos: `publicar()`, `cancelar()`, `actualizarDatos(...)`, `cambiarCupoTotal(nuevo)` (valida RN7).
- `Reserva`: `confirmar(Clock)`, `cancelar(Clock, fechaEvento)`, `expirar(Clock)`. Cada método valida el estado actual y lanza `TransicionInvalidaException` si no corresponde. `confirmar` emite las entradas y registra `ReservaConfirmadaEvent` (usar `AbstractAggregateRoot` o publicar desde el servicio).
- `Entrada`: `validarIngreso(Clock, usuarioStaff)`, `anular()`.
- Jerarquía de excepciones: `DominioException` → `CupoAgotadoException`, `ReservaExpiradaException`, `EntradaYaUsadaException`, `EntradaInexistenteException`, `TransicionInvalidaException`, `CancelacionFueraDePlazoException`, `EventoNoReservableException`, `AccesoDenegadoException`.

### 6.2 Patrones a dejar visibles en el código (los vamos a mostrar en la presentación)

Repository · DTO + Mapper (MapStruct) · State · Strategy (`CanalNotificacion` con `EmailNotificador` y `LogNotificador`, seleccionable por propiedad) · Observer (eventos de dominio) · Transactional Outbox · capas.

### 6.3 API (prefijo `/api/v1`)

| Método y ruta | Rol | Respuestas |
|---|---|---|
| `POST /auth/register` | Público (crea ASISTENTE) | 201, 400, 409 email duplicado |
| `POST /auth/login` | Público | 200 `{ token, usuario }`, 401 |
| `GET /auth/me` | Autenticado | 200 |
| `GET /events?q=&from=&to=&page=&size=` | Público | 200 paginado (solo PUBLICADO y futuros) |
| `GET /events/{id}` | Público | 200, 404 |
| `POST /events` | ORGANIZADOR | 201 (BORRADOR), 400 |
| `PUT /events/{id}` | ORGANIZADOR dueño | 200, 409 (versión o RN7) |
| `POST /events/{id}/publish` | ORGANIZADOR dueño | 200, 409 |
| `POST /events/{id}/cancel` | ORGANIZADOR dueño / ADMIN | 200 (anula entradas y notifica) |
| `GET /me/events` | ORGANIZADOR | 200 |
| `GET /events/{id}/stats` | ORGANIZADOR dueño / ADMIN | 200 `{ cupoTotal, disponibles, retenidas, vendidas, canceladas, ingresadas }` |
| `POST /events/{id}/reservations` | ASISTENTE/ORGANIZADOR | 201, 404, 409 `CUPO_AGOTADO`, 422 |
| `GET /reservations/{id}` | dueño | 200, 404 |
| `POST /reservations/{id}/confirm` | dueño | 200 con entradas, 410 `RESERVA_EXPIRADA`, 409 |
| `DELETE /reservations/{id}` | dueño | 204, 409 `CANCELACION_FUERA_DE_PLAZO` |
| `GET /me/tickets` | Autenticado | 200 |
| `POST /events/{id}/check-ins` `{ codigo }` | ORGANIZADOR dueño | 200 `VALIDA`, 404 `ENTRADA_INEXISTENTE`, 409 `ENTRADA_YA_USADA`/`ENTRADA_ANULADA`, 422 entrada de otro evento |
| `GET /admin/users`, `PATCH /admin/users/{id}` | ADMIN | 200 |

**Convenciones:**

- Errores siempre en **Problem Details (RFC 7807)** con `ProblemDetail` de Spring, incluyendo `code` estable (string en MAYÚSCULAS) y datos extra cuando aporten (`cupoDisponible`, `usadaEn`).
- Validación con Bean Validation → 400 con lista de `errores` por campo.
- Paginación: `{ contenido, pagina, tamanio, totalElementos, totalPaginas }`.
- Fechas ISO-8601 con offset. Zona por defecto `America/Argentina/Buenos_Aires`.
- `POST /events/{id}/reservations` acepta `Idempotency-Key` (UUID). Misma clave + mismo usuario → devolver la respuesta guardada sin reservar de nuevo.
- Recurso ajeno → **404** (no revelar existencia), salvo ADMIN.
- OpenAPI en `/api/v1/docs` y Swagger UI en `/api/swagger-ui`.

### 6.4 Seguridad

- Spring Security stateless, `oauth2ResourceServer().jwt()` con clave HS256 desde `JWT_SECRET` (mín. 32 bytes). Expiración 8 h. Claims: `sub` (id), `email`, `nombre`, `rol`.
- Passwords con BCrypt. Rate limiting simple en `/auth/login` (opcional, en memoria; anotarlo si no se hace).
- Actuator: `health` público; `prometheus` accesible solo desde la red interna (no proxyear `/actuator` en Nginx).

### 6.5 Datos semilla (perfil `dev` y `demo`, `DataSeeder` idempotente)

- `admin@eventpass.dev` / `Admin123!` (ADMIN)
- `orga@eventpass.dev` / `Orga123!` (ORGANIZADOR)
- `asistente1..5@eventpass.dev` / `Asis123!` (ASISTENTE)
- 6 eventos de ejemplo del organizador: uno con **cupo 50** llamado "Demo apertura de venta" (para k6), uno casi agotado (cupo 3), uno en BORRADOR, uno pasado (FINALIZADO), dos normales con fechas en las próximas semanas.
- Para k6: endpoint o script que cree **200 usuarios** `carga001..200@eventpass.dev` solo en perfil `demo`.

---

## 7. Frontend (Angular 18) — responsive obligatorio

### 7.1 Estructura

- `core/`: `AuthService` (token en `localStorage`, usuario en signal), `authInterceptor`, `errorInterceptor` (traduce `code` de Problem Details a mensajes en español y muestra `MatSnackBar`; en 401 → logout + `/login?redirect=`), `roleGuard`, modelos tipados alineados con los DTOs.
- `shared/`: `EventCardComponent`, `CountdownComponent`, `QrViewComponent`, `EmptyStateComponent`, `LoadingComponent`, `ConfirmDialogComponent`.
- `features/`: lazy loading por ruta (`loadComponent`/`loadChildren`).
- Componentes **smart** (hablan con servicios) vs **presentational** (solo `input()`/`output()`), con `ChangeDetectionStrategy.OnPush`.
- Formularios reactivos tipados.
- `environment.apiUrl = '/api/v1'` (relativo; en `ng serve` usar `proxy.conf.json` hacia `http://localhost:8080`).

### 7.2 Pantallas

| Ruta | Pantalla | Rol |
|---|---|---|
| `/login`, `/registro` | Acceso / alta | Público |
| `/eventos` | Catálogo con búsqueda y filtro por fecha | Público |
| `/eventos/:id` | Detalle + cupo disponible (refresco cada 15 s) + selector 1–4 + "Reservar" | Público (reservar pide login) |
| `/reservas/:id` | Confirmación con cuenta regresiva (tomar `expiraEn` del servidor) | Asistente |
| `/mis-entradas` | Entradas con QR grande, estado, botón cancelar | Asistente |
| `/organizador` | Mis eventos + stats | Organizador |
| `/organizador/eventos/nuevo`, `/:id/editar` | Formulario con validaciones | Organizador |
| `/organizador/check-in/:id` | Escáner QR + ingreso manual del código + resultado a pantalla completa | Organizador |
| `/admin` | Usuarios y roles | Admin |

### 7.3 Pantalla de check-in (la más importante de la demo)

- Pensada **100 % para celular**: cámara ocupando el ancho, selector de cámara (trasera por defecto).
- Resultado a pantalla completa: **verde** "Entrada válida" + nombre del titular; **rojo** "Ya utilizada a las HH:mm" / "Entrada inexistente" / "Anulada". Volver a escanear automáticamente a los 2,5 s.
- Vibración (`navigator.vibrate`) si está disponible.
- **Fallback manual**: input para tipear el código (siempre visible), por si la cámara no tiene permiso.
- Evitar validaciones duplicadas por lecturas repetidas del mismo QR en el mismo segundo (debounce por código).
- ⚠️ `getUserMedia` **requiere HTTPS** fuera de `localhost`. Para probar desde un celular en la misma red, dejar preparado en `ops/` un certificado autofirmado (o mkcert) y un server block 443 en Nginx (perfil dev), y documentar en el README la alternativa con un túnel (`cloudflared tunnel --url http://localhost`).

### 7.4 Responsive (criterios de aceptación)

- **Mobile-first**. Breakpoints: `< 600 px` (celular), `600–959 px` (tablet), `≥ 960 px` (desktop). Usar `BreakpointObserver` de CDK solo cuando cambie la estructura; el resto con CSS (grid/flex, `clamp()`).
- Navegación: en celular, **toolbar con menú hamburguesa** (`mat-sidenav` en modo `over`); en desktop, links visibles en la toolbar.
- Catálogo: grilla de cards `repeat(auto-fill, minmax(280px, 1fr))` → 1 columna en celular.
- Tablas (stats, admin): en celular se transforman en **lista de cards**; nunca scroll horizontal de la página. Si una tabla debe quedar, va dentro de un contenedor con `overflow-x: auto`.
- Botones de acción principales con alto mínimo **48 px** y área táctil cómoda; en celular, el botón "Reservar"/"Confirmar" queda **fijo abajo** (sticky) respetando `env(safe-area-inset-bottom)`.
- Formularios a una columna en celular, dos en desktop.
- Textos sin desbordar con títulos largos (`overflow-wrap: anywhere`).
- QR legible: mínimo 220 px, centrado, con el código en texto debajo. Opción de subir el brillo no es posible desde la web: mostrar el QR sobre fondo blanco puro.
- Imágenes con `max-width: 100%`, `object-fit: cover`.
- `<meta name="viewport" content="width=device-width, initial-scale=1, viewport-fit=cover">`.
- Soporte de **modo oscuro** según `prefers-color-scheme` (tema Material claro/oscuro).
- Accesibilidad básica: contraste AA, `aria-label` en botones con ícono, foco visible, navegación por teclado en desktop, `lang="es"`.
- **Verificación**: agregar en Playwright proyectos para `Pixel 7`, `iPhone 13` (WebKit), `iPad Mini` y `Desktop Chrome`, con un test de "no hay scroll horizontal" (`document.documentElement.scrollWidth <= window.innerWidth`) en cada pantalla principal, y capturas en `e2e/screenshots/` para usar en la presentación.

### 7.5 UX de errores y estados

- Toda pantalla tiene estados de **carga**, **vacío** y **error**.
- Mensajes por `code`: `CUPO_AGOTADO` → "No quedan entradas suficientes. Quedan X."; `RESERVA_EXPIRADA` → "Tu reserva venció. Volvé a intentarlo." (redirigir al detalle); `CANCELACION_FUERA_DE_PLAZO`; `ENTRADA_YA_USADA`; error de red → "Sin conexión con el servidor".
- Doble clic en "Reservar": botón deshabilitado mientras hay request en curso **y** `Idempotency-Key` generada una vez por intento.

---

## 8. Docker y entornos

- **backend/Dockerfile** multi-stage: `maven:3.9-eclipse-temurin-21` (usar cache de `~/.m2` con `--mount=type=cache`) → `eclipse-temurin:21-jre-alpine`, usuario no root, `JAVA_OPTS` con `-XX:MaxRAMPercentage=75`.
- **frontend/Dockerfile** multi-stage: `node:20-alpine` (`npm ci && npm run build`) → `nginx:1.27-alpine` con `nginx.conf` que: sirve la SPA con fallback `try_files $uri /index.html`, cachea assets con hash, hace proxy de `/api/` a `api:8080`, agrega headers de seguridad básicos y gzip.
- **docker-compose.yml**: servicios `frontend` (puerto 80 expuesto), `api`, `worker` (misma imagen que api, `SPRING_PROFILES_ACTIVE=worker`), `db` (volumen `pgdata`). Healthchecks en `api` (`/actuator/health`) y `db` (`pg_isready`); `depends_on` con `condition: service_healthy`; `restart: unless-stopped`.
- **override (dev)**: `mailpit` (8025), db expuesta en 5432, perfil Spring `dev`.
- **obs**: `prometheus` (scrapea `api:8080` y `worker:8081` en `/actuator/prometheus`), `grafana` (3000, datasources y dashboards **provisionados desde archivos**), `loki`, `promtail` (lee logs de contenedores Docker).
- Comandos objetivo (documentar en README):
  - `docker compose up --build` → app en `http://localhost`
  - `docker compose -f docker-compose.yml -f docker-compose.override.yml -f docker-compose.obs.yml up --build` → todo
- `.env.example` con `DB_PASSWORD`, `JWT_SECRET`, `SPRING_PROFILES_ACTIVE`, `SMTP_HOST`, `SMTP_PORT`, `APP_RESERVA_TTL_MINUTOS`, `GRAFANA_ADMIN_PASSWORD`.

---

## 9. Observabilidad

### 9.1 Logs

- JSON con `logstash-logback-encoder` en perfiles `docker`/`prod`; texto legible en `dev` local.
- Campos: `ts, level, service (api|worker), traceId, spanId, userId, eventoId, reservaId, accion, motivo, duracionMs`. Cargar ids en MDC desde un filtro/aspecto.
- Niveles: ERROR (requiere acción), WARN (anomalía de negocio: sin cupo, check-in duplicado, acceso denegado), INFO (hitos: reserva creada/confirmada/expirada/cancelada, evento publicado).
- **Nunca** loguear passwords, tokens ni el `Authorization`.
- Micrometer Tracing (`micrometer-tracing-bridge-otel`) para tener `traceId` y propagarlo; devolver `X-Trace-Id` en las respuestas de error para poder buscarlo en Loki.

### 9.2 Métricas de negocio (nombres exactos, usados en el documento)

| Métrica | Tipo | Tags |
|---|---|---|
| `eventpass_reservas_total` | Counter | `resultado=ok|sin_cupo|no_reservable|error` |
| `eventpass_reservas_expiradas_total` | Counter | — |
| `eventpass_checkin_total` | Counter | `resultado=valida|ya_usada|inexistente|anulada` |
| `eventpass_outbox_pendientes` | Gauge | — |
| `eventpass_reservas_vencidas_sin_procesar` | Gauge | — |
| `eventpass_cupo_disponible` | Gauge | `evento` (solo eventos publicados futuros; refrescar cada 15 s) |

Más las técnicas que da Actuator: `http_server_requests_seconds` (habilitar histograma de percentiles), `hikaricp_connections_pending`, JVM, CPU.

### 9.3 Grafana

Dos dashboards JSON provisionados en `ops/grafana/dashboards/`:

- **Salud técnica**: p95 por endpoint, tasa 5xx, pool Hikari, memoria/CPU, estado de targets.
- **Venta en vivo**: reservas por minuto por resultado, cupo disponible por evento, check-ins por resultado, outbox pendientes. (Este es el que se muestra en la demo de k6.)

Reglas de alerta provisionadas (aunque no se configure el canal): p95 `POST reservations` > 800 ms 5 min; `outbox_pendientes` > 100 por 10 min; `reservas_vencidas_sin_procesar` > 0 por 5 min; check-ins `ya_usada` > 5/min; 5xx > 2 % por 5 min; target down 1 min.

---

## 10. Testing (casos obligatorios del documento)

| ID | Tipo | Escenario | Esperado |
|---|---|---|---|
| CP01 | Unit | Confirmar reserva PENDIENTE de 3, no vencida | CONFIRMADA; 3 entradas VALIDA con códigos únicos; evento de dominio registrado |
| CP02 | Unit | Confirmar reserva vencida (Clock fijo) | `ReservaExpiradaException`; sin entradas |
| CP03 | Integración (Testcontainers) | Evento cupo 50; 200 hilos reservan 1 en simultáneo vía HTTP | Exactamente 50×201 y 150×409; `cupo_disponible = 0`; nunca negativo |
| CP04 | Integración | Reserva de 2 vencida hace 1 min; correr el job | EXPIRADA; cupo +2 |
| CP05 | Integración API | Check-in de entrada ya usada | 409 `ENTRADA_YA_USADA`; `usada_en` sin cambios |
| CP06 | Integración seguridad | ASISTENTE hace `POST /events` | 403; no se crea |
| CP07 | Unit frontend | `errorInterceptor` recibe 409 `CUPO_AGOTADO` | Muestra "No quedan entradas…" |
| CP08 | E2E Playwright | Login → reservar 2 → confirmar → Mis entradas | 2 QR visibles; mail visible en Mailpit (API de Mailpit) |
| CP09 | Carga k6 | 300 VUs, 2 min, detalle + reserva | p95 < 500 ms; 5xx < 1 %; 0 sobreventa (verificar con `/stats` al final) |

Además:

- Unit tests de **cada transición** de `Reserva` y `Entrada` (válidas e inválidas) y de RN1–RN7.
- Tests de concurrencia también para **check-in**: 10 hilos validan la misma entrada → exactamente 1 `VALIDA`.
- Test de idempotencia: dos requests con la misma `Idempotency-Key` → una sola reserva.
- Test del outbox: confirmar con SMTP caído (mock que lanza) → reserva queda CONFIRMADA y el mensaje sigue PENDIENTE con `intentos` incrementado.
- Frontend: tests de `AuthService`, `roleGuard`, `authInterceptor`, `CountdownComponent`.
- E2E responsive (sección 7.4).
- **JaCoCo**: falla el build si la cobertura de líneas de los paquetes de dominio y servicios < 70 %.
- Testcontainers con imagen `postgres:16-alpine`, mismo esquema vía Flyway (nada de H2).
- `load/apertura-venta.js` (k6): parametrizable por `BASE_URL` y `EVENTO_ID`; login de los usuarios de carga en `setup()`; umbrales (`thresholds`) que hacen fallar el script si no se cumplen.

---

## 11. CI/CD (GitHub Actions)

- **ci.yml** (PR y push a main), jobs en paralelo:
  - `backend`: `./mvnw -B verify` (unit + integración con Testcontainers + JaCoCo), publicar reporte como artifact.
  - `frontend`: `npm ci`, `npm run lint`, `npm test -- --watch=false --browsers=ChromeHeadless`, `npm run build`.
  - `docker`: si los dos anteriores pasan, build de imágenes `ghcr.io/<owner>/eventpass-frontend` y `eventpass-api` con tag `sha-<commit>`; push solo en `main`.
- **cd.yml** (en push a main, después de CI):
  - `staging`: deploy por SSH (`docker compose pull && docker compose up -d`) usando secrets `STAGING_HOST`, `STAGING_SSH_KEY`; luego smoke test `/api/v1/actuator/health` (o `/api/v1/events`) y Playwright contra staging.
  - `production`: `environment: production` con **aprobación manual**; mismo procedimiento con el tag aprobado. Documentar rollback (re-deploy del tag anterior).
  - Si no hay servidores todavía, dejar los jobs de CD con `if: vars.DEPLOY_ENABLED == 'true'` para que el pipeline no falle.
- Agregar `dependabot.yml` (maven, npm, docker, github-actions) — opcional.

---

## 12. Plan por fases (checkpoints)

**Fase 0 — Esqueleto**
- Repo, `.gitattributes`, `.editorconfig`, README inicial, `.env.example`.
- Backend Spring Boot vacío con Actuator + Flyway + conexión a Postgres; frontend Angular con Material, layout responsive (toolbar + sidenav) y página "Hola".
- Dockerfiles y `docker-compose.yml` funcionando: `http://localhost` muestra el front y `/api/v1/actuator/health` responde UP.
- ✅ `docker compose up --build` funciona en Windows con Docker Desktop.

**Fase 1 — Usuarios y seguridad**
- Migración `usuario`, registro, login JWT, `/auth/me`, roles, `DataSeeder`.
- Front: login, registro, interceptores, guard, menú según rol.
- Tests: CP06 + login ok/fallido.

**Fase 2 — Eventos**
- Migración `evento`, ABM del organizador, publicar/cancelar, catálogo público paginado, detalle.
- Front: catálogo responsive, detalle, panel de organizador, formulario.
- Tests de RN1 y RN7.

**Fase 3 — Reservas y entradas (núcleo)**
- Migraciones `reserva`, `entrada`, `idempotency_key`. UPDATE atómico, reservar, confirmar, cancelar, `/me/tickets`.
- Front: flujo de reserva con countdown, Mis entradas con QR, cancelación.
- Tests: CP01, CP02, CP03, idempotencia, RN2–RN5.

**Fase 4 — Worker: expiración y notificaciones**
- Perfil `worker`, `ExpiracionReservasJob`, outbox, `EmailNotificador` (Mailpit) con plantilla HTML simple que incluye los códigos, `LogNotificador`.
- Tests: CP04, outbox con SMTP caído.

**Fase 5 — Check-in**
- Endpoint de check-in, stats del evento.
- Front: pantalla de escáner mobile-first con fallback manual; stats en el panel.
- HTTPS dev para probar desde el celular.
- Tests: CP05, concurrencia de check-in.

**Fase 6 — Observabilidad**
- Logs JSON + MDC + traceId, métricas de negocio, `docker-compose.obs.yml`, Prometheus, Loki/Promtail, dashboards y alertas provisionados.
- ✅ Grafana muestra "Venta en vivo" con datos al reservar desde la UI.

**Fase 7 — Admin, pulido y responsive**
- Pantalla admin (usuarios/roles), estados vacío/error/carga en todas las pantallas, modo oscuro, accesibilidad, revisión responsive completa.
- Tests unitarios de frontend (CP07 y los de la sección 10).

**Fase 8 — E2E, carga y CI/CD**
- Playwright (CP08 + suite responsive con capturas), k6 (CP09), workflows de GitHub Actions.
- README final: requisitos, cómo levantar, usuarios, URLs, cómo correr cada tipo de test, **guion de demo** (sección 14), troubleshooting en Windows.

---

## 13. Consideraciones para Windows

- `.gitattributes` con `* text=auto eol=lf` para que los scripts `.sh`, `mvnw` y archivos que se copian a contenedores Linux no rompan por CRLF. Dar permiso de ejecución a `mvnw` en git (`git update-index --chmod=+x backend/mvnw`).
- No usar nombres de archivo reservados de Windows (`con`, `aux`, `nul`, `prn`, `com1`…) ni rutas muy largas.
- Los comandos del README deben funcionar en **PowerShell** y en Git Bash; cuando haya diferencias, mostrar ambos. Preferir que todo corra dentro de contenedores (por ejemplo, k6 con `docker run --rm -i grafana/k6 run - < load/apertura-venta.js`, ojo que en PowerShell la redirección `<` no existe: usar `Get-Content load/apertura-venta.js | docker run --rm -i grafana/k6 run -`).
- Testcontainers necesita Docker Desktop corriendo; documentarlo.
- En volúmenes bind de Windows, evitar depender de permisos Unix; para Postgres usar volumen nombrado.

---

## 14. Guion de la demo (debe poder ejecutarse de punta a punta)

1. Levantar todo con observabilidad. Abrir Grafana → "Venta en vivo".
2. Correr k6 contra "Demo apertura de venta" (cupo 50) con 200 usuarios: el cupo baja a 0 en el panel, 50 reservas ok y el resto `sin_cupo`; `/stats` confirma 0 sobreventa.
3. Desde el celular (HTTPS dev o túnel): un asistente reserva 2 entradas, confirma, ve los QR; el mail aparece en Mailpit.
4. El organizador abre check-in en otro celular, escanea un QR → verde; escanea el mismo → rojo "Ya utilizada a las HH:mm".
5. Mostrar en Loki el log del rechazo filtrando por `traceId`.
6. Mostrar la app en celular, tablet y desktop (capturas de Playwright como respaldo).

---

## 15. Definición de terminado (global)

- [ ] `docker compose up --build` levanta la app sin pasos manuales extra.
- [ ] Todos los tests (unit, integración, frontend) pasan en CI; cobertura ≥ 70 % en dominio y servicios.
- [ ] CP01–CP09 implementados y referenciados por ID en el nombre o `@DisplayName` del test.
- [ ] Sin scroll horizontal en 360 px; check-in usable con una mano en celular.
- [ ] Swagger UI documenta todos los endpoints con ejemplos.
- [ ] Dashboards de Grafana se cargan solos al levantar el stack de observabilidad.
- [ ] README completo y probado en Windows.
- [ ] Ningún secreto commiteado.
