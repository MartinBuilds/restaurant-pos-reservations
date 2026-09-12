import { api } from '../api.js';
import { clear, el } from '../dom.js';
import { fromDateTimeLocalValue, text, toDateTimeLocalValue } from '../format.js';
import {
  badge, closeDialog, errorBox, handleError, loadingBox, openDialog,
  setBanner, setPageMeta, toast
} from '../ui.js';
import { navigate } from '../router.js';
import { createDatetimePicker } from '/shared/js/datetime-picker.js?v=fix-client-ui-1';
import { setPageRefresh } from '/shared/js/page-refresh.js?v=fix-refresh-4';
import { t } from '/shared/js/i18n/i18n.js?v=fix-client-ui-1';

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

function guestStepper(id, value = 2) {
  const input = el('input', {
    id,
    type: 'number',
    min: '1',
    step: '1',
    required: true,
    value: String(value),
    className: 'guest-input',
    inputmode: 'numeric'
  });
  const dec = el('button', {
    type: 'button',
    className: 'guest-btn',
    text: '−',
    onClick: () => {
      input.value = String(Math.max(1, Number(input.value || 1) - 1));
    }
  });
  const inc = el('button', {
    type: 'button',
    className: 'guest-btn',
    text: '+',
    onClick: () => {
      input.value = String(Number(input.value || 1) + 1);
    }
  });
  return {
    root: el('div', { className: 'guest-stepper' }, [dec, input, inc]),
    input
  };
}

