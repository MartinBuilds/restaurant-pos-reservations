import { api } from '../api.js';
import { clear, el } from '../dom.js';
import { dateTime, text } from '../format.js';
import {
  badge, closeDialog, errorBox, handleError, loadingBox, openDialog,
  setBanner, setPageMeta, toast
} from '../ui.js';
import { navigate } from '../router.js';
import { setPageRefresh } from '/shared/js/page-refresh.js?v=fix-refresh-4';
import { t } from '/shared/js/i18n/i18n.js?v=pr19-1';

let abortController = null;

function detailRow(label, valueNode) {
  return el('div', { className: 'detail-row' }, [
    el('dt', { text: label }),
    el('dd', {}, [typeof valueNode === 'string' ? document.createTextNode(valueNode) : valueNode])
  ]);
}

export async function renderReservationDetails(root, reservationId) {
  setPageRefresh(() => renderReservationDetails(root, reservationId));
  setPageMeta(t('page.myReservations.title'), t('page.myReservations.subtitle'));
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
      root.appendChild(errorBox(t('common.error'), () => navigate('#/reservations')));
    } else {
      root.appendChild(errorBox(e.message || t('common.error'), () => renderReservationDetails(root, reservationId)));
      handleError(e);
    }
    return;
  }

  clear(root);
  const canMutate = reservation.status === 'CONFIRMED';

  const dl = el('dl', { className: 'detail-list' }, [
    detailRow(t('col.number'), text(reservation.reservationNumber)),
    detailRow(t('col.status'), badge(reservation.status)),
    detailRow(t('col.table'), t('label.tableNamed', {
      number: text(reservation.tableNumber),
      name: text(reservation.tableDisplayName)
    })),
    detailRow(t('col.start'), dateTime(reservation.startTime)),
    detailRow(t('col.end'), dateTime(reservation.endTime)),
    detailRow(t('msg.guests'), text(reservation.guestCount)),
    detailRow(t('col.notes'), reservation.notes ? text(reservation.notes) : '—'),
    reservation.createdAt ? detailRow(t('col.created'), dateTime(reservation.createdAt)) : null,
    reservation.updatedAt ? detailRow(t('col.updated'), dateTime(reservation.updatedAt)) : null
  ].filter(Boolean));

  const actions = el('div', { className: 'actions' }, [
    el('button', {
      type: 'button',
      className: 'btn btn-ghost',
      text: t('common.back'),
      onClick: () => navigate('#/reservations')
    })
  ]);

  if (canMutate) {
    actions.append(
      el('button', {
        type: 'button',
        className: 'btn btn-primary',
        text: t('action.reschedule'),
        onClick: () => navigate(`#/reservations/${reservationId}/edit`)
      }),
      el('button', {
        type: 'button',
        className: 'btn btn-danger',
        text: t('action.cancelReservation'),
        onClick: (ev) => confirmCancel(ev.currentTarget, reservation, root)
      })
    );
  }

  root.append(
    el('section', { className: 'detail-panel' }, [dl]),
    el('p', { className: 'hint', text: t('hint.localTimezone') }),
    actions
  );
}

function confirmCancel(openerEl, reservation, root) {
  const confirmBtn = el('button', {
    type: 'button',
    className: 'btn btn-danger',
    text: t('common.confirm')
  });

  openDialog({
    title: t('action.cancelReservation'),
    body: el('div', { className: 'stack' }, [
      el('p', { text: t('reservations.numberLabel', { number: text(reservation.reservationNumber) }) }),
      el('p', {
        text: t('reservations.intervalLabel', {
          start: dateTime(reservation.startTime),
          end: dateTime(reservation.endTime)
        })
      }),
      el('p', { text: t('common.confirm') })
    ]),
    footer: el('div', { className: 'actions' }, [
      el('button', { type: 'button', className: 'btn btn-ghost', text: t('common.back'), onClick: closeDialog }),
      confirmBtn
    ]),
    openerEl
  });

  confirmBtn.addEventListener('click', async () => {
    confirmBtn.disabled = true;
    try {
      await api.patch(`/api/client/reservations/${reservation.id}/cancel`);
      closeDialog();
      toast(t('msg.reservationCancelled'), 'success');
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
