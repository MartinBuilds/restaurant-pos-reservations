import { ensureCsrf, logout, setUnauthorizedHandler, api } from '/operations/js/api.js';
import { handleError, setBanner, toast } from '/operations/js/notifications.js';
import { onRealtimeRefresh, startWaiterRealtime, stopRealtime } from './realtime.js?v=fix-table-live-1';
import { registerRoute, setActiveNav, startRouter } from './router.js';
import { renderOrders } from './views/orders.js?v=fix-orders-board-1';
import { renderReservations } from './views/reservations.js';
import { renderTables } from './views/tables.js?v=fix-table-live-1';
import { wireDialogChrome } from './views/ui-shared.js';
import { decorateNavIcons, mountShellChrome } from '/shared/js/account-shell.js?v=fix-account-sheet-1';
import { polishBottomAccount, wireBottomNav } from '/shared/js/bottom-nav.js?v=fix-account-sheet-1';
import { consumeSignedInWelcome } from '/shared/js/session-flash.js?v=fix-toasts-3';
import { clearPageRefresh, wirePageRefresh } from '/shared/js/page-refresh.js?v=fix-refresh-4';
import { t } from '/shared/js/i18n/i18n.js?v=fix-table-live-1';

registerRoute('tables', renderTables);
registerRoute('orders', renderOrders);
registerRoute('reservations', renderReservations);

function wireShell() {
  wireBottomNav();
  wireDialogChrome();
}

async function boot() {
  decorateNavIcons({
    tables: 'tables',
    orders: 'orders',
    reservations: 'reservations'
  });
  wireShell();
  wirePageRefresh();

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
        polishBottomAccount();
        window.dispatchEvent(new Event('hashchange'));
      }
    });
    polishBottomAccount();
  } catch (err) {
    console.error(err);
    handleError(err, t('boot.waiterError'));
  }

  if (csrfOk && consumeSignedInWelcome()) {
    toast(t('session.activeShort'), 'success');
  }

  onRealtimeRefresh(async () => {
    const hash = window.location.hash || '';
    if (hash.includes('orders')) await renderOrders();
    else if (hash.includes('tables')) await renderTables();
  });

  await startRouter(async (name, handler) => {
    clearPageRefresh();
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
