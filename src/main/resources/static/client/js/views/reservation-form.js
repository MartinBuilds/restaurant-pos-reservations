import { api } from '../api.js';
import { clear, el } from '../dom.js';
import { text, toDateTimeLocalValue } from '../format.js';
import {
  badge, closeDialog, errorBox, handleError, loadingBox, openDialog,
  setBanner, setPageMeta, toast
} from '../ui.js';
import { navigate } from '../router.js';
import { t } from '/shared/js/i18n/i18n.js?v=pr19-1';

const NOTES_MAX = 500;

function parseHashQuery() {
  const hash = window.location.hash || '';
  const qIndex = hash.indexOf('?');
  if (qIndex < 0) return new URLSearchParams();
  return new URLSearchParams(hash.slice(qIndex + 1));
}

function validate(start, end, guestCount, diningTableId) {
  if (!diningTableId) return t('msg.requiredFields');
  if (!start || !end || !guestCount) return t('msg.requiredFields');
  if (start >= end) return t('msg.startBeforeEnd');
  const n = Number(guestCount);
  if (!Number.isInteger(n) || n < 1) return t('msg.guestCountInvalid');
  return null;
}

export async function renderCreateForm(root) {
  setPageMeta(t('action.createReservation'), t('page.availability.subtitle'));
  setBanner('');
  clear(root);

  const q = parseHashQuery();
  const diningTableId = q.get('diningTableId') || '';
  const tableLabel = [q.get('tableNumber'), q.get('displayName')].filter(Boolean).join(' — ')
    || t('reservations.tableFromSearch');
  const start = toDateTimeLocalValue(q.get('startTime') || '');
  const end = toDateTimeLocalValue(q.get('endTime') || '');
  const guests = q.get('guestCount') || '2';

  if (!diningTableId) {
    root.appendChild(errorBox(t('msg.requiredFields'), () => navigate('#/availability')));
    return;
  }

  const form = el('form', { className: 'form-grid', novalidate: true });
  const startInput = el('input', { id: 'cr-start', type: 'datetime-local', required: true, value: start });
  const endInput = el('input', { id: 'cr-end', type: 'datetime-local', required: true, value: end });
  const guestInput = el('input', { id: 'cr-guests', type: 'number', min: '1', step: '1', required: true, value: guests });
  const notesInput = el('textarea', { id: 'cr-notes', rows: '3', maxlength: String(NOTES_MAX) });
  const tableIdHidden = el('input', { type: 'hidden', id: 'cr-table-id', value: diningTableId });
  const fieldError = el('p', { className: 'field-error', id: 'cr-error', hidden: true });
  const submitBtn = el('button', { type: 'submit', className: 'btn btn-primary', text: t('action.createReservation') });
  const progress = el('p', { className: 'muted', id: 'cr-progress', hidden: true, text: t('common.loading') });

  form.append(
    el('div', { className: 'field' }, [
      el('label', { text: t('col.table') }),
      el('p', {
        className: 'readonly',
        text: t('label.tableWithId', { label: tableLabel, id: diningTableId })
      }),
      tableIdHidden
    ]),
    el('div', { className: 'field' }, [
      el('label', { for: 'cr-start', text: t('col.startRequired') }),
      startInput,
      el('p', { className: 'hint', text: t('hint.localTimezone') })
    ]),
    el('div', { className: 'field' }, [
      el('label', { for: 'cr-end', text: t('col.endRequired') }),
      endInput
    ]),
    el('div', { className: 'field' }, [
      el('label', { for: 'cr-guests', text: `${t('msg.guests')} *` }),
      guestInput
    ]),
    el('div', { className: 'field' }, [
      el('label', { for: 'cr-notes', text: t('col.notesOptionalMax', { max: NOTES_MAX }) }),
      notesInput
    ]),
    fieldError,
    progress,
    el('div', { className: 'actions' }, [
      el('button', { type: 'button', className: 'btn btn-ghost', text: t('common.back'), onClick: () => navigate('#/availability') }),
      submitBtn
    ])
  );

  root.appendChild(form);

  form.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    fieldError.hidden = true;
    const err = validate(startInput.value, endInput.value, guestInput.value, diningTableId);
    if (err) {
      fieldError.textContent = err;
      fieldError.hidden = false;
      return;
    }

    submitBtn.disabled = true;
    progress.hidden = false;
    try {
      const notes = notesInput.value.trim();
      const body = {
        diningTableId: Number(diningTableId),
        startTime: startInput.value.length === 16 ? `${startInput.value}:00` : startInput.value,
        endTime: endInput.value.length === 16 ? `${endInput.value}:00` : endInput.value,
        guestCount: Number(guestInput.value)
      };
      if (notes) body.notes = notes;

      const created = await api.post('/api/client/reservations', body);
      toast(t('msg.reservationCreated'), 'success');
      navigate(`#/reservations/${created.id}`);
    } catch (e) {
      if (e && e.status === 409) {
        fieldError.textContent = e.message || t('common.error');
        fieldError.hidden = false;
        setBanner(e.message, 'error');
        openDialog({
          title: t('common.error'),
          body: el('p', { text: e.message || t('common.error') }),
          footer: el('div', { className: 'actions' }, [
            el('button', {
              type: 'button',
              className: 'btn btn-primary',
              text: t('action.search'),
              onClick: () => { closeDialog(); navigate('#/availability'); }
            }),
            el('button', { type: 'button', className: 'btn btn-ghost', text: t('common.close'), onClick: closeDialog })
          ]),
          openerEl: submitBtn
        });
      } else {
        handleError(e);
        fieldError.textContent = e.message || t('common.error');
        fieldError.hidden = false;
      }
    } finally {
      submitBtn.disabled = false;
      progress.hidden = true;
    }
  });
}

