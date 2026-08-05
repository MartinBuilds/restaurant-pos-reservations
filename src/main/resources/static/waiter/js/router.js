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

export function currentRoute() {
  const hash = window.location.hash.replace(/^#\/?/, '');
  const name = (hash.split('?')[0] || 'tables').trim();
  return routes.has(name) ? name : 'tables';
}

export async function startRouter(handler) {
  onRoute = handler;
  const run = async () => {
    const name = currentRoute();
    if (!routes.has(name) && name !== 'tables') {
      window.location.hash = '#/tables';
      return;
    }
    const routeName = routes.has(name) ? name : 'tables';
    if (!window.location.hash || window.location.hash === '#') {
      window.location.hash = `#/${routeName}`;
      return;
    }
    await onRoute(routeName, routes.get(routeName));
  };
  window.addEventListener('hashchange', () => { run().catch(() => {}); });
  await run();
}