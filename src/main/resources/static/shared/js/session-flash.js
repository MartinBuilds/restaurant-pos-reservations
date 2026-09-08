/**
 * One-shot welcome toast after form login (not on refresh).
 * Prefers ?signedIn=1 from the auth redirect; falls back to sessionStorage.
 */
const KEY = 'restaurant.ui.justSignedIn';

export function markJustSignedIn() {
  try {
    sessionStorage.setItem(KEY, '1');
  } catch {
    // ignore storage failures
  }
}

export function clearJustSignedIn() {
  try {
    sessionStorage.removeItem(KEY);
  } catch {
    // ignore
  }
}

export function consumeJustSignedIn() {
  try {
    if (sessionStorage.getItem(KEY) === '1') {
      sessionStorage.removeItem(KEY);
      return true;
    }
  } catch {
    // ignore
  }
  return false;
}

/** True once after login redirect (?signedIn=1) or login form flash flag. */
export function consumeSignedInWelcome() {
  try {
    const url = new URL(window.location.href);
    if (url.searchParams.get('signedIn') === '1') {
      url.searchParams.delete('signedIn');
      const qs = url.searchParams.toString();
      const next = url.pathname + (qs ? `?${qs}` : '') + url.hash;
      window.history.replaceState(null, '', next);
      clearJustSignedIn();
      return true;
    }
  } catch {
    // ignore URL parse issues
  }
  return consumeJustSignedIn();
}