export async function renderEditForm(root, reservationId) {
  setPageMeta(t('action.reschedule'), t('page.myReservations.subtitle'));
  setBanner('');
  clear(root);
  root.appendChild(loadingBox());

  let reservation;
  try {
    reservation = await api.get(`/api/client/reservations/${reservationId}`);
  } catch (e) {
    clear(root);
    if (e && e.status === 404) {
      root.appendChild(errorBox(t('common.error'), () => navigate('#/reservations')));
    } else {
      root.appendChild(errorBox(e.message || t('common.error'), () => renderEditForm(root, reservationId)));
      handleError(e);
    }
    return;
  }

  clear(root);
  if (reservation.status !== 'CONFIRMED') {
    root.appendChild(errorBox(t('common.error'), () => navigate(`#/reservations/${reservationId}`)));
    return;
  }

  const form = el('form', { className: 'form-grid', novalidate: true });
  const tableIdInput = el('input', {
    id: 'ed-table-id',
    type: 'number',
    min: '1',
    required: true,
    value: String(reservation.diningTableId)
  });
  const startInput = el('input', {
    id: 'ed-start',
    type: 'datetime-local',
    required: true,
    value: toDateTimeLocalValue(reservation.startTime)
  });
  const endInput = el('input', {
    id: 'ed-end',
    type: 'datetime-local',
    required: true,
    value: toDateTimeLocalValue(reservation.endTime)
  });
  const guestInput = el('input', {
    id: 'ed-guests',
    type: 'number',
    min: '1',
    step: '1',
    required: true,
    value: String(reservation.guestCount)
  });
  const notesInput = el('textarea', { id: 'ed-notes', rows: '3', maxlength: String(NOTES_MAX) });
  notesInput.value = reservation.notes || '';
  const fieldError = el('p', { className: 'field-error', hidden: true });
  const submitBtn = el('button', { type: 'submit', className: 'btn btn-primary', text: t('common.save') });
  const progress = el('p', { className: 'muted', hidden: true, text: t('common.loading') });

  form.append(
    el('div', { className: 'field' }, [
      el('label', { text: t('col.number') }),
      el('p', { className: 'readonly', text: text(reservation.reservationNumber) })
    ]),
    el('div', { className: 'field' }, [
      el('label', { text: t('col.status') }),
      badge(reservation.status)
    ]),
    el('div', { className: 'field' }, [
      el('label', { for: 'ed-table-id', text: t('col.tableIdRequired') }),
      tableIdInput,
      el('p', {
        className: 'hint',
        text: t('reservations.currentTable', {
          number: text(reservation.tableNumber),
          name: text(reservation.tableDisplayName)
        })
      })
    ]),
    el('div', { className: 'field' }, [
      el('label', { for: 'ed-start', text: t('col.startRequired') }),
      startInput,
      el('p', { className: 'hint', text: t('hint.localTimezone') })
    ]),
    el('div', { className: 'field' }, [
      el('label', { for: 'ed-end', text: t('col.endRequired') }),
      endInput
    ]),
    el('div', { className: 'field' }, [
      el('label', { for: 'ed-guests', text: `${t('msg.guests')} *` }),
      guestInput
    ]),
    el('div', { className: 'field' }, [
      el('label', { for: 'ed-notes', text: t('col.notesMax', { max: NOTES_MAX }) }),
      notesInput
    ]),
    fieldError,
    progress,
    el('div', { className: 'actions' }, [
      el('button', {
        type: 'button',
        className: 'btn btn-ghost',
        text: t('action.search'),
        onClick: () => navigate('#/availability')
      }),
      el('button', {
        type: 'button',
        className: 'btn btn-ghost',
        text: t('common.cancel'),
        onClick: () => navigate(`#/reservations/${reservationId}`)
      }),
      submitBtn
    ])
  );

  root.appendChild(form);

  form.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    fieldError.hidden = true;
    const err = validate(startInput.value, endInput.value, guestInput.value, tableIdInput.value);
    if (err) {
      fieldError.textContent = err;
      fieldError.hidden = false;
      return;
    }

    submitBtn.disabled = true;
    progress.hidden = false;
    try {
      const notes = notesInput.value.trim();
      const body = {
        diningTableId: Number(tableIdInput.value),
        startTime: startInput.value.length === 16 ? `${startInput.value}:00` : startInput.value,
        endTime: endInput.value.length === 16 ? `${endInput.value}:00` : endInput.value,
        guestCount: Number(guestInput.value),
        notes: notes || null
      };
      await api.put(`/api/client/reservations/${reservationId}`, body);
      toast(t('msg.reservationUpdated'), 'success');
      navigate(`#/reservations/${reservationId}`);
    } catch (e) {
      handleError(e);
      fieldError.textContent = e.message || t('common.error');
      fieldError.hidden = false;
    } finally {
      submitBtn.disabled = false;
      progress.hidden = true;
    }
  });
}
