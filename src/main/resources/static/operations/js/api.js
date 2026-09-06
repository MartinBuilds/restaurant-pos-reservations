import { clearCsrf, getCsrfHeaders, getCsrfParameterName, getCsrfToken, isCsrfLoaded, loadCsrf } from './csrf.js';
import { t } from '/shared/js/i18n/i18n.js?v=pr19-1';

export class ApiClientError extends Error {
  constructor(status, message, body) {
    super(message);
    this.name = 'ApiClientError';
    this.status = status;
    this.body = body;
  }
}

let onUnauthorized = null;

export function setUnauthorizedHandler(handler) {
  onUnauthorized = typeof handler === 'function' ? handler : null;
}

function extractMessage(status, body) {
  if (body && typeof body.message === 'string' && body.message.trim()) {
    return body.message;
  }
  if (status === 400) return t('error.http400');
  if (status === 401) return t('error.session');
  if (status === 403) return t('error.http403');
  if (status === 404) return t('error.http404');
  if (status === 409) return t('error.http409');
  if (status >= 500) return t('error.http5xx');
  return t('error.requestFailed', { status });
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

function handleUnauthorized() {
  clearCsrf();
  if (onUnauthorized) {
    onUnauthorized();
  } else {
    window.location.assign('/login');
  }
}

export async function ensureCsrf() {
  if (!isCsrfLoaded()) {
    await loadCsrf();
  }
}

export async function apiRequest(method, url, { body, signal, headers } = {}) {
  const upper = method.toUpperCase();
  const mutating = ['POST', 'PUT', 'PATCH', 'DELETE'].includes(upper);
  if (mutating) {
    await ensureCsrf();
  }

  const reqHeaders = {
    Accept: 'application/json',
    ...(headers || {}),
    ...(mutating ? getCsrfHeaders() : {})
  };
  if (body !== undefined) {
    reqHeaders['Content-Type'] = 'application/json';
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
    throw new ApiClientError(0, t('error.network'));
  }

  if (response.status === 401) {
    handleUnauthorized();
    throw new ApiClientError(401, t('error.session'));
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

export async function logout() {
  await ensureCsrf();
  const headers = {
    'Content-Type': 'application/x-www-form-urlencoded',
    ...getCsrfHeaders()
  };
  const body = new URLSearchParams();
  body.set(getCsrfParameterName(), getCsrfToken());
  await fetch('/logout', {
    method: 'POST',
    credentials: 'same-origin',
    headers,
    body
  });
  clearCsrf();
}