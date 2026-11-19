import { useState } from 'react';
import { api } from '../api';
import useRefresh from '../hooks/useRefresh';
import ErrorMessage from '../components/ErrorMessage';
import ChangeNotice from '../components/ChangeNotice';
import Details from '../components/Details';

export default function TicketDetails({ id, revision, onEdit, onDelete }) {
  const [ticket, setTicket] = useState(null);
  const [error, setError] = useState(null);
  const notice = useRefresh(
    async (active, accept) => {
      try {
        const result = await api(`/tickets/${id}`);
        if (active()) {
          setError(null);
          if (accept(result)) setTicket(result);
        }
      } catch (e) {
        if (active()) {
          if (e.status === 404 && ticket && !accept(null, 'Этот билет удалён в другом окне. Здесь сохранены его прежние данные.')) {
            setError(null);
            return;
          }
          setError(e);
        }
      }
    },
    [id, revision],
  );
  return (
    <>
      <a href="#/tickets">← Все билеты</a>
      <div className="toolbar">
        <div>
          <h1>{ticket?.name || `Билет #${id}`}</h1>
        </div>
        {ticket && (
          <div className="actions">
            <button onClick={() => onDelete(ticket)}>Удалить</button>
            <button onClick={() => onEdit(ticket)}>Изменить билет</button>
          </div>
        )}
      </div>
      <ErrorMessage error={error} />
      <ChangeNotice message={notice} />
      {ticket ? (
        <div>
          <Details value={ticket} />
        </div>
      ) : (
        !error && <p>Загрузка…</p>
      )}
    </>
  );
}
