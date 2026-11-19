let authorization;
export function setCredentials(username, password) {
  authorization = username ? `Basic ${btoa(String.fromCharCode(...new TextEncoder().encode(`${username}:${password}`)))}` : undefined;
}

export async function api(path, options = {}) {
  const { body, headers, ...requestOptions } = options;
  const isMultipart = body instanceof FormData;
  let response;
  try {
    response = await fetch(`/api${path}`, {
      ...requestOptions,
      headers: {
        ...(isMultipart ? {} : { 'Content-Type': 'application/json' }),
        ...(authorization ? { Authorization: authorization } : {}),
        ...headers,
      },
      body: isMultipart ? body : body === undefined ? undefined : JSON.stringify(body),
    });
  } catch {
    throw new Error('Не удалось подключиться к серверу. Проверьте, что он запущен.');
  }
  if (response.status === 204) return null;
  const data = await response.json().catch(() => null);
  if (!response.ok) {
    const error = new Error(data?.message || data?.error || `Ошибка сервера (${response.status})`);
    error.operation = data?.status === 'FAILED' ? data : null;
    error.status = response.status;
    error.fields = data?.fields || {};
    throw error;
  }
  return data;
}

export const referenceKinds = ['coordinates', 'locations', 'persons', 'events', 'venues'];
export async function getReferences() {
  const results = await Promise.all(referenceKinds.map((kind) => api(`/references/${kind}`)));
  return Object.fromEntries(referenceKinds.map((kind, index) => [kind, results[index]]));
}
