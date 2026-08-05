import { api } from '../api.js';
import { clear, el } from '../dom.js';
import { dateTime, text, toDateTimeLocalValue } from '../format.js';
import {
  badge, closeDialog, errorBox, handleError, loadingBox, openDialog,
  setBanner, setPageMeta, toast
} from '../ui.js';
import { navigate } from '../router.js';

const NOTES_MAX = 500;

function parseHashQuery() {
  const hash = window.location.hash || '';
  const qIndex = hash.indexOf('?');
  if (qIndex < 0) return new URLSearchParams();
  return new URLSearchParams(hash.slice(qIndex + 1));
}

function validate(start, end, guestCount, diningTableId) {
  if (!diningTableId) return 'Изберете маса.';
  if (!start || !end || !guestCount) return 'Всички задължителни полета трябва да са попълнени.';
  if (start >= end) return 'Началото трябва да е преди края.';
  const n = Number(guestCount);
  if (!Number.isInteger(n) || n < 1) return 'Броят гости трябва да е положително цяло число.';
  return null;
}

export async function renderCreateForm(root) {
  setPageMeta('Създаване на резервация', 'Попълнете данните и потвърдете. Client identity идва от сесията.');
  setBanner('');
  clear(root);

  const q = parseHashQuery();
  const diningTableId = q.get('diningTableId') || '';
  const tableLabel = [q.get('tableNumber'), q.get('displayName')].filter(Boolean).join(' — ') || 'избрана от търсенето';
  const start = toDateTimeLocalValue(q.get('startTime') || '');
  const end = toDateTimeLocalValue(q.get('endTime') || '');
  const guests = q.get('guestCount') || '2';

  if (!diningTableId) {
    root.appendChild(errorBox('Няма избрана маса. Първо потърсете свободни маси.', () => navigate('#/availability')));
    return;
  }

  const form = el('form', { className: 'form-grid', novalidate: true });
  const startInput = el('input', { id: 'cr-start', type: 'datetime-local', required: true, value: start });
  const endInput = el('input', { id: 'cr-end', type: 'datetime-local', required: true, value: end });
  const guestInput = el('input', { id: 'cr-guests', type: 'number', min: '1', step: '1', required: true, value: guests });
  const notesInput = el('textarea', { id: 'cr-notes', rows: '3', maxlength: String(NOTES_MAX) });
  const tableIdHidden = el('input', { type: 'hidden', id: 'cr-table-id', value: diningTableId });
  const fieldError = el('p', { className: 'field-error', id: 'cr-error', hidden: true });
  const submitBtn = el('button', { type: 'submit', className: 'btn btn-primary', text: 'Създай резервация' });
  const progress = el('p', { className: 'muted', id: 'cr-progress', hidden: true, text: 'Изпращане…' });

  form.append(
    el('div', { className: 'field' }, [
      el('label', { text: 'Маса' }),
      el('p', { className: 'readonly', text: `Маса ${tableLabel} (id ${diningTableId})` }),
      tableIdHidden
    ]),
    el('div', { className: 'field' }, [
      el('label', { for: 'cr-start', text: 'Начало *' }),
      startInput,
      el('p', { className: 'hint', text: 'Всички часове са в локалната часова зона на ресторанта.' })
    ]),
    el('div', { className: 'field' }, [
      el('label', { for: 'cr-end', text: 'Край *' }),
      endInput
    ]),
    el('div', { className: 'field' }, [
      el('label', { for: 'cr-guests', text: 'Брой гости *' }),
      guestInput
    ]),
    el('div', { className: 'field' }, [
      el('label', { for: 'cr-notes', text: `Бележки (по избор, макс. ${NOTES_MAX})` }),
      notesInput
    ]),
    fieldError,
    progress,
    el('div', { className: 'actions' }, [
      el('button', { type: 'button', className: 'btn btn-ghost', text: 'Назад към търсене', onClick: () => navigate('#/availability') }),
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
      toast(`Резервацията е създадена: ${created.reservationNumber || created.id}`, 'success');
      navigate(`#/reservations/${created.id}`);
    } catch (e) {
      if (e && e.status === 409) {
        fieldError.textContent = e.message || 'Интервалът вече не е свободен.';
        fieldError.hidden = false;
        setBanner(e.message, 'error');
        openDialog({
          title: 'Конфликт на резервация',
          body: el('p', { text: e.message || 'Интервалът вече не е свободен. Потърсете отново свободни маси.' }),
          footer: el('div', { className: 'actions' }, [
            el('button', {
              type: 'button',
              className: 'btn btn-primary',
              text: 'Ново търсене',
              onClick: () => { closeDialog(); navigate('#/availability'); }
            }),
            el('button', { type: 'button', className: 'btn btn-ghost', text: 'Затвори', onClick: closeDialog })
          ]),
          openerEl: submitBtn
        });
      } else {
        handleError(e);
        fieldError.textContent = e.message || 'Грешка при създаване.';
        fieldError.hidden = false;
      }
    } finally {
      submitBtn.disabled = false;
      progress.hidden = true;
    }
  });
}

