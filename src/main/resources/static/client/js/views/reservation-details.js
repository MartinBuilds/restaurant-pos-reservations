import { api } from '../api.js';
import { clear, el } from '../dom.js';
import { dateTime, text } from '../format.js';
import {
  badge, closeDialog, errorBox, handleError, loadingBox, openDialog,
  setBanner, setPageMeta, toast
} from '../ui.js';
import { navigate } from '../router.js';

let abortController = null;

function detailRow(label, valueNode) {
  return el('div', { className: 'detail-row' }, [
    el('dt', { text: label }),
    el('dd', {}, [typeof valueNode === 'string' ? document.createTextNode(valueNode) : valueNode])
  ]);
}

export async function renderReservationDetails(root, reservationId) {
  setPageMeta('Детайли за резервация', 'Данни от REST за собствена резервация.');
  setBanner('');
  clear(root);

  if (abortController) abortController.abort();
  abortController = new AbortController();
  root.appendChild(loadingBox());

  let reservation;
  try {
    reservation = await api.get(`/api/client/reservations/${reservationId}`, { signal: abortController.signal });
  } catch (e) {
    if (e && e.name === 'AbortError') return;
    clear(root);
    if (e && e.status === 404) {
      root.appendChild(errorBox('Резервацията не е намерена.', () => navigate('#/reservations')));
    } else {
      root.appendChild(errorBox(e.message || 'Грешка.', () => renderReservationDetails(root, reservationId)));
      handleError(e);
    }
    return;
  }

  clear(root);
  const canMutate = reservation.status === 'CONFIRMED';

  const dl = el('dl', { className: 'detail-list' }, [
    detailRow('Номер', text(reservation.reservationNumber)),
    detailRow('Статус', badge(reservation.status)),
    detailRow('Маса', `Маса ${text(reservation.tableNumber)} — ${text(reservation.tableDisplayName)}`),
    detailRow('Начало', dateTime(reservation.startTime)),
    detailRow('Край', dateTime(reservation.endTime)),
    detailRow('Гости', text(reservation.guestCount)),
    detailRow('Бележки', reservation.notes ? text(reservation.notes) : '—'),
    reservation.createdAt ? detailRow('Създадена', dateTime(reservation.createdAt)) : null,
    reservation.updatedAt ? detailRow('Обновена', dateTime(reservation.updatedAt)) : null
  ].filter(Boolean));

  const actions = el('div', { className: 'actions' }, [
    el('button', {
      type: 'button',
      className: 'btn btn-ghost',
      text: 'Към списъка',
      onClick: () => navigate('#/reservations')
    })
  ]);

  if (canMutate) {
    actions.append(
      el('button', {
        type: 'button',
        className: 'btn btn-primary',
        text: 'Пренасрочи',
        onClick: () => navigate(`#/reservations/${reservationId}/edit`)
      }),
      el('button', {
        type: 'button',
        className: 'btn btn-danger',
        text: 'Откажи резервацията',
        onClick: (ev) => confirmCancel(ev.currentTarget, reservation, root)
      })
    );
  }

  root.append(
    el('section', { className: 'detail-panel' }, [dl]),
    el('p', { className: 'hint', text: 'Всички часове са в локалната часова зона на ресторанта.' }),
    actions
  );
}

function confirmCancel(openerEl, reservation, root) {
  const confirmBtn = el('button', {
    type: 'button',
    className: 'btn btn-danger',
    text: 'Потвърди отказ'
  });

  openDialog({
    title: 'Отказване на резервация',
    body: el('div', { className: 'stack' }, [
      el('p', { text: `Номер: ${text(reservation.reservationNumber)}` }),
      el('p', {
        text: `Интервал: ${dateTime(reservation.startTime)} – ${dateTime(reservation.endTime)}`
      }),
      el('p', { text: 'Сигурни ли сте, че искате да откажете тази резервация?' })
    ]),
    footer: el('div', { className: 'actions' }, [
      el('button', { type: 'button', className: 'btn btn-ghost', text: 'Назад', onClick: closeDialog }),
      confirmBtn
    ]),
    openerEl
  });

  confirmBtn.addEventListener('click', async () => {
    confirmBtn.disabled = true;
    try {
      await api.patch(`/api/client/reservations/${reservation.id}/cancel`);
      closeDialog();
      toast('Резервацията е отказана.', 'success');
      await renderReservationDetails(root, reservation.id);
    } catch (e) {
      handleError(e);
      confirmBtn.disabled = false;
    }
  });
}

export function abortReservationDetails() {
  if (abortController) {
    abortController.abort();
    abortController = null;
  }
}