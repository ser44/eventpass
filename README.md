# EventPass

Plataforma web para publicar eventos con cupo limitado, reservar entradas con código QR y validar el ingreso escaneándolo desde el celular.

Trabajo Final Grupal — **Programación de Vanguardia** (Universidad de la Ciudad de Buenos Aires, 2º cuatrimestre 2026).

> Estado: **Fase 0 — Esqueleto**. Frontend "Hola" servido por nginx + backend Spring Boot con Actuator y Flyway sobre PostgreSQL, todo en Docker. Las funcionalidades (usuarios, eventos, reservas, check-in…) llegan en las fases siguientes (ver `CLAUDE.md` §12).

---

## Requisitos

- **Docker Desktop** (con el engine corriendo).
- **Git**.

No hace falta instalar Java, Maven ni Node: todo se compila dentro de contenedores.

## Cómo levantar

Desde la raíz del repo:

```bash
# 1) Copiá el archivo de entorno de ejemplo
cp .env.example .env        # PowerShell: Copy-Item .env.example .env

# 2) Levantá el stack (build incluido)
docker compose up --build
```

La primera vez tarda (descarga imágenes y compila). Cuando termine:

| Qué | URL |
|---|---|
| App (frontend) | http://localhost |
| Health del backend | http://localhost/api/v1/actuator/health |
| API directa (solo dev) | http://localhost:8080/api/v1/actuator/health |
| Mailpit (solo dev) | http://localhost:8025 |

En la página de inicio, el botón **"Probar backend"** hace un request a `/api/v1/hola` y confirma que la cadena navegador → nginx → api funciona.

Para bajar todo:

```bash
docker compose down          # agregá -v para borrar también la base
```

## Publicar una URL pública (sin servidor, sin tarjeta)

El acceso público se hace con un **túnel de Cloudflare** sobre el stack local (ver `CLAUDE.md` §7.3). Con el stack levantado:

```bash
# Git Bash / PowerShell — no requiere instalar nada (corre por Docker)
docker run --rm --network host cloudflare/cloudflared tunnel --url http://localhost:80
```

Cloudflared imprime una URL `https://<algo>.trycloudflare.com` accesible desde cualquier lado (y con HTTPS). La URL vive mientras el comando siga corriendo.

> Un link estable (named tunnel con cuenta gratuita de Cloudflare) se documentará cuando sea necesario.

### Check-in desde el celular (requiere HTTPS)

La pantalla de check-in usa la cámara (`getUserMedia`), que **fuera de `localhost` exige HTTPS**. Para escanear QR desde un celular:

- **En la PC** (navegador en `http://localhost`): la cámara funciona sin HTTPS.
- **Desde un celular**: abrí la **URL del túnel de Cloudflare** (es HTTPS) → `…/organizador/check-in/<id>`. El organizador ingresa con `orga@eventpass.dev` / `Orga123!`.
- Si la cámara no tiene permiso, la pantalla tiene **ingreso manual del código** siempre visible.

## Estructura

```
backend/    API + worker (Spring Boot, un código / dos perfiles)
frontend/   SPA Angular 18 + nginx (sirve la app y proxea /api)
docs/       Documento técnico del grupo
```

Ver el diseño completo y el plan por fases en [`CLAUDE.md`](./CLAUDE.md).

## Notas para Windows

- Los finales de línea están normalizados a LF vía `.gitattributes` (clave para que `mvnw` y los scripts funcionen dentro de los contenedores Linux).
- **Testcontainers** (tests de integración) necesita Docker Desktop corriendo.
- Los comandos de arriba funcionan igual en **PowerShell** y **Git Bash**; donde difieren, se indica.

## Tests

```bash
# Backend (unit + integración con Testcontainers; requiere Docker)
cd backend && ./mvnw -B verify      # Windows: .\mvnw.cmd -B verify

# Frontend
cd frontend && npm ci && npm test -- --watch=false --browsers=ChromeHeadless
```
