import { useState } from 'react';
import { api, setCredentials } from '../api';

export default function useAuth() {
  const [user, setUser] = useState(null);
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);

  async function login(username, password) {
    setBusy(true);
    setError(null);
    setCredentials(username, password);
    try {
      setUser(await api('/auth/me'));
      return true;
    } catch (error) {
      setCredentials(null);
      setError(error);
      return false;
    } finally {
      setBusy(false);
    }
  }

  function logout() {
    setCredentials(null);
    setUser(null);
    setError(null);
  }

  return { user, error, busy, login, logout };
}
