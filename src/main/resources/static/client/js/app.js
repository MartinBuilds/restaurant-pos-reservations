import { clearCsrf, loadCsrf, logout, setUnauthorizedHandler } from './api.js';
import { clear } from './dom.js';
import { registerRoute, setActiveNav, startRouter } from './router.js';
import { handleError, setBanner, toast, wireDialogChrome } from './ui.js';
import { abortAvailability, renderAvailability } from './views/availability.js';
import { renderCreateForm, renderEditForm } from './views/reservation-form.js';
import { abortReservationDetails, renderReservationDetails } from './views/reservation-details.js';
import { abortReservations, renderReservations } from './views/reservations.js';

const root = document.getElementById('view-root');

function abortAll() {
  abortAvailability();
  abortReservations();
  abortReservationDetails();
}

registerRoute('availability', async () => {
  setActiveNav('availability');
  await renderAvailability(root);
});

registerRoute('new', async () => {
  setActiveNav('availability');
  await renderCreateForm(root);
});

registerRoute('reservations', async (rest) => {
  setActiveNav('reservations');
  if (rest[0] && rest[1] === 'edit') {
    await renderEditForm(root, rest[0]);
    return;
  }
  if (rest[0]) {
    await renderReservationDetails(root, rest[0]);
    return;
  }
  await renderReservations(root);
});

async function onRoute(name, handler, rest) {
  abortAll();
  clear(root);
  setBanner('');
  try {
    await handler(rest);
  } catch (e) {
    handleError(e);
  }
}

function wireNav() {
  document.querySelectorAll('#main-nav .nav-link').forEach((link) => {
    link.addEventListener('click', (ev) => {
      ev.preventDefault();
      const route = link.dataset.route;
      if (route === 'availability') window.location.hash = '#/availability';
      if (route === 'reservations') window.location.hash = '#/reservations';
    });
  });

  const toggle = document.getElementById('nav-toggle');
  const nav = document.getElementById('main-nav');
  if (toggle && nav) {
    toggle.addEventListener('click', () => {
      const open = nav.classList.toggle('open');
      toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
    });
  }

  document.getElementById('logout-btn')?.addEventListener('click', async () => {
    abortAll();
    try {
      await logout();
      window.location.assign('/login');
    } catch (e) {
      clearCsrf();
      handleError(e, 'Неуспешен изход.');
      window.location.assign('/login');
    }
  });
}

async function boot() {
  wireDialogChrome();
  wireNav();
  setUnauthorizedHandler(() => {
    clearCsrf();
    window.location.assign('/login');
  });

  try {
    await loadCsrf();
  } catch (e) {
    toast(e.message || 'Неуспешно зареждане на CSRF.', 'error');
  }

  await startRouter(onRoute);
}

boot();