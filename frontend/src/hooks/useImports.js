import { useEffect, useState } from 'react';
import { api } from '../api';

export const IMPORT_PAGE_SIZE = 20;
const MAX_IMPORT_BYTES = 5 * 1024 * 1024;

export default function useImports(onImported) {
  const [file, setFile] = useState(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  const [historyError, setHistoryError] = useState(null);
  const [result, setResult] = useState(null);
  const [page, setPage] = useState(0);
  const [revision, setRevision] = useState(0);
  const [data, setData] = useState({ items: [], total: 0 });
  const [loading, setLoading] = useState(true);
  useEffect(() => {
    let active = true;
    let pending = false;
    setLoading(true);
    async function refresh() {
      if (pending) return;
      pending = true;
      try {
        const next = await api(`/imports?page=${page}&size=${IMPORT_PAGE_SIZE}`);
        if (active) { setData(next); setHistoryError(null); }
      } catch (e) { if (active) setHistoryError(e); }
      finally { pending = false; if (active) setLoading(false); }
    }
    refresh();
    const timer = setInterval(refresh, 5000);
    return () => { active = false; clearInterval(timer); };
  }, [page, revision]);
  async function upload(event) {
    event.preventDefault();
    setError(null); setResult(null);
    if (!file) return;
    if (file.size > MAX_IMPORT_BYTES) { setError(new Error('Максимальный размер файла — 5 МБ')); return; }
    setBusy(true);
    const body = new FormData(); body.append('file', file);
    try {
      const operation = await api('/imports', { method: 'POST', body });
      setResult(`Импорт #${operation.id} завершён. Добавлено билетов: ${operation.addedCount}.`);
      onImported();
    } catch (e) { setError(e); }
    finally { setBusy(false); setPage(0); setRevision(v => v + 1); }
  }
  return { file, setFile, busy, error, historyError, result, page, setPage, data, loading, upload };
}
