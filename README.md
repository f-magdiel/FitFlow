# FitFlow

Plataforma de reservas de clases fitness construida con una arquitectura de microservicios.
Proyecto del curso de Postgrado en Diseño y Desarrollo de Software, FISICC — Universidad Galileo.

> Estado actual: avance de **Task 1** (microservicios + Docker). `users-svc`, `booking-svc` y
> `notif-svc` ya tienen implementacion base en `develop`; Consul y MCP quedan para Task 2.

### Enlace del video - Fase 1

[Ver video Fase 1](https://drive.google.com/file/d/1gtPRzjmDeGrf3pPAyUHc2nPjdujXBpGT/view?usp=sharing)

---

## Arquitectura

```
                          ┌───────────────────┐
                 :8003    │     users-svc      │      ┌───────────┐
        ┌───────────────► │  (Java/SpringBoot) ├─────►│ users-db  │
        │                 │  registro / login  │      │ Postgres  │
        │                 │  / perfil          │      └───────────┘
        │                 └───────────────────┘
        │
        │                 ┌───────────────────┐
        │       :8001     │    booking-svc     │      ┌───────────┐
        ├───────────────► │ Java/SpringBoot    ├─────►│ booking-db│
        │                 │  reservas de clases│      │ Postgres  │
        │                 └───────────────────┘      └───────────┘
        │
        │                 ┌───────────────────┐
        │       :8002     │     notif-svc      │      ┌───────────┐
   Cliente/─────────────► │ Java/SpringBoot    ├─────►│ notif-db  │
   Postman                │   notificaciones   │      │ Postgres  │
                          └───────────────────┘      └───────────┘
```

Cada microservicio es dueño exclusivo de sus datos (**database per service**): ningún servicio
consulta directamente la base de datos de otro. Si `booking-svc` necesita validar un usuario, debe
llamar a la API HTTP de `users-svc`, nunca a su base de datos.

| Servicio | Puerto | Responsable | Estado |
| --- | --- | --- | --- |
| `users-svc` | 8003 | Magdiel | Base Task 1 implementada |
| `booking-svc` | 8001 | Sergio Garcia | Base Task 1 implementada |
| `notif-svc` | 8002 | _(asignar)_ | Base Task 1 implementada |
| `consul` | 8500 | — | ⏳ Task 2 |
| `fitflow-mcp` | 8000 | — | ⏳ Task 2 |

## Cómo correr el proyecto

Requisitos: [Docker](https://docs.docker.com/get-docker/) y Docker Compose (viene incluido con
Docker Desktop).

```bash
git clone <repo>
cd FitFlow
cp .env.example .env   # completar los valores, especialmente JWT_SECRET
docker compose up --build
```

En `develop`, Docker Compose levanta los tres microservicios con sus respectivas bases de datos
PostgreSQL. Cada servicio conserva su propia carpeta, Dockerfile y README.

## Gestión de secretos

- Ningún password ni secreto vive en el código fuente ni se sube al repositorio.
- Todos los valores sensibles (passwords de BD, `JWT_SECRET`) se definen en `.env`, que está en
  `.gitignore`. `.env.example` documenta qué variables existen, sin valores reales.
- Cada servicio se conecta con su propio usuario de base de datos (principio de menor privilegio):
  `users-svc` no puede leer las bases de `booking-svc` ni `notif-svc`, y viceversa.

### Rotar credenciales (sin downtime)

Para rotar, por ejemplo, la contraseña de `users-db`:

1. Crear un nuevo usuario/password en Postgres sin eliminar el actual:
   `ALTER USER users_user WITH PASSWORD 'nuevo-password';` (o crear un usuario nuevo con los mismos
   permisos si se prefiere no reutilizar el nombre).
2. Actualizar `USERS_DB_PASSWORD` en `.env` con el nuevo valor.
3. Reiniciar solo `users-svc` (no la base de datos) para que tome la nueva credencial:
   `docker compose up -d --no-deps users-svc`.
4. Confirmar con `curl http://localhost:8003/readyz` que la nueva conexión funciona.
5. Si se creó un usuario nuevo en vez de rotar el password del existente, eliminar el usuario viejo
   una vez confirmado que nada más lo usa.

Para rotar `JWT_SECRET`: cambiarlo en `.env` invalida todos los tokens emitidos previamente (los
usuarios deben volver a hacer login). Por eso conviene avisar/coordinar antes de rotarlo en un
entorno con usuarios activos.

## Estructura del repo

```
FitFlow/
├── docker-compose.yml       # levanta todo el sistema
├── .env.example             # variables de entorno documentadas (sin valores reales)
├── users-svc/                # registro, login y perfil
├── booking-svc/              # clases y reservas
├── notif-svc/                 # notificaciones
└── Fitflow.md                # enunciado completo del proyecto
```

## Próximos pasos (fuera de alcance de esta entrega)

- Task 2: auto-registro en Consul + servidor MCP.
- Task 3: resiliencia (timeout/retries/circuit breaker) + logs estructurados con `x-correlation-id`.
- Task 4: JWT validado en `booking-svc`, checklist de seguridad, video demo.
- Task 5: arquitectura Agent-to-Agent (A2A).
