const routes = new Map();
let onRoute = null;

export function registerRoute(name, handler) {
  routes.set(name, handler);
}

export function setActiveNav(name) {
  document.querySelectorAll('#main-nav .nav-link').forEach((link) => {
    link.classList.toggle('active', link.dataset.route === name);
  });
}

export function navigate(hash) {
  window.location.hash = hash.startsWith('#') ? hash : `#/${hash}`;
}

export function currentRoute() {
  const rawFull = (window.location.hash || '').replace(/^#\/?/, '');
  const raw = rawFull.split('?')[0];
  const [name, ...rest] = (raw || 'availability').split('/').filter(Boolean);
  const route = routes.has(name) ? name : 'availability';
  return { name: route, rest };
}

export async function startRouter(handler) {
  onRoute = handler;
  const run = async () => {
    const { name, rest } = currentRoute();
    if (!window.location.hash || window.location.hash === '#') {
      window.location.hash = '#/availability';
      return;
    }
    if (!routes.has(name)) {
      window.location.hash = '#/availability';
      return;
    }
    await onRoute(name, routes.get(name), rest);
  };
  window.addEventListener('hashchange', () => { run().catch(() => {}); });
  await run();
}