import { setPageMeta, mount, el, panel, badge } from '../ui.js';

export async function renderDashboard() {
  setPageMeta('Dashboard', 'Административен панел върху съществуващите REST endpoints');
  const links = [
    ['Потребители', 'users', 'Създаване, роли и статус'],
    ['Меню', 'menu', 'Категории, ястия и наличност'],
    ['Склад и рецепти', 'inventory', 'Съставки, запас и рецепти'],
    ['Маси', 'tables', 'Капацитет, статус и активност'],
    ['Резервации', 'reservations', 'График, създаване и статуси'],
    ['Плащания', 'payments', 'Симулационни CASH/CARD записи'],
    ['Отчети', 'reports', 'Оборот, ястия и методи на плащане']
  ];

  mount(el('div', { className: 'stack' }, [
    panel('Добре дошли', [
      el('p', { text: 'Това е административният интерфейс. Бизнес логиката остава в Spring Boot REST API.' }),
      el('p', { className: 'muted', text: 'Няма нови business endpoints. Данните се зареждат само от съществуващите admin API.' }),
      el('div', { className: 'row-actions', style: 'margin-top:1rem' }, [
        badge('ADMIN only', 'info'),
        badge('Session + CSRF', 'ok'),
        badge('Vanilla JS', 'muted')
      ])
    ]),
    el('div', { className: 'grid grid-3' }, links.map(([title, route, desc]) =>
      el('a', { className: 'card card-link', href: `#/${route}` }, [
        el('div', { className: 'card-label', text: title }),
        el('p', { text: desc }),
        el('p', { className: 'muted', text: `#/${route}` })
      ])
    ))
  ]));
}