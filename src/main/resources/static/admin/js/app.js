import { loadCsrf, api } from './api.js';
import { registerRoute, startRouter, setActiveNav } from './router.js';
import { setBanner, toast, handleError, setPageMeta } from './ui.js';
import { renderDashboard } from './views/dashboard.js';
import { renderUsers } from './views/users.js';
import { renderMenu } from './views/menu.js';
import { renderInventory } from './views/inventory.js';
import { renderTables } from './views/tables.js';
import { renderReservations } from './views/reservations.js';
import { renderPayments } from './views/payments.js';
import { renderReports } from './views/reports.js';

registerRoute('dashboard', renderDashboard);
registerRoute('users', renderUsers);
registerRoute('menu', renderMenu);
registerRoute('inventory', renderInventory);
registerRoute('tables', renderTables);
registerRoute('reservations', renderReservations);
registerRoute('payments', renderPayments);
registerRoute('reports', renderReports);

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
  document.getElementById('logout-btn').addEventListener('click', () => {
    logout().catch((err) => handleError(err, 'Изходът неуспешен.'));
  });
}

async function boot() {
  wireShell();
  try {
    await loadCsrf();
    setBanner('Сесията е активна. CSRF токенът е зареден.', 'success');
  } catch (err) {
    handleError(err, 'Неуспешно зареждане на CSRF токен.');
    setBanner('Проблем със сесията или CSRF.', 'error');
    return;
  }

  await startRouter(async (name, handler) => {
    setActiveNav(name);
    try {
      await handler();
    } catch (err) {
      handleError(err);
      setPageMeta('Грешка', 'Изгледът не можа да се зареди.');
    }
  });
}

boot().catch((err) => {
  console.error(err);
  toast('Административният панел не можа да стартира.', 'error');
});