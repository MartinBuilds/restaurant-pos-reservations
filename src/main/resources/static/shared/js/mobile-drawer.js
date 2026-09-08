import { icon } from '/shared/js/icons.js';

function clear(node) {
  while (node.firstChild) node.removeChild(node.firstChild);
}

/**
 * Mobile drawer: open from topbar arrow; close from sticky sidebar arrow / scrim / Escape.
 */
export function wireMobileSidebarDrawer({
  sidebarId = 'sidebar',
  toggleId = 'nav-toggle',
  closeId = 'sidebar-drawer-close',
  scrimId = 'sidebar-scrim',
  navId = 'main-nav'
} = {}) {
  const sidebar = document.getElementById(sidebarId);
  const toggle = document.getElementById(toggleId);
  const closeBtn = document.getElementById(closeId);
  const scrim = document.getElementById(scrimId);
  const nav = document.getElementById(navId);
  if (!sidebar || !toggle) return;

  const syncIcons = (open) => {
    clear(toggle);
    toggle.appendChild(icon('expand'));
    toggle.setAttribute('aria-expanded', open ? 'true' : 'false');
    if (closeBtn) {
      clear(closeBtn);
      closeBtn.appendChild(icon('collapse'));
    }
  };

  const setOpen = (open) => {
    sidebar.classList.toggle('open', open);
    document.body.classList.toggle('sidebar-drawer-open', open);
    if (scrim) {
      scrim.hidden = !open;
      scrim.classList.toggle('is-open', open);
      scrim.setAttribute('aria-hidden', open ? 'false' : 'true');
    }
    syncIcons(open);
  };

  syncIcons(false);

  toggle.addEventListener('click', (event) => {
    event.stopPropagation();
    setOpen(true);
  });

  if (closeBtn) {
    closeBtn.addEventListener('click', (event) => {
      event.stopPropagation();
      setOpen(false);
    });
  }

  if (scrim) {
    scrim.addEventListener('click', () => setOpen(false));
  }

  if (nav) {
    nav.addEventListener('click', (event) => {
      if (event.target.closest('a.nav-link')) setOpen(false);
    });
  }

  document.addEventListener('keydown', (event) => {
    if (event.key !== 'Escape' || !sidebar.classList.contains('open')) return;
    const dialog = document.getElementById('dialog');
    if (dialog && dialog.classList.contains('is-open')) return;
    setOpen(false);
  });
}
