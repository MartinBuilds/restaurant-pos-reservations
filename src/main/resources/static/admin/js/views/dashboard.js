import { setPageMeta, mount, el, panel, badge, setPageRefresh } from '../ui.js';
import { t } from '/shared/js/i18n/i18n.js?v=fix-refresh-1';
import { icon } from '/shared/js/icons.js';

export async function renderDashboard() {
  setPageRefresh(() => renderDashboard());
  const account = window.__adminAccount;
  const welcome = account?.name
    ? t('dashboard.welcome', { name: account.name })
    : t('dashboard.welcomeGuest');

  setPageMeta(t('dashboard.title'), t('dashboard.subtitle'));

  const links = [
    [t('nav.users'), 'users', t('dashboard.usersDesc'), 'users'],
    [t('nav.menu'), 'menu', t('dashboard.menuDesc'), 'menu'],
    [t('nav.inventory'), 'inventory', t('dashboard.inventoryDesc'), 'inventory'],
    [t('nav.tables'), 'tables', t('dashboard.tablesDesc'), 'tables'],
    [t('nav.reservations'), 'reservations', t('dashboard.reservationsDesc'), 'reservations'],
    [t('nav.payments'), 'payments', t('dashboard.paymentsDesc'), 'payments'],
    [t('nav.reports'), 'reports', t('dashboard.reportsDesc'), 'reports']
  ];

  const welcomePanel = panel(welcome, [
    el('p', { text: t('dashboard.intro') }),
    el('p', { className: 'muted', text: t('dashboard.note') }),
    el('div', { className: 'welcome-badges' }, [
      badge(t('dashboard.badgeAdmin'), 'info'),
      badge(t('dashboard.badgeSession'), 'ok'),
      badge(t('dashboard.badgeStack'), 'muted')
    ])
  ]);
  welcomePanel.classList.add('welcome-panel');

  mount(el('div', { className: 'stack' }, [
    welcomePanel,
    el('h2', { className: 'panel-title', text: t('dashboard.quickLinks') }),
    el('div', { className: 'grid grid-3 quick-links' }, links.map(([title, route, desc, iconName]) =>
      el('a', {
        className: 'card card-link',
        href: `#/${route}`,
        'aria-label': title
      }, [
        el('span', { className: 'card-link-arrow', 'aria-hidden': 'true' }),
        el('div', { className: 'card-link-icon' }, [icon(iconName)]),
        el('div', { className: 'card-link-title', text: title }),
        el('p', { className: 'card-link-desc', text: desc })
      ])
    ))
  ]));
}
