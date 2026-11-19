import { useState } from 'react';
import { getReferences, referenceKinds } from '../api';
import useRefresh from './useRefresh';

export default function useReferences(revision) {
  const [references, setReferences] = useState(() =>
    Object.fromEntries(referenceKinds.map(kind => [kind, []])),
  );
  const [error, setError] = useState(null);
  const notice = useRefresh(async (isActive, accept) => {
    try {
      const data = await getReferences();
      if (!isActive()) return;
      setError(null);
      if (accept(data, 'Связанные объекты на сервере изменились. На странице сохранены прежние данные.')) {
        setReferences(data);
      }
    } catch (error) {
      if (isActive()) setError(error);
    }
  }, [revision]);
  return { references, error, notice };
}
