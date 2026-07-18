/**
 * NEW FILE (Feature 15/16). Thin, named wrappers around apiClient so components never
 * hand-roll a URL string - keeps every endpoint path in exactly one place.
 */
import apiClient from './client';

// ── Auth (Feature 1) ──────────────────────────────────────────────────────
export const registerUser = (name, email, password) =>
  apiClient.post('/auth/register', { name, email, password }).then((r) => r.data);

export const loginUser = (email, password) =>
  apiClient.post('/auth/login', { email, password }).then((r) => r.data);

// ── Profile (Feature 12) ──────────────────────────────────────────────────
export const fetchProfile = () => apiClient.get('/users/me').then((r) => r.data);

// ── Conversations (Feature 4/11) ──────────────────────────────────────────
export const fetchMyConversations = () => apiClient.get('/conversations').then((r) => r.data);

// ── PDF upload (Feature 6) ────────────────────────────────────────────────
export const uploadPdf = (file, onProgress) => {
  const formData = new FormData();
  formData.append('pdf', file);
  return apiClient
    .post('/pdf/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: onProgress,
    })
    .then((r) => r.data);
};

// ── Chat (existing /api/v1/* endpoints, now token-aware via apiClient) ────
export const sendChatMessage = (message) => apiClient.post('/v1/chat', { message }).then((r) => r.data);
export const clearChat = () => apiClient.post('/v1/clear').then((r) => r.data);
export const createNewChat = () => apiClient.post('/v1/new-chat').then((r) => r.data);
export const fetchHistory = () => apiClient.get('/v1/history').then((r) => r.data);
export const fetchSessions = () => apiClient.get('/v1/sessions').then((r) => r.data);
export const fetchSession = (sessionId) => apiClient.get(`/v1/session/${sessionId}`).then((r) => r.data);
export const deleteSessionById = (sessionId) => apiClient.delete(`/v1/session/${sessionId}`).then((r) => r.data);
