const routes = new Map();

export function registerRoute(name, handler) {
  routes.set(name, handler);
}

export function currentRoute() {
  const hash = window.location.hash || '#/dashboard';
  const path = hash.replace(/^#\/?/, '').split('?')[0];
  return path || 'dashboard';
}

export function navigate(name) {
  window.location.hash = `#/${name}`;
}

export async function startRouter(onRoute) {
  const run = async () => {
    let name = currentRoute();
    if (!routes.has(name)) {
      name = 'dashboard';
      if (currentRoute() !== 'dashboard') {
        window.location.hash = '#/dashboard';
        return;
      }
    }
    await onRoute(name, routes.get(name));
  };
  window.addEventListener('hashchange', () => {
    run().catch(console.error);
  });
  await run();
}

export function setActiveNav(routeName) {
  document.querySelectorAll('.nav-link').forEach((link) => {
    const active = link.dataset.route === routeName;
    link.classList.toggle('active', active);
    if (active) link.setAttribute('aria-current', 'page');
    else link.removeAttribute('aria-current');
  });
}