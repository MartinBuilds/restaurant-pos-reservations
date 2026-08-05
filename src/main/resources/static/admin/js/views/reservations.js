import { api, queryString } from '../api.js';
import { dateTime, toDateTimeLocalValue, fromDateTimeLocalValue } from '../format.js';
import {
  setPageMeta, mount, el, panel, table, badge, loading, errorBox, emptyState,
  openDialog, closeDialog, toast, handleError, field, confirmDialog
} from '../ui.js';

const STATUSES = ['CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW'];
const TERMINAL = new Set(['CANCELLED', 'COMPLETED', 'NO_SHOW']);

function statusBadge(status) {
  const map = { CONFIRMED: 'ok', CANCELLED: 'muted', COMPLETED: 'info', NO_SHOW: 'warn' };
  return badge(status || '—', map[status] || 'muted');
}

export async function renderReservations() {
  setPageMeta('Резервации', 'Списък, график и статуси · LocalDateTime в часовата зона на ресторанта (Europe/Sofia)');
  mount(loading());
  await reload();
}

async function reload(filters = {}) {
  try {
    const [tables, users, reservations, schedule] = await Promise.all([
      api.get('/api/admin/tables'),
      api.get('/api/admin/users'),
      api.get(`/api/admin/reservations${queryString(filters)}`),
      filters.from && filters.to
        ? api.get(`/api/admin/reservations/schedule${queryString({
          from: filters.from, to: filters.to, tableId: filters.tableId, status: filters.status
        })}`)
        : Promise.resolve([])
    ]);

    const fromInput = el('input', { type: 'datetime-local', value: toDateTimeLocalValue(filters.from) });
    const toInput = el('input', { type: 'datetime-local', value: toDateTimeLocalValue(filters.to) });
    const status = el('select', {}, [
      el('option', { value: '', text: 'Всички статуси' }),
      ...STATUSES.map((s) => el('option', { value: s, text: s, selected: filters.status === s ? 'true' : null }))
    ]);
    const tableId = el('select', {}, [
      el('option', { value: '', text: 'Всички маси' }),
      ...tables.map((t) => el('option', {
        value: String(t.id),
        text: `#${t.tableNumber} ${t.displayName || ''}`.trim(),
        selected: String(filters.tableId || '') === String(t.id) ? 'true' : null
      }))
    ]);
    const clientId = el('select', {}, [
      el('option', { value: '', text: 'Всички клиенти' }),
      ...users.filter((u) => (u.roles || []).includes('CLIENT')).map((u) => el('option', {
        value: String(u.id),
        text: `${u.fullName} (${u.email})`,
        selected: String(filters.clientId || '') === String(u.id) ? 'true' : null
      }))
    ]);

    const apply = el('button', {
      type: 'button', className: 'btn', text: 'Филтрирай',
      onClick: () => reload({
        from: fromDateTimeLocalValue(fromInput.value),
        to: fromDateTimeLocalValue(toInput.value),
        status: status.value || undefined,
        tableId: tableId.value || undefined,
        clientId: clientId.value || undefined
      })
    });

    const rows = reservations.map((r) => [
      r.reservationNumber || String(r.id),
      r.tableDisplayName || `#${r.tableNumber}`,
      r.clientName || String(r.clientId),
      dateTime(r.startTime),
      dateTime(r.endTime),
      String(r.guestCount ?? '—'),
      statusBadge(r.status),
      r.notes || '—',
      el('div', { className: 'row-actions' }, [
        el('button', {
          type: 'button', className: 'btn btn-secondary', text: 'Редакция',
          onClick: () => openEditDialog(r, tables, () => reload(filters))
        }),
        el('button', {
          type: 'button', className: 'btn btn-secondary', text: 'Статус',
          onClick: () => openStatusDialog(r, () => reload(filters))
        })
      ])
    ]);

    const scheduleRows = (schedule || []).map((s) => [
      s.reservationNumber || String(s.reservationId),
      String(s.tableNumber),
      s.clientName || String(s.clientId),
      dateTime(s.startTime),
      dateTime(s.endTime),
      String(s.guestCount ?? '—'),
      statusBadge(s.status)
    ]);

    mount(el('div', { className: 'stack' }, [
      panel('Филтри', [
        el('p', { className: 'note-info note', text: 'Изпращайте LocalDateTime без Z/offset. Часова зона на ресторанта: Europe/Sofia.' }),
        el('div', { className: 'filters' }, [
          field('От', fromInput),
          field('До', toInput),
          field('Статус', status),
          field('Маса', tableId),
          field('Клиент', clientId),
          apply
        ])
      ], [
        el('button', {
          type: 'button', className: 'btn', text: 'Нова резервация',
          onClick: () => openCreateDialog(tables, users, () => reload(filters))
        }),
        el('button', { type: 'button', className: 'btn btn-secondary', text: 'Презареди', onClick: () => reload(filters) })
      ]),
      panel('Списък', [
        reservations.length
          ? table('Резервации', ['Номер', 'Маса', 'Клиент', 'Начало', 'Край', 'Гости', 'Статус', 'Бележки', 'Действия'], rows)
          : emptyState('Няма резервации за избраните филтри.')
      ]),
      panel('График', [
        filters.from && filters.to
          ? (scheduleRows.length
            ? table('График', ['Номер', 'Маса', 'Клиент', 'Начало', 'Край', 'Гости', 'Статус'], scheduleRows)
            : emptyState('Няма записи в графика за периода.'))
          : el('p', { className: 'muted', text: 'Задайте from/to филтри, за да заредите /schedule.' })
      ])
    ]));
  } catch (err) {
    mount(errorBox(handleError(err), () => reload(filters)));
  }
}

