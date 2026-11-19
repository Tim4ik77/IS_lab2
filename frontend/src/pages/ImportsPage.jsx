import useImports, { IMPORT_PAGE_SIZE } from '../hooks/useImports';
import ImportHistoryTable from '../components/ImportHistoryTable';
import ErrorMessage from '../components/ErrorMessage';
import Pagination from '../components/Pagination';

export default function ImportsPage({ user, onImported }) {
  const { file, setFile, busy, error, historyError, result, page, setPage, data, loading, upload } = useImports(onImported);
  return <>
    <h1>Импорт билетов</h1>
    <p>JSON-массив из 1–1000 билетов, до 5 МБ. Поля вложенных объектов указываются в каждой записи.
      При любой ошибке весь импорт отменяется.</p>
    <p><a href="/import-example.json" download>Скачать пример JSON</a></p>
    <form className="toolbar" onSubmit={upload}>
      <label>Файл JSON<input type="file" accept=".json,application/json" required disabled={busy}
        onChange={e => setFile(e.target.files[0] || null)} /></label>
      <button disabled={busy || !file}>{busy ? 'Импортируется…' : 'Импортировать файл'}</button>
    </form>
    <ErrorMessage error={error} />
    {result && <p role="status">{result}</p>}
    <h2>История импорта</h2>
    <p>{user.admin ? 'Операции всех пользователей.' : 'Ваши операции импорта.'} Обновление каждые 5 секунд.</p>
    <ErrorMessage error={historyError} />
    <ImportHistoryTable items={data.items} />
    {loading && <p role="status">Загрузка истории…</p>}
    {!loading && !historyError && !data.items.length && <p>Операций импорта пока нет.</p>}
    <Pagination page={page} pageSize={IMPORT_PAGE_SIZE} total={data.total} onPageChange={setPage} emptyLabel="0 операций" />
  </>;
}
