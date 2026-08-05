let csrfHeaderName = 'X-CSRF-TOKEN';
let csrfToken = null;
let csrfLoaded = false;

export class ApiClientError extends Error {
  constructor(status, message, body) {
    super(message);
    this.name = 'ApiClientError';
    this.status = status;
    this.body = body;
  }
}

function extractMessage(status, body) {
  if (body && typeof body.message === 'string' && body.message.trim()) {
    return body.message;
  }
  if (status === 400) return 'Невалидни входни данни или липсващи параметри.';
  if (status === 401) return 'Сесията липсва или е изтекла.';
  if (status === 403) return 'Нямате достъп или сесията/CSRF токенът е невалиден.';
  if (status === 404) return 'Ресурсът не е намерен.';
  if (status === 409) return 'Операцията е в конфликт с текущото състояние.';
  if (status >= 500) return 'Възникна неочаквана сървърна грешка.';
  return `Заявката неуспешна (${status}).`;
}

async function parseBody(response) {
  if (response.status === 204) return null;
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return { raw: text };
  }
}

export async function loadCsrf() {
  const response = await fetch('/api/csrf', {
    method: 'GET',
    credentials: 'same-origin',
    headers: { Accept: 'application/json' }
  });
  if (response.status === 401) {
    window.location.assign('/login');
    throw new ApiClientError(401, 'Сесията липсва или е изтекла.');
  }
  if (!response.ok) {
    const body = await parseBody(response);
    throw new ApiClientError(response.status, extractMessage(response.status, body), body);
  }
  const data = await response.json();
  csrfToken = data.token;
  csrfHeaderName = data.headerName || 'X-CSRF-TOKEN';
  csrfLoaded = true;
  return data;
}

export function getCsrfState() {
  return { loaded: csrfLoaded, headerName: csrfHeaderName };
}

export async function apiRequest(method, url, { body, signal, headers } = {}) {
  const upper = method.toUpperCase();
  const mutating = ['POST', 'PUT', 'PATCH', 'DELETE'].includes(upper);
  if (mutating && !csrfLoaded) {
    await loadCsrf();
  }

  const reqHeaders = {
    Accept: 'application/json',
    ...(headers || {})
  };
  if (body !== undefined) {
    reqHeaders['Content-Type'] = 'application/json';
  }
  if (mutating && csrfToken) {
    reqHeaders[csrfHeaderName] = csrfToken;
  }

  let response;
  try {
    response = await fetch(url, {
      method: upper,
      credentials: 'same-origin',
      headers: reqHeaders,
      body: body === undefined ? undefined : JSON.stringify(body),
      signal
    });
  } catch (err) {
    if (err && err.name === 'AbortError') throw err;
    throw new ApiClientError(0, 'Мрежова грешка при заявката.');
  }

  if (response.status === 401) {
    window.location.assign('/login');
    throw new ApiClientError(401, 'Сесията липсва или е изтекла.');
  }

  const parsed = await parseBody(response);
  if (!response.ok) {
    throw new ApiClientError(response.status, extractMessage(response.status, parsed), parsed);
  }
  return parsed;
}

export const api = {
  get: (url, opts) => apiRequest('GET', url, opts),
  post: (url, body, opts) => apiRequest('POST', url, { ...opts, body }),
  put: (url, body, opts) => apiRequest('PUT', url, { ...opts, body }),
  patch: (url, body, opts) => apiRequest('PATCH', url, { ...opts, body }),
  delete: (url, opts) => apiRequest('DELETE', url, opts)
};

export function queryString(params) {
  const search = new URLSearchParams();
  Object.entries(params || {}).forEach(([key, value]) => {
    if (value === undefined || value === null || value === '') return;
    search.set(key, String(value));
  });
  const q = search.toString();
  return q ? `?${q}` : '';
}