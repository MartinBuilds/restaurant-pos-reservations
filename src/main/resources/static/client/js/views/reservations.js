import { api } from '../api.js';
import { clear, el } from '../dom.js';
import { dateTime, text } from '../format.js';
import { badge, emptyBox, errorBox, handleError, loadingBox, setBanner, setPageMeta } from '../ui.js';
import { navigate } from '../router.js';
import { t } from '/shared/js/i18n/i18n.js?v=pr17-4';

let abortController = null;

function row(reservation) {
  return el('tr', {}, [
    el('td', { 'data-label': t('col.number'), text: text(reservation.reservationNumber) }),
    el('td', {
      'data-label': t('col.table'),
      text: t('label.tableNamed', {
        number: text(reservation.tableNumber),
        name: text(reservation.tableDisplayName)
      })
    }),
    el('td', { 'data-label': t('col.start'), text: dateTime(reservation.startTime) }),
    el('td', { 'data-label': t('col.end'), text: dateTime(reservation.endTime) }),
    el('td', { 'data-label': t('msg.guests'), text: text(reservation.guestCount) }),
    el('td', { 'data-label': t('col.status') }, [badge(reservation.status)]),
    el('td', { 'data-label': t('col.notes'), className: 'wrap', text: reservation.notes ? text(reservation.notes) : '—' }),
    el('td', { 'data-label': t('common.actions') }, [
      el('button', {
        type: 'button',
        className: 'btn btn-ghost btn-sm',
        text: t('action.details'),
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
    el('p', { text: t('label.tableNamed', {
      number: text(reservation.tableNumber),
      name: text(reservation.tableDisplayName)
    }) }),
    el('p', { text: `${dateTime(reservation.startTime)} – ${dateTime(reservation.endTime)}` }),
    el('p', { text: `${t('msg.guests')}: ${text(reservation.guestCount)}` }),
    reservation.notes ? el('p', { className: 'wrap muted', text: text(reservation.notes) }) : null,
    el('div', { className: 'actions' }, [
      el('button', {
        type: 'button',
        className: 'btn btn-primary btn-sm',
        text: t('action.details'),
        onClick: () => navigate(`#/reservations/${reservation.id}`)
      })
    ])
  ]);
}

export async function renderReservations(root) {
  setPageMeta(t('page.myReservations.title'), t('page.myReservations.subtitle'));
  setBanner('');
  clear(root);

  if (abortController) abortController.abort();
  abortController = new AbortController();
  root.appendChild(loadingBox(t('common.loading')));

  try {
    const list = await api.get('/api/client/reservations', { signal: abortController.signal });
    clear(root);

    if (!list || !list.length) {
      root.appendChild(emptyBox(t('common.empty')));
      root.appendChild(el('div', { className: 'actions' }, [
        el('button', {
          type: 'button',
          className: 'btn btn-primary',
          text: t('page.availability.title'),
          onClick: () => navigate('#/availability')
        })
      ]));
      return;
    }

    const table = el('table', { className: 'data-table desktop-only' }, [
      el('thead', {}, [
        el('tr', {}, [
          el('th', { text: t('col.number') }),
          el('th', { text: t('col.table') }),
          el('th', { text: t('col.start') }),
          el('th', { text: t('col.end') }),
          el('th', { text: t('msg.guests') }),
          el('th', { text: t('col.status') }),
          el('th', { text: t('col.notes') }),
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
    root.appendChild(errorBox(e.message || t('common.error'), () => renderReservations(root)));
    handleError(e);
  }
}

export function abortReservations() {
  if (abortController) {
    abortController.abort();
    abortController = null;
  }
}
