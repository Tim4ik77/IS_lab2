import { useEffect, useRef, useState } from 'react';

export default function useRefresh(load, dependencies = []) {
  const [notice, setNotice] = useState(null);
  const latest = useRef(load);
  latest.current = load;
  useEffect(() => {
    let active = true;
    let loading = false;
    let loaded = false;
    let snapshot;
    setNotice(null);

    // Apply the first response only. Later polls just report changes.
    function accept(data, message = 'Данные на сервере изменились. Текущая страница не обновлена.') {
      if (!active) return false;
      const next = JSON.stringify(data);
      if (!loaded) {
        loaded = true;
        snapshot = next;
        return true;
      }
      if (next !== snapshot) setNotice(message);
      return false;
    }

    async function refresh() {
      if (!active || loading) return;
      loading = true;
      try {
        await latest.current(() => active, accept);
      } finally {
        loading = false;
      }
    }

    refresh();
    const timer = setInterval(refresh, 5000);
    return () => {
      active = false;
      clearInterval(timer);
    };
  }, dependencies);
  return notice;
}
