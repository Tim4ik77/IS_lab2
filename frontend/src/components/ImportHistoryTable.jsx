const statuses = { RUNNING: 'Выполняется', SUCCESS: 'Завершён', FAILED: 'Ошибка' };

export default function ImportHistoryTable({ items }) {
  return (
    <div className="table-scroll"><table className="import-history">
      <thead><tr><th>ID</th><th>Статус</th><th>Пользователь</th><th>Файл</th><th>Добавлено билетов</th><th>Начало</th><th>Ошибка</th></tr></thead>
      <tbody>{items.map(op => <tr key={op.id}>
        <td>{op.id}</td><td>{statuses[op.status] || op.status}</td><td>{op.username}</td><td>{op.filename}</td>
        <td>{op.status === 'SUCCESS' ? op.addedCount : '—'}</td>
        <td>{new Date(op.startedAt).toLocaleString('ru-RU')}</td><td>{op.error || '—'}</td>
      </tr>)}</tbody>
    </table></div>
  );
}
