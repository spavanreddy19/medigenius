/**
 * NEW FILE (Feature 15 - Frontend API layer).
 *
 * Central axios instance used by every authenticated call in the app (auth, profile,
 * conversations, PDF upload). The existing anonymous chat endpoints (/api/v1/*) are also
 * routed through this instance so that IF a user is logged in, their token rides along
 * automatically and the backend attaches ownership - but nothing about those endpoints'
 * request/response shape changes, and they keep working with no token present at all.
 *
 * Token storage: localStorage under `medigenius_token` (simple + survives refresh; this is
 * a client-only demo app, not multi-tenant enterprise auth, so we didn't add refresh-token
 * rotation - see the 401 interceptor below for "automatic token refresh handling", which in
 * this JWT-only setup means: detect the expired/invalid token, clear it, and send the user
 * back to /login rather than silently failing).
 */
import axios from 'axios';

const TOKEN_KEY = 'medigenius_token';

export function getToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
  if (token) localStorage.setItem(TOKEN_KEY, token);
  else localStorage.removeItem(TOKEN_KEY);
}

const apiClient = axios.create({
  baseURL: '/api',
  headers: { 'Content-Type': 'application/json' },
});

// ── Request interceptor: attach JWT if we have one ───────────────────────────
apiClient.interceptors.request.use((config) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// ── Response interceptor: 401 -> clear token + redirect to /login ───────────
// ("automatic token refresh handling" - this backend issues long-lived JWTs (no refresh
// endpoint), so the correct automatic behavior on expiry is a clean logout + redirect
// rather than a silent retry loop against an endpoint that doesn't exist.)
let onUnauthorized = null;
export function registerUnauthorizedHandler(handler) {
  onUnauthorized = handler;
}

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      setToken(null);
      if (onUnauthorized) onUnauthorized();
    }
    return Promise.reject(error);
  }
);

export default apiClient;
