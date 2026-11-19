import { api } from './api.js';

export const entityPath = kind => kind === 'tickets' ? '/tickets' : `/references/${kind}`;
const versionHeaders = (kind, item) => kind === 'tickets' && item ? { 'If-Match': item.version } : {};

export function saveEntity(kind, item, body) {
  return api(`${entityPath(kind)}${item ? `/${item.id}` : ''}`, {
    method: item ? 'PUT' : 'POST',
    headers: versionHeaders(kind, item),
    body,
  });
}

export function deleteEntity(kind, item, replacementId) {
  const query = replacementId ? `?${new URLSearchParams({ replacementId })}` : '';
  return api(`${entityPath(kind)}/${item.id}${query}`, {
    method: 'DELETE',
    headers: versionHeaders(kind, item),
  });
}
