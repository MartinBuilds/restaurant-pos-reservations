import { icon, initialsFrom } from './icons.js';
import { applyDomI18n, onLanguageChange, t } from './i18n/i18n.js?v=pr17-4';
import {
  getLanguage,
  getSidebarCollapsed,
  getTheme,
  initUiPreferences,
  setLanguage,
  setSidebarCollapsed,
  setTheme,
  SIDEBAR_CHANGE,
  THEME_CHANGE
} from './ui-preferences.js';

async function fetchAccount(apiGet) {
  if (typeof apiGet !== 'function') return null;
  try {
    return await apiGet('/api/account/me');
  } catch {
    return null;
  }
}

function isMenuOpen(root) {
  return root?.dataset?.accountMenu === 'open';
}

function closeMenu(root) {
  if (!root) return;
  const menu = root.querySelector('#account-menu');
  const trigger = root.querySelector('#account-trigger');
  root.dataset.accountMenu = 'closed';
  if (menu) {
    menu.hidden = true;
    menu.classList.remove('is-open');
    menu.style.display = 'none';
  }
  if (trigger) trigger.setAttribute('aria-expanded', 'false');
}

function openMenu(root) {
  if (!root) return;
  const menu = root.querySelector('#account-menu');
  const trigger = root.querySelector('#account-trigger');
  root.dataset.accountMenu = 'open';
  if (menu) {
    menu.hidden = false;
    menu.classList.add('is-open');
    menu.style.display = 'grid';
  }
  if (trigger) trigger.setAttribute('aria-expanded', 'true');
}

function toggleMenu(root) {
  if (isMenuOpen(root)) closeMenu(root);
  else openMenu(root);
}

function renderAccountBody(accountMount, account) {
  const name = account?.name || t('account.unavailable');
  const email = account?.email || '';
  const initials = initialsFrom(account?.name, account?.email);
  accountMount.replaceChildren();

  const trigger = document.createElement('button');
  trigger.type = 'button';
  trigger.id = 'account-trigger';
  trigger.className = 'account-trigger';
  trigger.setAttribute('aria-haspopup', 'menu');
  trigger.setAttribute('aria-expanded', 'false');
  trigger.setAttribute('aria-controls', 'account-menu');
  trigger.setAttribute('aria-label', t('account.menu'));

  const avatar = document.createElement('span');
  avatar.className = 'account-avatar';
  avatar.setAttribute('aria-hidden', 'true');
  avatar.textContent = initials;

  const meta = document.createElement('span');
  meta.className = 'account-meta';
  const nameEl = document.createElement('span');
  nameEl.className = 'account-name';
  nameEl.textContent = name;
  const emailEl = document.createElement('span');
  emailEl.className = 'account-email';
  emailEl.textContent = email;
  meta.append(nameEl, emailEl);
  trigger.append(avatar, meta);

  const menu = document.createElement('div');
  menu.id = 'account-menu';
  menu.className = 'account-menu';
  menu.setAttribute('role', 'menu');

  const header = document.createElement('div');
  header.className = 'account-menu-header';
  const hName = document.createElement('div');
  hName.className = 'account-name';
  hName.textContent = name;
  const hEmail = document.createElement('div');
  hEmail.className = 'account-email';
  hEmail.textContent = email;
  header.append(hName, hEmail);

  const themeSection = section(t('account.theme'), [
    optionButton('theme', 'system', t('theme.system'), getTheme() === 'system'),
    optionButton('theme', 'light', t('theme.light'), getTheme() === 'light'),
    optionButton('theme', 'dark', t('theme.dark'), getTheme() === 'dark')
  ]);

  const langSection = section(t('account.language'), [
    optionButton('language', 'bg', `🇧🇬 ${t('lang.bg')}`, getLanguage() === 'bg'),
    optionButton('language', 'en', `🇬🇧 ${t('lang.en')}`, getLanguage() === 'en')
  ]);

  const logoutBtn = document.createElement('button');
  logoutBtn.type = 'button';
  logoutBtn.className = 'account-menu-item account-logout';
  logoutBtn.setAttribute('role', 'menuitem');
  logoutBtn.id = 'account-logout-btn';
  logoutBtn.append(icon('logout'), document.createTextNode(t('account.logout')));

  menu.append(header, themeSection, langSection, logoutBtn);
  accountMount.append(trigger, menu);
  closeMenu(accountMount);

  trigger.addEventListener('click', (event) => {
    event.preventDefault();
    event.stopPropagation();
    toggleMenu(accountMount);
  });

  themeSection.querySelectorAll('button[data-theme]').forEach((btn) => {
    btn.addEventListener('click', (event) => {
      event.preventDefault();
      event.stopPropagation();
      closeMenu(accountMount);
      setTheme(btn.dataset.theme);
    });
  });
  langSection.querySelectorAll('button[data-language]').forEach((btn) => {
    btn.addEventListener('click', (event) => {
      event.preventDefault();
      event.stopPropagation();
      closeMenu(accountMount);
      setLanguage(btn.dataset.language);
    });
  });

  return { logoutBtn, trigger, menu };
}

