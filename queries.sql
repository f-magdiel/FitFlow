-- PostgreSQL: ejecutar en la base de datos de booking-svc (booking_db por defecto).
-- Iniciar booking-svc primero para que Hibernate cree la tabla fitness_classes.
-- Las fechas se calculan respecto al momento de ejecutar este script.
-- Si un ID ya existe, se conserva su registro y sus reservas sin modificaciones.

BEGIN;

INSERT INTO fitness_classes (
    id, name, description, instructor_name, start_time, end_time,
    capacity, reserved_count, status, created_at, updated_at
) VALUES (
    'b1000000-0000-4000-8000-000000000001',
    'Yoga para principiantes',
    'Sesion de movilidad, respiracion y estiramientos para principiantes.',
    'Andrea Lopez',
    CURRENT_TIMESTAMP + INTERVAL '1 day',
    CURRENT_TIMESTAMP + INTERVAL '1 day 1 hour',
    15, 0, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

INSERT INTO fitness_classes (
    id, name, description, instructor_name, start_time, end_time,
    capacity, reserved_count, status, created_at, updated_at
) VALUES (
    'b1000000-0000-4000-8000-000000000002',
    'HIIT de cuerpo completo',
    'Entrenamiento por intervalos de 30 minutos para fuerza y resistencia.',
    'Carlos Mendez',
    CURRENT_TIMESTAMP + INTERVAL '2 days',
    CURRENT_TIMESTAMP + INTERVAL '2 days 30 minutes',
    10, 0, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

INSERT INTO fitness_classes (
    id, name, description, instructor_name, start_time, end_time,
    capacity, reserved_count, status, created_at, updated_at
) VALUES (
    'b1000000-0000-4000-8000-000000000003',
    'Pilates y postura',
    'Sesion de 45 minutos para fortalecer el abdomen y mejorar la postura.',
    'Mariana Ruiz',
    CURRENT_TIMESTAMP + INTERVAL '3 days',
    CURRENT_TIMESTAMP + INTERVAL '3 days 45 minutes',
    12, 0, 'AVAILABLE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
) ON CONFLICT (id) DO NOTHING;

COMMIT;

-- Verificar las clases con los mismos filtros que GET /api/classes/available.
SELECT id, name, instructor_name, start_time, end_time,
       capacity, reserved_count, capacity - reserved_count AS available_spots
FROM fitness_classes
WHERE status = 'AVAILABLE'
  AND start_time > CURRENT_TIMESTAMP
  AND reserved_count < capacity
ORDER BY start_time ASC;

-- Consultar la API: GET http://localhost:8001/api/classes/available
-- Para reservar: POST http://localhost:8001/api/bookings
-- Reemplazar userId por el UUID de un usuario existente en users-svc:
-- {
--   "userId": "UUID-DE-UN-USUARIO-EXISTENTE",
--   "classId": "b1000000-0000-4000-8000-000000000001"
-- }
-- Crear la reserva por la API actualiza el cupo y llama a notif-svc.

-- CONEXION MANUAL A POSTGRESQL EN DOCKER
-- docker-compose.yml toma BOOKING_DB_NAME, BOOKING_DB_USER y BOOKING_DB_PASSWORD
-- del .env y los configura como POSTGRES_DB, POSTGRES_USER y POSTGRES_PASSWORD
-- dentro del contenedor. Los comandos reutilizan esos valores sin copiar la clave.
--
-- 1. En tu terminal, entrar al contenedor (copiar el comando sin el prefijo --):
-- docker exec -it fitflow-booking-db-1 sh
--
-- 2. Dentro del contenedor, conectarte a PostgreSQL:
-- PGPASSWORD="$POSTGRES_PASSWORD" psql -h 127.0.0.1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"
--
-- 3. Cuando aparezca el prompt de psql, copiar y pegar el bloque de este archivo
-- desde BEGIN; hasta COMMIT; inclusive. Presionar Enter para ejecutarlo.
-- Luego puedes pegar el SELECT de verificacion que aparece despues de COMMIT;.
-- Si ocurre un error dentro de la transaccion, ejecutar ROLLBACK; antes de reintentar.
--
-- 4. Para salir de psql:
-- \q
--
-- 5. Para salir del contenedor:
-- exit

-- USUARIOS: ejecutar esta seccion solamente en la base de datos de users-svc.
-- Este archivo contiene bloques para DOS bases distintas; copiar cada bloque
-- en la conexion correspondiente, sin ejecutar el archivo completo en una sola base.
-- Iniciar users-svc primero para que Hibernate cree la tabla users.
--
-- CONEXION MANUAL AL CONTENEDOR DE USUARIOS
-- 1. En tu terminal, entrar al contenedor:
-- docker exec -it fitflow-users-db-1 sh
--
-- 2. Dentro del contenedor, conectarte a PostgreSQL:
-- PGPASSWORD="$POSTGRES_PASSWORD" psql -h 127.0.0.1 -U "$POSTGRES_USER" -d "$POSTGRES_DB"
-- Docker Compose configura estas variables con USERS_DB_NAME, USERS_DB_USER
-- y USERS_DB_PASSWORD del .env.
--
-- 3. Pegar el siguiente bloque desde BEGIN; hasta COMMIT; y presionar Enter.
-- Ambos usuarios de prueba tienen la contrasena: FitFlowDemo123!
-- password_hash contiene su hash BCrypt, compatible con users-svc.
-- Si ya existe el ID o el email, se conserva el usuario existente sin cambios.

BEGIN;

INSERT INTO users (id, full_name, email, password_hash, created_at)
VALUES (
    'a1000000-0000-4000-8000-000000000001',
    'Ana Garcia',
    'ana.garcia@example.com',
    '$2y$10$UxqLMZCo4M3y1JDZTiIGZuKP/jwK09jzLvL483Uc6.raZf3mKcdPy',
    CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;

INSERT INTO users (id, full_name, email, password_hash, created_at)
VALUES (
    'a1000000-0000-4000-8000-000000000002',
    'Luis Perez',
    'luis.perez@example.com',
    '$2y$10$UxqLMZCo4M3y1JDZTiIGZuKP/jwK09jzLvL483Uc6.raZf3mKcdPy',
    CURRENT_TIMESTAMP
) ON CONFLICT DO NOTHING;

COMMIT;

-- 4. Pegar esta consulta para verificar los usuarios y obtener sus IDs reales:
SELECT id, full_name, email, created_at
FROM users
WHERE email IN ('ana.garcia@example.com', 'luis.perez@example.com')
ORDER BY email;

-- Si ocurre un error dentro de la transaccion, ejecutar ROLLBACK; antes de reintentar.
-- 5. Salir de psql con \q y luego del contenedor con exit.
--
-- Consultar un usuario: GET http://localhost:8003/api/users/a1000000-0000-4000-8000-000000000001
-- Iniciar sesion: POST http://localhost:8003/api/auth/login
-- { "email": "ana.garcia@example.com", "password": "FitFlowDemo123!" }
-- Para crear una reserva, usar el ID devuelto por el SELECT como userId
-- en POST http://localhost:8001/api/bookings junto con uno de los classId anteriores.
