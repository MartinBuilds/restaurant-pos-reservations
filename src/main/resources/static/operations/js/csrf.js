let csrfHeaderName = 'X-CSRF-TOKEN';
let csrfParameterName = '_csrf';
let csrfToken = null;
let csrfLoaded = false;

export function getCsrfHeaderName() {
  return csrfHeaderName;
}

export function getCsrfParameterName() {
  return csrfParameterName;
}

export function getCsrfToken() {
  return csrfToken;
}

export function isCsrfLoaded() {
  return csrfLoaded;
}

export function getCsrfHeaders() {
  if (!csrfLoaded || !csrfToken) {
    return {};
  }
  return { [csrfHeaderName]: csrfToken };
}

export async function loadCsrf() {
  const response = await fetch('/api/csrf', {
    method: 'GET',
    credentials: 'same-origin',
    headers: { Accept: 'application/json' }
  });
  if (response.status === 401) {
    const err = new Error('Сесията липсва или е изтекла.');
    err.status = 401;
    throw err;
  }
  if (!response.ok) {
    const err = new Error('Неуспешно зареждане на CSRF токен.');
    err.status = response.status;
    throw err;
  }
  const data = await response.json();
  csrfToken = data.token;
  csrfHeaderName = data.headerName || 'X-CSRF-TOKEN';
  csrfParameterName = data.parameterName || '_csrf';
  csrfLoaded = true;
  return {
    headerName: csrfHeaderName,
    parameterName: csrfParameterName,
    token: csrfToken
  };
}

export function clearCsrf() {
  csrfToken = null;
  csrfLoaded = false;
  csrfHeaderName = 'X-CSRF-TOKEN';
  csrfParameterName = '_csrf';
}