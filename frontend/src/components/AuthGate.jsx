import { useState } from 'react';
import useAuth from '../hooks/useAuth';
import App from '../App';
import ErrorMessage from './ErrorMessage';

export default function AuthGate() {
  const { user, error, busy, login, logout } = useAuth();
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  async function submit(event) {
    event.preventDefault();
    if (await login(username, password)) setPassword('');
  }
  if (user) return <App user={user} onLogout={logout} />;
  return (
    <main className="app login-page">
      <div className="login-panel">
      <h1>Вход в систему билетов</h1>
      <form className="login-form" onSubmit={submit}>
        <label>Логин<input autoComplete="username" required value={username} onChange={e => setUsername(e.target.value)} /></label>
        <label>Пароль<input type="password" autoComplete="current-password" required value={password} onChange={e => setPassword(e.target.value)} /></label>
        <ErrorMessage error={error} />
        <button disabled={busy}>{busy ? 'Вход…' : 'Войти'}</button>
      </form>
      </div>
    </main>
  );
}
