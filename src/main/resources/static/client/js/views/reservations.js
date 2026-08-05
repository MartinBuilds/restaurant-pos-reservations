import { api } from '../api.js';
import { clear, el } from '../dom.js';
import { dateTime, text } from '../format.js';
import { badge, emptyBox, errorBox, handleError, loadingBox, setBanner, setPageMeta } from '../ui.js';
import { navigate } from '../router.js';

let abortController = null;

function row(reservation) {
  return el('tr', {}, [
    el('td', { 'data-label': 'Номер', text: text(reservation.reservationNumber) }),
    el('td', { 'data-label': 'Маса', text: `${text(reservation.tableNumber)} — ${text(reservation.tableDisplayName)}` }),
    el('td', { 'data-label': 'Начало', text: dateTime(reservation.startTime) }),
    el('td', { 'data-label': 'Край', text: dateTime(reservation.endTime) }),
    el('td', { 'data-label': 'Гости', text: text(reservation.guestCount) }),
    el('td', { 'data-label': 'Статус' }, [badge(reservation.status)]),
    el('td', { 'data-label': 'Бележки', className: 'wrap', text: reservation.notes ? text(reservation.notes) : '—' }),
    el('td', { 'data-label': 'Действия' }, [
      el('button', {
        type: 'button',
        className: 'btn btn-ghost btn-sm',
        text: 'Детайли',
        onClick: () => navigate(`#/reservations/${reservation.id}`)
      })
    ])
  ]);
}

function card(reservation) {
  return el('article', { className: 'card' }, [
    el('div', { className: 'card-head' }, [
      el('h3', { className: 'wrap', text: text(reservation.reservationNumber) }),
      badge(reservation.status)
    ]),
    el('p', { text: `Маса ${text(reservation.tableNumber)} — ${text(reservation.tableDisplayName)}` }),
    el('p', { text: `${dateTime(reservation.startTime)} – ${dateTime(reservation.endTime)}` }),
    el('p', { text: `Гости: ${text(reservation.guestCount)}` }),
    reservation.notes ? el('p', { className: 'wrap muted', text: text(reservation.notes) }) : null,
    el('div', { className: 'actions' }, [
      el('button', {
        type: 'button',
        className: 'btn btn-primary btn-sm',
        text: 'Детайли',
        onClick: () => navigate(`#/reservations/${reservation.id}`)
      })
    ])
  ]);
}

export async function renderReservations(root) {
  setPageMeta('Моите резервации', 'Списък със собствените ви резервации от сървъра.');
  setBanner('');
  clear(root);

  if (abortController) abortController.abort();
  abortController = new AbortController();
  root.appendChild(loadingBox('Зареждане на резервации…'));

  try {
    const list = await api.get('/api/client/reservations', { signal: abortController.signal });
    clear(root);

    if (!list || !list.length) {
      root.appendChild(emptyBox('Все още нямате резервации.'));
      root.appendChild(el('div', { className: 'actions' }, [
        el('button', {
          type: 'button',
          className: 'btn btn-primary',
          text: 'Нова резервация',
          onClick: () => navigate('#/availability')
        })
      ]));
      return;
    }

    const table = el('table', { className: 'data-table desktop-only' }, [
      el('thead', {}, [
        el('tr', {}, [
          el('th', { text: 'Номер' }),
          el('th', { text: 'Маса' }),
          el('th', { text: 'Начало' }),
          el('th', { text: 'Край' }),
          el('th', { text: 'Гости' }),
          el('th', { text: 'Статус' }),
          el('th', { text: 'Бележки' }),
          el('th', { text: '' })
        ])
      ]),
      el('tbody', {}, list.map(row))
    ]);

    const cards = el('div', { className: 'card-grid mobile-only' }, list.map(card));
    root.append(table, cards);
  } catch (e) {
    if (e && e.name === 'AbortError') return;
    clear(root);
    root.appendChild(errorBox(e.message || 'Грешка при зареждане.', () => renderReservations(root)));
    handleError(e);
  }
}

export function abortReservations() {
  if (abortController) {
    abortController.abort();
    abortController = null;
  }
}