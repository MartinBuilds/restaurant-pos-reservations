import { clearCsrf, loadCsrf, logout, setUnauthorizedHandler, api } from './api.js';
import { clear } from './dom.js';
import { registerRoute, setActiveNav, startRouter } from './router.js';
import { handleError, setBanner, toast, wireDialogChrome } from './ui.js';
import { abortAvailability, renderAvailability } from './views/availability.js';
import { renderCreateForm, renderEditForm } from './views/reservation-form.js';
import { abortReservationDetails, renderReservationDetails } from './views/reservation-details.js';
import { abortReservations, renderReservations } from './views/reservations.js';
import { decorateNavIcons, mountShellChrome } from '/shared/js/account-shell.js?v=pr20-4';
import { icon } from '/shared/js/icons.js';
import { t } from '/shared/js/i18n/i18n.js?v=pr20-4';

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
      const nav = document.getElementById('main-nav');
      const toggle = document.getElementById('nav-toggle');
      if (nav?.classList.contains('open')) {
        nav.classList.remove('open');
        if (toggle) {
          while (toggle.firstChild) toggle.removeChild(toggle.firstChild);
          toggle.appendChild(icon('expand'));
          toggle.setAttribute('aria-expanded', 'false');
        }
      }
    });
  });

  const toggle = document.getElementById('nav-toggle');
  const nav = document.getElementById('main-nav');
  if (toggle && nav) {
    while (toggle.firstChild) toggle.removeChild(toggle.firstChild);
    toggle.appendChild(icon('expand'));
    toggle.addEventListener('click', () => {
      const open = nav.classList.toggle('open');
      while (toggle.firstChild) toggle.removeChild(toggle.firstChild);
      toggle.appendChild(icon(open ? 'collapse' : 'expand'));
      toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
    });
  }
}

async function boot() {
  wireDialogChrome();
  wireNav();
  decorateNavIcons({
    availability: 'availability',
    reservations: 'reservations'
  });

  setUnauthorizedHandler(() => {
    clearCsrf();
    window.location.assign('/login');
  });

  try {
    await loadCsrf();
  } catch (e) {
    toast(e.message || t('session.csrfError'), 'error');
  }

  await mountShellChrome({
    apiGet: (path) => api.get(path),
    onLogout: async () => {
      abortAll();
      try {
        await logout();
      } catch (e) {
        clearCsrf();
        handleError(e, t('session.logoutError'));
      }
      window.location.assign('/login');
    },
    onLanguageApplied: () => {
      window.dispatchEvent(new Event('hashchange'));
    }
  });

  await startRouter(onRoute);
}

boot().catch((err) => {
  console.error(err);
  toast(t('boot.clientError'), 'error');
});
