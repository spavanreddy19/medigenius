# Frontend Integration Guide

The existing React (`frontend/`) app is reused **unmodified**. It already talks to the
backend purely over `fetch()` calls against a configurable `API_BASE`, so nothing about
its components, state, or markup needs to change.

## 1. What to check/change

Open `frontend/src/App.jsx` and confirm the base URL constant (near the top of the file)
reads from an environment variable, e.g.:

```js
const API_BASE = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8000/api/v1';
```

If it's hardcoded to the old Python port, just point it at the Java backend's port
(default `8000` for both, so in most local setups **no change is needed at all**).

## 2. Environment variable

Create `frontend/.env` (or pass via docker-compose, already wired in `docker-compose.yml`):

```
VITE_API_BASE_URL=http://localhost:8000/api/v1
```

## 3. Session/cookie behavior

The Java backend accepts the same two session-identification mechanisms as Python:

- an `X-Session-ID` request header (if your frontend already sends one, keep doing so), or
- a browser cookie (`JSESSIONID`), automatically sent by `fetch(..., { credentials: 'include' })`.

Make sure any `fetch()` calls in `App.jsx` include `credentials: 'include'` (check the
existing calls at lines ~411-542) so the session cookie round-trips correctly - this
matches the requirement Starlette's `SessionMiddleware` had in the Python version.

## 4. CORS

The backend's `WebConfig` mirrors the Python app's permissive CORS policy
(`allow_origins=["*"]`, `allow_credentials=True`). If you lock down
`medigenius.cors.allowed-origins` in production, list your exact frontend origin(s)
(comma-separated) instead of `*`.

## 5. Running locally without Docker

```bash
# Terminal 1 - backend
cd medigenius-java
mvn spring-boot:run

# Terminal 2 - frontend (unchanged from the original project)
cd frontend
npm install
npm run dev
```

The frontend will be available at `http://localhost:5173` (Vite default) and will call
the Java backend at `http://localhost:8000/api/v1`.

## 6. Running via Docker Compose (full stack)

```bash
cd medigenius-java
cp .env.example .env   # fill in GROQ_API_KEY, JWT_SECRET, etc.
docker compose up --build
```

This brings up MySQL, ChromaDB, the Spring Boot backend, and the existing frontend's
nginx-served build - all networked together, no frontend code changes required.