function openCreateDialog(tables, users, onDone) {
  const clients = users.filter((u) => (u.roles || []).includes('CLIENT'));
  const clientId = el('select', {}, [
    el('option', { value: '', text: 'Клиент' }),
    ...clients.map((u) => el('option', { value: String(u.id), text: `${u.fullName} (${u.email})` }))
  ]);
  const diningTableId = el('select', {}, [
    el('option', { value: '', text: 'Маса' }),
    ...tables.filter((t) => t.active).map((t) => el('option', {
      value: String(t.id),
      text: `#${t.tableNumber} ${t.displayName || ''}`.trim()
    }))
  ]);
  const startTime = el('input', { type: 'datetime-local' });
  const endTime = el('input', { type: 'datetime-local' });
  const guestCount = el('input', { type: 'number', min: '1', value: '2' });
  const notes = el('textarea');
  const submit = el('button', { type: 'button', className: 'btn', text: 'Създай' });
  submit.addEventListener('click', async () => {
    submit.disabled = true;
    try {
      await api.post('/api/admin/reservations', {
        clientId: Number(clientId.value),
        diningTableId: Number(diningTableId.value),
        startTime: fromDateTimeLocalValue(startTime.value),
        endTime: fromDateTimeLocalValue(endTime.value),
        guestCount: Number(guestCount.value),
        notes: notes.value.trim() || null
      });
      closeDialog();
      toast('Резервацията е създадена.', 'success');
      await onDone();
    } catch (err) {
      handleError(err);
      submit.disabled = false;
    }
  });
  openDialog({
    title: 'Нова резервация',
    body: el('div', { className: 'stack' }, [
      field('Клиент', clientId),
      field('Маса', diningTableId),
      field('Начало', startTime),
      field('Край', endTime),
      field('Гости', guestCount),
      field('Бележки', notes)
    ]),
    footerButtons: [
      el('button', { type: 'button', className: 'btn btn-secondary', text: 'Отказ', onClick: () => closeDialog() }),
      submit
    ]
  });
}

function openEditDialog(reservation, tables, onDone) {
  const diningTableId = el('select', {}, tables.map((t) => el('option', {
    value: String(t.id),
    text: `#${t.tableNumber}`,
    selected: reservation.diningTableId === t.id ? 'true' : null
  })));
  const startTime = el('input', { type: 'datetime-local', value: toDateTimeLocalValue(reservation.startTime) });
  const endTime = el('input', { type: 'datetime-local', value: toDateTimeLocalValue(reservation.endTime) });
  const guestCount = el('input', { type: 'number', min: '1', value: reservation.guestCount ?? 1 });
  const notes = el('textarea', {}, reservation.notes || '');
  const submit = el('button', { type: 'button', className: 'btn', text: 'Запази' });
  submit.addEventListener('click', async () => {
    submit.disabled = true;
    try {
      await api.put(`/api/admin/reservations/${reservation.id}`, {
        diningTableId: Number(diningTableId.value),
        startTime: fromDateTimeLocalValue(startTime.value),
        endTime: fromDateTimeLocalValue(endTime.value),
        guestCount: Number(guestCount.value),
        notes: notes.value.trim() || null
      });
      closeDialog();
      toast('Резервацията е обновена.', 'success');
      await onDone();
    } catch (err) {
      handleError(err);
      submit.disabled = false;
    }
  });
  openDialog({
    title: `Редакция — ${reservation.reservationNumber}`,
    body: el('div', { className: 'stack' }, [
      field('Маса', diningTableId),
      field('Начало', startTime),
      field('Край', endTime),
      field('Гости', guestCount),
      field('Бележки', notes)
    ]),
    footerButtons: [
      el('button', { type: 'button', className: 'btn btn-secondary', text: 'Отказ', onClick: () => closeDialog() }),
      submit
    ]
  });
}

function openStatusDialog(reservation, onDone) {
  const status = el('select', {}, STATUSES.map((s) => el('option', {
    value: s, text: s, selected: reservation.status === s ? 'true' : null
  })));
  const submit = el('button', { type: 'button', className: 'btn', text: 'Запази статус' });
  submit.addEventListener('click', async () => {
    if (TERMINAL.has(status.value)) {
      const ok = await confirmDialog({
        title: 'Терминален статус',
        message: `Потвърдете преминаване към ${status.value}.`,
        confirmLabel: 'Потвърди',
        danger: true
      });
      if (!ok) return;
    }
    submit.disabled = true;
    try {
      await api.patch(`/api/admin/reservations/${reservation.id}/status`, { status: status.value });
      closeDialog();
      toast('Статусът е обновен.', 'success');
      await onDone();
    } catch (err) {
      handleError(err);
      submit.disabled = false;
    }
  });
  openDialog({
    title: `Статус — ${reservation.reservationNumber}`,
    body: field('Статус', status),
    footerButtons: [
      el('button', { type: 'button', className: 'btn btn-secondary', text: 'Отказ', onClick: () => closeDialog() }),
      submit
    ]
  });
}