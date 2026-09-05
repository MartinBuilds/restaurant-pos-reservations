export const THEME_KEY = 'restaurant.ui.theme';
export const LANGUAGE_KEY = 'restaurant.ui.language';
export const SIDEBAR_KEY = 'restaurant.ui.sidebar.collapsed';

export const THEME_CHANGE = 'restaurant:theme-change';
export const LANGUAGE_CHANGE = 'restaurant:language-change';
export const SIDEBAR_CHANGE = 'restaurant:sidebar-change';

const THEMES = new Set(['system', 'light', 'dark']);
const LANGUAGES = new Set(['bg', 'en']);

let systemListener = null;

function dispatch(name, detail) {
  window.dispatchEvent(new CustomEvent(name, { detail }));
}

export function getTheme() {
  try {
    const value = localStorage.getItem(THEME_KEY);
    return THEMES.has(value) ? value : 'system';
  } catch {
    return 'system';
  }
}

export function getLanguage() {
  try {
    const value = localStorage.getItem(LANGUAGE_KEY);
    return LANGUAGES.has(value) ? value : 'bg';
  } catch {
    return 'bg';
  }
}

export function getSidebarCollapsed() {
  try {
    return localStorage.getItem(SIDEBAR_KEY) === 'true';
  } catch {
    return false;
  }
}

export function resolveTheme(pref = getTheme()) {
  if (pref === 'light' || pref === 'dark') return pref;
  try {
    return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
  } catch {
    return 'light';
  }
}

function applyThemeDom(pref = getTheme()) {
  const resolved = resolveTheme(pref);
  document.documentElement.setAttribute('data-theme', resolved);
  document.documentElement.setAttribute('data-theme-pref', pref);
}

function ensureSystemListener(pref) {
  if (systemListener) {
    try {
      window.matchMedia('(prefers-color-scheme: dark)').removeEventListener('change', systemListener);
    } catch {
      // older browsers
    }
    systemListener = null;
  }
  if (pref !== 'system') return;
  systemListener = () => {
    applyThemeDom('system');
    dispatch(THEME_CHANGE, { preference: 'system', resolved: resolveTheme('system') });
  };
  try {
    window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', systemListener);
  } catch {
    // ignore
  }
}

export function setTheme(pref) {
  const next = THEMES.has(pref) ? pref : 'system';
  try {
    localStorage.setItem(THEME_KEY, next);
  } catch {
    // ignore quota / private mode
  }
  applyThemeDom(next);
  ensureSystemListener(next);
  dispatch(THEME_CHANGE, { preference: next, resolved: resolveTheme(next) });
  return next;
}

export function setLanguage(lang) {
  const next = LANGUAGES.has(lang) ? lang : 'bg';
  try {
    localStorage.setItem(LANGUAGE_KEY, next);
  } catch {
    // ignore
  }
  document.documentElement.lang = next;
  dispatch(LANGUAGE_CHANGE, { language: next });
  return next;
}

export function setSidebarCollapsed(collapsed) {
  const next = !!collapsed;
  try {
    localStorage.setItem(SIDEBAR_KEY, next ? 'true' : 'false');
  } catch {
    // ignore
  }
  dispatch(SIDEBAR_CHANGE, { collapsed: next });
  return next;
}

export function initUiPreferences() {
  applyThemeDom(getTheme());
  ensureSystemListener(getTheme());
  document.documentElement.lang = getLanguage();

  window.addEventListener('storage', (event) => {
    if (event.key === THEME_KEY) {
      applyThemeDom(getTheme());
      ensureSystemListener(getTheme());
      dispatch(THEME_CHANGE, { preference: getTheme(), resolved: resolveTheme(), crossTab: true });
    }
    if (event.key === LANGUAGE_KEY) {
      document.documentElement.lang = getLanguage();
      dispatch(LANGUAGE_CHANGE, { language: getLanguage(), crossTab: true });
    }
    if (event.key === SIDEBAR_KEY) {
      dispatch(SIDEBAR_CHANGE, { collapsed: getSidebarCollapsed(), crossTab: true });
    }
  });
}
