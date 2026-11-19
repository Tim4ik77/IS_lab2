const links = [
  ['/tickets', 'Билеты'],
  ['/references', 'Связанные объекты'],
  ['/operations', 'Специальные операции'],
  ['/imports', 'Импорт и история'],
];

export default function AppNavigation({ route, user, onLogout }) {
  return (
    <nav className="menu" aria-label="Главное меню">
      {links.map(([path, label]) => (
        <a key={path} href={`#${path}`}
          aria-current={route === path || route.startsWith(`${path}/`) ? 'page' : undefined}>
          {label}
        </a>
      ))}
      <span className="current-user">{user.username} · {user.admin ? 'Администратор' : 'Пользователь'}</span>
      <button onClick={onLogout}>Выйти</button>
    </nav>
  );
}
