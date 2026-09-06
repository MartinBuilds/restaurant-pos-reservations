import { setPageMeta, mount, el, panel, badge } from '../ui.js';
import { t } from '/shared/js/i18n/i18n.js?v=pr19-1';

export async function renderDashboard() {
  const account = window.__adminAccount;
  const welcome = account?.name
    ? t('dashboard.welcome', { name: account.name })
    : t('dashboard.welcomeGuest');

  setPageMeta(t('dashboard.title'), t('dashboard.subtitle'));

  const links = [
    [t('nav.users'), 'users', t('dashboard.usersDesc')],
    [t('nav.menu'), 'menu', t('dashboard.menuDesc')],
    [t('nav.inventory'), 'inventory', t('dashboard.inventoryDesc')],
    [t('nav.tables'), 'tables', t('dashboard.tablesDesc')],
    [t('nav.reservations'), 'reservations', t('dashboard.reservationsDesc')],
    [t('nav.payments'), 'payments', t('dashboard.paymentsDesc')],
    [t('nav.reports'), 'reports', t('dashboard.reportsDesc')]
  ];

  mount(el('div', { className: 'stack' }, [
    panel(welcome, [
      el('p', { text: t('dashboard.intro') }),
      el('p', { className: 'muted', text: t('dashboard.note') }),
      el('div', { className: 'row-actions', style: 'margin-top:1rem' }, [
        badge(t('dashboard.badgeAdmin'), 'info'),
        badge(t('dashboard.badgeSession'), 'ok'),
        badge(t('dashboard.badgeStack'), 'muted')
      ])
    ]),
    el('h2', { className: 'panel-title', text: t('dashboard.quickLinks') }),
    el('div', { className: 'grid grid-3' }, links.map(([title, route, desc]) =>
      el('a', { className: 'card card-link', href: `#/${route}` }, [
        el('div', { className: 'card-label', text: title }),
        el('p', { text: desc }),
        el('p', { className: 'muted', text: `#/${route}` })
      ])
    ))
  ]));
}
