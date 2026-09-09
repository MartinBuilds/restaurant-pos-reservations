import { loadCsrf, api } from './api.js';
import { registerRoute, startRouter, setActiveNav } from './router.js';
import { setBanner, toast, handleError, setPageMeta } from './ui.js';
import { renderDashboard } from './views/dashboard.js';
import { renderUsers } from './views/users.js';
import { renderMenu } from './views/menu.js?v=fix-avail-2';
import { renderInventory } from './views/inventory.js';
import { renderTables } from './views/tables.js';
import { renderReservations } from './views/reservations.js';
import { renderPayments } from './views/payments.js';
import { renderReports } from './views/reports.js';
import { decorateNavIcons, mountShellChrome } from '/shared/js/account-shell.js?v=fix-account-sheet-1';
import { polishBottomAccount, wireBottomNav } from '/shared/js/bottom-nav.js?v=fix-account-sheet-1';
import { consumeSignedInWelcome } from '/shared/js/session-flash.js?v=fix-toasts-3';
import { clearPageRefresh, wirePageRefresh } from '/shared/js/page-refresh.js?v=fix-refresh-2';
import { wireDatetimePickers } from '/shared/js/datetime-picker.js?v=fix-datetime-2';
import { t } from '/shared/js/i18n/i18n.js?v=fix-avail-2';

registerRoute('dashboard', renderDashboard);
registerRoute('users', renderUsers);
registerRoute('menu', renderMenu);
registerRoute('inventory', renderInventory);
registerRoute('tables', renderTables);
registerRoute('reservations', renderReservations);
registerRoute('payments', renderPayments);
registerRoute('reports', renderReports);

let shellAccount = null;

async function logout() {
  try {
    const csrf = await api.get('/api/csrf');
    const body = new URLSearchParams();
    if (csrf && csrf.parameterName && csrf.token) {
      body.set(csrf.parameterName, csrf.token);
    }
    await fetch('/logout', {
      method: 'POST',
      credentials: 'same-origin',
      headers: {
        'Content-Type': 'application/x-www-form-urlencoded',
        [csrf.headerName || 'X-CSRF-TOKEN']: csrf.token
      },
      body
    });
  } catch (err) {
    // Always leave the UI after logout attempt.
  }
  window.location.assign('/login');
}

function wireShell() {
  wireBottomNav();
}

async function boot() {
  decorateNavIcons({
    dashboard: 'dashboard',
    users: 'users',
    menu: 'menu',
    inventory: 'inventory',
    tables: 'tables',
    reservations: 'reservations',
    payments: 'payments',
    reports: 'reports'
  });
  wireShell();
  wirePageRefresh();
  wireDatetimePickers();

  let csrfOk = false;
  try {
    await loadCsrf();
    csrfOk = true;
  } catch (err) {
    handleError(err, t('session.csrfError'));
    setBanner(t('session.problem'), 'error');
  }

  try {
    const shell = await mountShellChrome({
      apiGet: (path) => api.get(path),
      onLogout: logout,
      collapsibleSidebar: true,
      onLanguageApplied: () => {
        polishBottomAccount();
        window.dispatchEvent(new Event('hashchange'));
      }
    });
    shellAccount = shell.account;
    window.__adminAccount = shellAccount;
    polishBottomAccount();
  } catch (err) {
    console.error(err);
    handleError(err, t('boot.adminError'));
  }

  if (csrfOk && consumeSignedInWelcome()) {
    toast(t('session.active'), 'success');
  }

  await startRouter(async (name, handler) => {
    clearPageRefresh();
    setActiveNav(name);
    try {
      await handler();
    } catch (err) {
      handleError(err);
      setPageMeta(t('page.error'), t('view.loadError'));
    }
  });
}

boot().catch((err) => {
  console.error(err);
  toast(t('boot.adminError'), 'error');
});
