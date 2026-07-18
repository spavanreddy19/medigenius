/**
 * NEW FILE (Features 1 & 10 - Authentication + Auth Pages).
 * Provides `useAuth()` app-wide: { user, loading, login, register, logout }.
 */
import { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { getToken, registerUnauthorizedHandler, setToken } from '../api/client';
import { fetchProfile, loginUser, registerUser } from '../api/endpoints';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);

  const loadProfile = useCallback(async () => {
    if (!getToken()) {
      setUser(null);
      setLoading(false);
      return;
    }
    try {
      const profile = await fetchProfile();
      setUser(profile);
    } catch {
      setToken(null);
      setUser(null);
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadProfile();
    // NEW (Feature 10 - "if token expires, redirect to login"): the axios client calls this
    // whenever any request comes back 401, so an expired/invalid token clears app state
    // immediately instead of waiting for the next manual profile fetch.
    registerUnauthorizedHandler(() => setUser(null));
  }, [loadProfile]);

  const login = useCallback(async (email, password) => {
    const { token } = await loginUser(email, password);
    setToken(token);
    const profile = await fetchProfile();
    setUser(profile);
    return profile;
  }, []);

  const register = useCallback(async (name, email, password) => {
    const { token } = await registerUser(name, email, password);
    setToken(token);
    const profile = await fetchProfile();
    setUser(profile);
    return profile;
  }, []);

  const logout = useCallback(() => {
    setToken(null);
    setUser(null);
  }, []);

  const refreshProfile = useCallback(async () => {
    if (!getToken()) return;
    try {
      const profile = await fetchProfile();
      setUser(profile);
    } catch {
      /* ignore - interceptor already handles 401 */
    }
  }, []);

  return (
    <AuthContext.Provider value={{ user, loading, login, register, logout, refreshProfile }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth() {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error('useAuth must be used within an AuthProvider');
  return ctx;
}
