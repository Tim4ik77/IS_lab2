import { useState } from 'react';
import { saveEntity } from '../entityApi';
import { schemas, titles, initialValues, requestValues } from '../schema';
import Modal from './Modal';
import Field from './Field';
import ErrorMessage from './ErrorMessage';

export default function EditDialog({ kind, item, references, onClose, onSaved }) {
  const [values, setValues] = useState(() => initialValues(kind, item));
  const [error, setError] = useState(null);
  const [busy, setBusy] = useState(false);
  async function save(e) {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await saveEntity(kind, item, requestValues(kind, values));
      onSaved();
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  }
  return (
    <Modal
      title={`${item ? `Изменить #${item.id}` : 'Создать объект'} · ${titles[kind]}`}
      onClose={() => !busy && onClose()}
    >
      <form onSubmit={save}>
        <ErrorMessage error={error} />
        <div className="form-grid">
          {schemas[kind].map((field) => (
            <Field
              key={field.key}
              field={field}
              value={values[field.key]}
              references={references}
              error={error?.fields?.[field.key]}
              onChange={(value) => setValues((v) => ({ ...v, [field.key]: value }))}
            />
          ))}
        </div>
        <div className="actions">
          <button type="button" disabled={busy} onClick={onClose}>
            Отмена
          </button>
          <button disabled={busy}>{busy ? 'Сохранение…' : 'Сохранить'}</button>
        </div>
      </form>
    </Modal>
  );
}