export async function renderCreateForm(root) {
  setPageRefresh(() => renderCreateForm(root));
  setPageMeta(t('action.createReservation'), t('page.availability.subtitle'));
  setBanner('');
  clear(root);

  const q = parseHashQuery();
  const diningTableId = q.get('diningTableId') || '';
  const tableLabel = [q.get('tableNumber'), q.get('displayName')].filter(Boolean).join(' — ')
    || t('reservations.tableFromSearch');
  const start = toDateTimeLocalValue(q.get('startTime') || '');
  const end = toDateTimeLocalValue(q.get('endTime') || '');
  const guestCount = q.get('guestCount') || '2';

  if (!diningTableId) {
    root.appendChild(errorBox(t('msg.requiredFields'), () => navigate('#/availability')));
    return;
  }

  const startPicker = createDatetimePicker({ value: start });
  startPicker.id = 'cr-start';
  const endPicker = createDatetimePicker({ value: end });
  endPicker.id = 'cr-end';
  const guests = guestStepper('cr-guests', Number(guestCount) || 2);
  const notesInput = el('textarea', { id: 'cr-notes', rows: '3', maxlength: String(NOTES_MAX) });
  const fieldError = el('p', { className: 'field-error', id: 'cr-error', hidden: true });
  const submitBtn = el('button', { type: 'submit', className: 'btn btn-primary', text: t('action.createReservation') });
  const progress = el('p', { className: 'muted', id: 'cr-progress', hidden: true, text: t('common.loading') });

  const form = el('form', { className: 'search-panel booking-panel', novalidate: true }, [
    el('div', { className: 'search-panel-head' }, [
      el('h3', { text: t('action.createReservation') }),
      el('p', { className: 'muted', text: t('client.confirmHint') })
    ]),
    el('div', { className: 'booking-table' }, [
      el('span', { className: 'search-summary-label', text: t('col.table') }),
      el('strong', { text: tableLabel })
    ]),
    el('div', { className: 'search-fields' }, [
      el('div', { className: 'field' }, [
        el('label', { for: 'cr-start', text: t('col.startRequired') }),
        startPicker
      ]),
      el('div', { className: 'field' }, [
        el('label', { for: 'cr-end', text: t('col.endRequired') }),
        endPicker
      ]),
      el('div', { className: 'field field-guests' }, [
        el('label', { for: 'cr-guests', text: `${t('msg.guests')} *` }),
        guests.root
      ])
    ]),
    el('div', { className: 'field' }, [
      el('label', { for: 'cr-notes', text: t('col.notesOptionalMax', { max: NOTES_MAX }) }),
      notesInput
    ]),
    fieldError,
    progress,
    el('div', { className: 'search-actions' }, [
      el('button', { type: 'button', className: 'btn btn-ghost', text: t('common.back'), onClick: () => navigate('#/availability') }),
      submitBtn
    ])
  ]);

  root.appendChild(form);

  form.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    fieldError.hidden = true;
    const err = validate(startPicker.value, endPicker.value, guests.input.value, diningTableId);
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
        startTime: fromDateTimeLocalValue(startPicker.value),
        endTime: fromDateTimeLocalValue(endPicker.value),
        guestCount: Number(guests.input.value)
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
  setPageRefresh(() => renderEditForm(root, reservationId));
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

  const startPicker = createDatetimePicker({ value: toDateTimeLocalValue(reservation.startTime) });
  startPicker.id = 'ed-start';
  const endPicker = createDatetimePicker({ value: toDateTimeLocalValue(reservation.endTime) });
  endPicker.id = 'ed-end';
  const guests = guestStepper('ed-guests', reservation.guestCount || 2);
  const tableIdInput = el('input', {
    id: 'ed-table-id',
    type: 'number',
    min: '1',
    required: true,
    value: String(reservation.diningTableId)
  });
  const notesInput = el('textarea', { id: 'ed-notes', rows: '3', maxlength: String(NOTES_MAX) });
  notesInput.value = reservation.notes || '';
  const fieldError = el('p', { className: 'field-error', hidden: true });
  const submitBtn = el('button', { type: 'submit', className: 'btn btn-primary', text: t('common.save') });
  const progress = el('p', { className: 'muted', hidden: true, text: t('common.loading') });

  const form = el('form', { className: 'search-panel booking-panel', novalidate: true }, [
    el('div', { className: 'search-panel-head' }, [
      el('h3', { text: t('action.reschedule') }),
      el('p', { className: 'muted', text: text(reservation.reservationNumber) })
    ]),
    el('div', { className: 'booking-table' }, [
      el('span', { className: 'search-summary-label', text: t('col.status') }),
      badge(reservation.status)
    ]),
    el('div', { className: 'field' }, [
      el('label', { for: 'ed-table-id', text: t('col.tableIdRequired') }),
      tableIdInput,
      el('p', {
        className: 'field-note',
        text: t('reservations.currentTable', {
          number: text(reservation.tableNumber),
          name: text(reservation.tableDisplayName)
        })
      })
    ]),
    el('div', { className: 'search-fields' }, [
      el('div', { className: 'field' }, [
        el('label', { for: 'ed-start', text: t('col.startRequired') }),
        startPicker
      ]),
      el('div', { className: 'field' }, [
        el('label', { for: 'ed-end', text: t('col.endRequired') }),
        endPicker
      ]),
      el('div', { className: 'field field-guests' }, [
        el('label', { for: 'ed-guests', text: `${t('msg.guests')} *` }),
        guests.root
      ])
    ]),
    el('div', { className: 'field' }, [
      el('label', { for: 'ed-notes', text: t('col.notesMax', { max: NOTES_MAX }) }),
      notesInput
    ]),
    fieldError,
    progress,
    el('div', { className: 'search-actions' }, [
      el('button', {
        type: 'button',
        className: 'btn btn-ghost',
        text: t('common.back'),
        onClick: () => navigate(`#/reservations/${reservationId}`)
      }),
      submitBtn
    ])
  ]);

  root.appendChild(form);

  form.addEventListener('submit', async (ev) => {
    ev.preventDefault();
    fieldError.hidden = true;
    const err = validate(startPicker.value, endPicker.value, guests.input.value, tableIdInput.value);
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
        startTime: fromDateTimeLocalValue(startPicker.value),
        endTime: fromDateTimeLocalValue(endPicker.value),
        guestCount: Number(guests.input.value),
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