export async function renderEditForm(root, reservationId) {
  setPageMeta('Пренасрочване', 'Промяна на бъдеща потвърдена резервация.');
  setBanner('');
  clear(root);
  root.appendChild(loadingBox());

  let reservation;
  try {
    reservation = await api.get(`/api/client/reservations/${reservationId}`);
  } catch (e) {
    clear(root);
    if (e && e.status === 404) {
      root.appendChild(errorBox('Резервацията не е намерена.', () => navigate('#/reservations')));
    } else {
      root.appendChild(errorBox(e.message || 'Грешка.', () => renderEditForm(root, reservationId)));
      handleError(e);
    }
    return;
  }

  clear(root);
  if (reservation.status !== 'CONFIRMED') {
    root.appendChild(errorBox('Редакцията е достъпна само за потвърдени резервации.', () => navigate(`#/reservations/${reservationId}`)));
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
  const submitBtn = el('button', { type: 'submit', className: 'btn btn-primary', text: 'Запази промените' });
  const progress = el('p', { className: 'muted', hidden: true, text: 'Запазване…' });

  form.append(
    el('div', { className: 'field' }, [
      el('label', { text: 'Номер' }),
      el('p', { className: 'readonly', text: text(reservation.reservationNumber) })
    ]),
    el('div', { className: 'field' }, [
      el('label', { text: 'Статус' }),
      badge(reservation.status)
    ]),
    el('div', { className: 'field' }, [
      el('label', { for: 'ed-table-id', text: 'ID на маса *' }),
      tableIdInput,
      el('p', { className: 'hint', text: `Текуща: маса ${text(reservation.tableNumber)} — ${text(reservation.tableDisplayName)}` })
    ]),
    el('div', { className: 'field' }, [
      el('label', { for: 'ed-start', text: 'Начало *' }),
      startInput,
      el('p', { className: 'hint', text: 'Всички часове са в локалната часова зона на ресторанта.' })
    ]),
    el('div', { className: 'field' }, [
      el('label', { for: 'ed-end', text: 'Край *' }),
      endInput
    ]),
    el('div', { className: 'field' }, [
      el('label', { for: 'ed-guests', text: 'Брой гости *' }),
      guestInput
    ]),
    el('div', { className: 'field' }, [
      el('label', { for: 'ed-notes', text: `Бележки (макс. ${NOTES_MAX})` }),
      notesInput
    ]),
    fieldError,
    progress,
    el('div', { className: 'actions' }, [
      el('button', {
        type: 'button',
        className: 'btn btn-ghost',
        text: 'Към търсене на маси',
        onClick: () => navigate('#/availability')
      }),
      el('button', {
        type: 'button',
        className: 'btn btn-ghost',
        text: 'Отказ',
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
      toast('Резервацията е обновена.', 'success');
      navigate(`#/reservations/${reservationId}`);
    } catch (e) {
      handleError(e);
      fieldError.textContent = e.message || 'Грешка при обновяване.';
      fieldError.hidden = false;
    } finally {
      submitBtn.disabled = false;
      progress.hidden = true;
    }
  });
}