function section(title, buttons) {
  const wrap = document.createElement('div');
  wrap.className = 'account-section';
  const label = document.createElement('div');
  label.className = 'account-section-label';
  label.textContent = title;
  wrap.append(label, ...buttons);
  return wrap;
}

function optionButton(kind, value, label, selected) {
  const btn = document.createElement('button');
  btn.type = 'button';
  btn.className = `account-menu-item${selected ? ' is-selected' : ''}`;
  btn.setAttribute('role', 'menuitemradio');
  btn.setAttribute('aria-checked', selected ? 'true' : 'false');
  if (kind === 'theme') btn.dataset.theme = value;
  if (kind === 'language') btn.dataset.language = value;
  btn.textContent = label;
  return btn;
}

function applySidebarState(sidebar, collapsed) {
  if (!sidebar) return;
  const desktop = window.matchMedia('(min-width: 769px)').matches;
  if (desktop && collapsed) sidebar.classList.add('is-collapsed');
  else sidebar.classList.remove('is-collapsed');

  const toggle = document.getElementById('sidebar-collapse-btn');
  if (toggle) {
    const key = (desktop && collapsed) ? 'sidebar.expand' : 'sidebar.collapse';
    toggle.setAttribute('aria-label', t(key));
    toggle.setAttribute('title', t(key));
    toggle.replaceChildren(icon(desktop && collapsed ? 'expand' : 'collapse'));
  }

  sidebar.querySelectorAll('.nav-link[data-i18n]').forEach((link) => {
    const key = link.getAttribute('data-i18n');
    if (key) {
      link.setAttribute('title', t(key));
      link.setAttribute('aria-label', t(key));
    }
  });
}

function bindLogout(logoutBtn, accountMount, onLogout) {
  logoutBtn.addEventListener('click', async (event) => {
    event.preventDefault();
    event.stopPropagation();
    closeMenu(accountMount);
    if (typeof onLogout === 'function') await onLogout();
  });
}

export async function mountShellChrome(options = {}) {
  const {
    apiGet,
    onLogout,
    onLanguageApplied,
    collapsibleSidebar = false
  } = options;

  initUiPreferences();
  applyDomI18n(document);

  const accountMount = document.getElementById('account-mount');
  const sidebar = document.getElementById('sidebar');
  let account = null;

  if (accountMount) {
    account = await fetchAccount(apiGet);
    const { logoutBtn } = renderAccountBody(accountMount, account);
    bindLogout(logoutBtn, accountMount, onLogout);

    document.addEventListener('click', (event) => {
      if (!isMenuOpen(accountMount)) return;
      if (accountMount.contains(event.target)) return;
      closeMenu(accountMount);
    });
    document.addEventListener('keydown', (event) => {
      if (event.key === 'Escape') closeMenu(accountMount);
    });
    sidebar?.querySelector('#main-nav')?.addEventListener('click', () => {
      closeMenu(accountMount);
    });
  }

  if (collapsibleSidebar && sidebar) {
    let collapsed = getSidebarCollapsed();
    applySidebarState(sidebar, collapsed);

    const collapseBtn = document.getElementById('sidebar-collapse-btn');
    if (collapseBtn) {
      collapseBtn.addEventListener('click', () => {
        if (!window.matchMedia('(min-width: 769px)').matches) return;
        closeMenu(accountMount);
        collapsed = !sidebar.classList.contains('is-collapsed');
        setSidebarCollapsed(collapsed);
        applySidebarState(sidebar, collapsed);
      });
    }

    window.addEventListener(SIDEBAR_CHANGE, () => {
      applySidebarState(sidebar, getSidebarCollapsed());
    });
    window.matchMedia('(min-width: 769px)').addEventListener('change', () => {
      applySidebarState(sidebar, getSidebarCollapsed());
    });
  }

  const refreshChrome = () => {
    applyDomI18n(document);
    if (accountMount) {
      const { logoutBtn } = renderAccountBody(accountMount, account);
      bindLogout(logoutBtn, accountMount, onLogout);
    }
    if (collapsibleSidebar && sidebar) {
      applySidebarState(sidebar, getSidebarCollapsed());
    }
    if (typeof onLanguageApplied === 'function') onLanguageApplied(getLanguage());
  };

  onLanguageChange(refreshChrome);
  window.addEventListener(THEME_CHANGE, () => {
    if (!accountMount) return;
    const { logoutBtn } = renderAccountBody(accountMount, account);
    bindLogout(logoutBtn, accountMount, onLogout);
  });

  return { account, refreshChrome };
}

export function decorateNavIcons(map) {
  Object.entries(map).forEach(([route, iconName]) => {
    const link = document.querySelector(`.nav-link[data-route="${route}"]`);
    if (!link || link.querySelector('.icon')) return;
    const label = document.createElement('span');
    label.className = 'nav-label';
    label.textContent = link.textContent.trim();
    link.textContent = '';
    link.append(icon(iconName), label);
  });
}
