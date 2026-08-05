import { ensureCsrf, logout, setUnauthorizedHandler } from '/operations/js/api.js';
import { handleError, setBanner, toast } from '/operations/js/notifications.js';
import { onRealtimeRefresh, startWaiterRealtime, stopRealtime } from './realtime.js';
import { registerRoute, setActiveNav, startRouter } from './router.js';
import { renderOrders } from './views/orders.js';
import { renderReservations } from './views/reservations.js';
import { renderTables } from './views/tables.js';
import { wireDialogChrome } from './views/ui-shared.js';

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
  document.getElementById('main-nav').addEventListener('click', () => {
    sidebar.classList.remove('open');
    toggle.setAttribute('aria-expanded', 'false');
  });
  document.getElementById('logout-btn').addEventListener('click', async () => {
    try {
      stopRealtime();
      await logout();
    } catch {
      // always leave
    }
    window.location.assign('/login');
  });
  wireDialogChrome();
}

async function boot() {
  wireShell();
  setUnauthorizedHandler(() => {
    stopRealtime();
    window.location.assign('/login');
  });

  try {
    await ensureCsrf();
    setBanner('Сесията е активна.', 'success');
  } catch (err) {
    handleError(err, 'CSRF не можа да се зареди.');
    return;
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

  try {
    await startWaiterRealtime();
  } catch (err) {
    handleError(err, 'WebSocket връзката не стартира.');
  }
}

boot().catch((err) => {
  console.error(err);
  toast('Сервитьорският панел не можа да стартира.', 'error');
});