import { useState } from 'react';
import useHashRoute from './hooks/useHashRoute';
import useReferences from './hooks/useReferences';
import AppNavigation from './components/AppNavigation';
import ErrorMessage from './components/ErrorMessage';
import ChangeNotice from './components/ChangeNotice';
import EditDialog from './components/EditDialog';
import DeleteDialog from './components/DeleteDialog';
import TicketsPage from './pages/TicketsPage';
import TicketDetails from './pages/TicketDetails';
import ReferencesPage from './pages/ReferencesPage';
import OperationsPage from './pages/OperationsPage';
import ImportsPage from './pages/ImportsPage';

export default function App({ user, onLogout }) {
  const route = useHashRoute();
  const [dialog, setDialog] = useState(null);
  const [revision, setRevision] = useState(0);
  const { references, error, notice } = useReferences(revision);

  function edit(kind, item = null) {
    setDialog({ action: 'edit', kind, item });
  }
  function remove(kind, item) {
    setDialog({ action: 'delete', kind, item });
  }
  function refresh() {
    setRevision((value) => value + 1);
  }
  function saved() {
    setDialog(null);
    refresh();
  }
  function deleted() {
    if (dialog.kind === 'tickets' && route === `/tickets/${dialog.item.id}`) {
      location.hash = '/tickets';
    }
    saved();
  }

  const detailId = route.match(/^\/tickets\/(\d+)$/)?.[1];
  let page;
  if (detailId) {
    page = (
      <TicketDetails
        key={detailId}
        id={detailId}
        revision={revision}
        onEdit={(item) => edit('tickets', item)}
        onDelete={(item) => remove('tickets', item)}
      />
    );
  } else if (route === '/references') {
    page = (
      <ReferencesPage
        references={references}
        onCreate={(kind) => edit(kind)}
        onEdit={edit}
        onDelete={remove}
      />
    );
  } else if (route === '/imports') {
    page = <ImportsPage user={user} onImported={refresh} />;
  } else if (route === '/operations') {
    page = (
      <OperationsPage
        references={references}
        revision={revision}
        onChanged={refresh}
        onEdit={(item) => edit('tickets', item)}
        onDelete={(item) => remove('tickets', item)}
      />
    );
  } else {
    page = (
      <TicketsPage
        revision={revision}
        onCreate={() => edit('tickets')}
        onEdit={(item) => edit('tickets', item)}
        onDelete={(item) => remove('tickets', item)}
      />
    );
  }

  return (
    <div className="app">
      <AppNavigation route={route} user={user} onLogout={onLogout} />
      <main>
        <ErrorMessage error={error} />
        <ChangeNotice message={notice} />
        {page}
      </main>
      {dialog?.action === 'edit' && (
        <EditDialog
          {...dialog}
          references={references}
          onClose={() => setDialog(null)}
          onSaved={saved}
        />
      )}
      {dialog?.action === 'delete' && (
        <DeleteDialog
          {...dialog}
          references={references}
          onClose={() => setDialog(null)}
          onDeleted={deleted}
        />
      )}
    </div>
  );
}
