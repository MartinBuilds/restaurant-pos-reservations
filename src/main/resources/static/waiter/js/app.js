import { ensureCsrf, logout, setUnauthorizedHandler, api } from '/operations/js/api.js';
import { handleError, setBanner, toast } from '/operations/js/notifications.js';
import { onRealtimeRefresh, startWaiterRealtime, stopRealtime } from './realtime.js';
import { registerRoute, setActiveNav, startRouter } from './router.js';
import { renderOrders } from './views/orders.js';
import { renderReservations } from './views/reservations.js';
import { renderTables } from './views/tables.js';
import { wireDialogChrome } from './views/ui-shared.js';
import { decorateNavIcons, mountShellChrome } from '/shared/js/account-shell.js?v=pr17-4';
import { t } from '/shared/js/i18n/i18n.js?v=pr17-4';

registerRoute('tables', renderTables);
registerRoute('orders', renderOrders);
registerRoute('reservations', renderReservations);

function wireShell() {
  const sidebar = document.getElementById('sidebar');
  const toggle = document.getElementById('nav-toggle');
  toggle.addEventListener('click', () => {
    const open = sidebar.classList.toggle('open');
    toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
  });
  document.getElementById('main-nav').addEventListener('click', (event) => {
    if (event.target.closest('a.nav-link')) {
      sidebar.classList.remove('open');
      toggle.setAttribute('aria-expanded', 'false');
    }
  });
  wireDialogChrome();
}

async function boot() {
  wireShell();
  decorateNavIcons({
    tables: 'tables',
    orders: 'orders',
    reservations: 'reservations'
  });

  setUnauthorizedHandler(() => {
    stopRealtime();
    window.location.assign('/login');
  });

  let csrfOk = false;
  try {
    await ensureCsrf();
    csrfOk = true;
  } catch (err) {
    handleError(err, t('session.csrfError'));
    setBanner(t('session.problem'), 'error');
  }

  try {
    await mountShellChrome({
      apiGet: (path) => api.get(path),
      collapsibleSidebar: true,
      onLogout: async () => {
        try {
          stopRealtime();
          await logout();
        } catch {
          // always leave
        }
        window.location.assign('/login');
      },
      onLanguageApplied: () => {
        window.dispatchEvent(new Event('hashchange'));
      }
    });
  } catch (err) {
    console.error(err);
    handleError(err, t('boot.waiterError'));
  }

  if (csrfOk) {
    setBanner(t('session.activeShort'), 'success');
  }

  onRealtimeRefresh(async () => {
    const hash = window.location.hash || '';
    if (hash.includes('orders')) await renderOrders();
    else if (hash.includes('tables')) await renderTables();
  });

  await startRouter(async (name, handler) => {
    setActiveNav(name);
    try {
      await handler();
    } catch (err) {
      handleError(err);
    }
  });

  if (csrfOk) {
    try {
      await startWaiterRealtime();
    } catch (err) {
      handleError(err, t('session.wsError'));
    }
  }
}

boot().catch((err) => {
  console.error(err);
  toast(t('boot.waiterError'), 'error');
});
