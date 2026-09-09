import { api, queryString } from '../api.js';
import { dateTime, toDateTimeLocalValue, fromDateTimeLocalValue } from '../format.js';
import {
  setPageMeta, mount, el, panel, table, badge, loading, errorBox, emptyState,
  openDialog, closeDialog, toast, toastIfUnchanged, handleError, field, confirmDialog,
  reloadButton, setPageRefresh
} from '../ui.js';
import { t, statusLabel } from '/shared/js/i18n/i18n.js?v=fix-datetime-1';
import { createDatetimePicker } from '/shared/js/datetime-picker.js?v=fix-datetime-2';

const STATUSES = ['CONFIRMED', 'CANCELLED', 'COMPLETED', 'NO_SHOW'];
const TERMINAL = new Set(['CANCELLED', 'COMPLETED', 'NO_SHOW']);

function statusBadge(status) {
  const map = { CONFIRMED: 'ok', CANCELLED: 'muted', COMPLETED: 'info', NO_SHOW: 'warn' };
  if (!status) return badge('—', 'muted');
  return badge(statusLabel(status) || '—', map[status] || 'muted');
}

function dtPicker(value = '') {
  return createDatetimePicker({ value: toDateTimeLocalValue(value) });
}

export async function renderReservations() {
  setPageMeta(t('page.reservations.title'), t('page.reservations.subtitle'));
  mount(loading(t('common.loading')));
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

    const fromInput = dtPicker(filters.from);
    const toInput = dtPicker(filters.to);
    const status = el('select', {}, [
      el('option', { value: '', text: t('filter.allStatuses') }),
      ...STATUSES.map((s) => el('option', { value: s, text: statusLabel(s), selected: filters.status === s ? 'true' : null }))
    ]);
    const tableId = el('select', {}, [
      el('option', { value: '', text: t('filter.allTables') }),
      ...tables.map((row) => el('option', {
        value: String(row.id),
        text: `#${row.tableNumber} ${row.displayName || ''}`.trim(),
        selected: String(filters.tableId || '') === String(row.id) ? 'true' : null
      }))
    ]);
    const clientId = el('select', {}, [
      el('option', { value: '', text: t('filter.allClients') }),
      ...users.filter((u) => (u.roles || []).includes('CLIENT')).map((u) => el('option', {
        value: String(u.id),
        text: `${u.fullName} (${u.email})`,
        selected: String(filters.clientId || '') === String(u.id) ? 'true' : null
      }))
    ]);

    const apply = el('button', {
      type: 'button', className: 'btn', text: t('common.search'),
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
          type: 'button', className: 'btn btn-info', text: t('common.edit'),
          onClick: () => openEditDialog(r, tables, () => reload(filters))
        }),
        el('button', {
          type: 'button', className: 'btn btn-warn', text: t('action.status'),
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
      panel(t('panel.filters'), [
        el('div', { className: 'filters' }, [
          field(t('col.from'), fromInput),
          field(t('col.to'), toInput),
          field(t('col.status'), status),
          field(t('col.table'), tableId),
          field(t('col.client'), clientId),
          apply
        ])
      ], [
        el('button', {
          type: 'button', className: 'btn', text: t('action.createReservation'),
          onClick: () => openCreateDialog(tables, users, () => reload(filters))
        }),
        reloadButton(() => reload(filters))
      ]),
      panel(t('panel.list'), [
        reservations.length
          ? table(t('page.reservations.title'), [t('col.number'), t('col.table'), t('col.client'), t('col.start'), t('col.end'), t('msg.guests'), t('col.status'), t('col.notes'), t('common.actions')], rows)
          : emptyState(t('msg.noResults'))
      ]),
      panel(t('panel.schedule'), [
        filters.from && filters.to
          ? (scheduleRows.length
            ? table(t('panel.schedule'), [t('col.number'), t('col.table'), t('col.client'), t('col.start'), t('col.end'), t('msg.guests'), t('col.status')], scheduleRows)
            : emptyState(t('msg.noResults')))
          : el('p', { className: 'muted', text: t('reservations.scheduleHint') })
      ])
    ]));
    setPageRefresh(() => reload(filters));
  } catch (err) {
    mount(errorBox(handleError(err), () => reload(filters)));
  }
}

function openCreateDialog(tables, users, onDone) {
  const clients = users.filter((u) => (u.roles || []).includes('CLIENT'));
  const clientId = el('select', {}, [
    el('option', { value: '', text: t('col.client') }),
    ...clients.map((u) => el('option', { value: String(u.id), text: `${u.fullName} (${u.email})` }))
  ]);
  const diningTableId = el('select', {}, [
    el('option', { value: '', text: t('col.table') }),
    ...tables.filter((row) => row.active).map((row) => el('option', {
      value: String(row.id),
      text: `#${row.tableNumber} ${row.displayName || ''}`.trim()
    }))
  ]);
  const startTime = dtPicker();
  const endTime = dtPicker();
  const guestCount = el('input', { type: 'number', min: '1', value: '2' });
  const notes = el('textarea');
  const submit = el('button', { type: 'button', className: 'btn', text: t('common.create') });
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
      toast(t('msg.reservationCreated'), 'success');
      await onDone();
    } catch (err) {
      handleError(err);
      submit.disabled = false;
    }
  });
  openDialog({
    title: t('action.createReservation'),
    body: el('div', { className: 'stack' }, [
      field(t('col.client'), clientId),
      field(t('col.table'), diningTableId),
      field(t('col.start'), startTime),
      field(t('col.end'), endTime),
      field(t('msg.guests'), guestCount),
      field(t('col.notes'), notes)
    ]),
    footerButtons: [
      el('button', { type: 'button', className: 'btn btn-secondary', text: t('common.cancel'), onClick: () => closeDialog() }),
      submit
    ]
  });
}

function openEditDialog(reservation, tables, onDone) {
  const diningTableId = el('select', {}, tables.map((row) => el('option', {
    value: String(row.id),
    text: `#${row.tableNumber}`,
    selected: reservation.diningTableId === row.id ? 'true' : null
  })));
  const startTime = dtPicker(reservation.startTime);
  const endTime = dtPicker(reservation.endTime);
  const guestCount = el('input', { type: 'number', min: '1', value: reservation.guestCount ?? 1 });
  const notes = el('textarea', {}, reservation.notes || '');
  const submit = el('button', { type: 'button', className: 'btn', text: t('common.save') });
  submit.addEventListener('click', async () => {
    const body = {
      diningTableId: Number(diningTableId.value),
      startTime: fromDateTimeLocalValue(startTime.value),
      endTime: fromDateTimeLocalValue(endTime.value),
      guestCount: Number(guestCount.value),
      notes: notes.value.trim() || null
    };
    const baseline = {
      diningTableId: Number(reservation.diningTableId),
      startTime: fromDateTimeLocalValue(toDateTimeLocalValue(reservation.startTime)),
      endTime: fromDateTimeLocalValue(toDateTimeLocalValue(reservation.endTime)),
      guestCount: Number(reservation.guestCount ?? 1),
      notes: reservation.notes || null
    };
    if (toastIfUnchanged(baseline, body, t('msg.noChanges'))) return;
    submit.disabled = true;
    try {
      await api.put(`/api/admin/reservations/${reservation.id}`, body);
      closeDialog();
      toast(t('msg.reservationUpdated'), 'success');
      await onDone();
    } catch (err) {
      handleError(err);
      submit.disabled = false;
    }
  });
  openDialog({
    title: t('reservations.editTitle', { number: reservation.reservationNumber }),
    body: el('div', { className: 'stack' }, [
      field(t('col.table'), diningTableId),
      field(t('col.start'), startTime),
      field(t('col.end'), endTime),
      field(t('msg.guests'), guestCount),
      field(t('col.notes'), notes)
    ]),
    footerButtons: [
      el('button', { type: 'button', className: 'btn btn-secondary', text: t('common.cancel'), onClick: () => closeDialog() }),
      submit
    ]
  });
}

function openStatusDialog(reservation, onDone) {
  const status = el('select', {}, STATUSES.map((s) => el('option', {
    value: s, text: statusLabel(s), selected: reservation.status === s ? 'true' : null
  })));
  const submit = el('button', { type: 'button', className: 'btn', text: t('common.save') });
  submit.addEventListener('click', async () => {
    if (status.value === reservation.status) {
      toast(t('msg.noChanges'), 'info');
      return;
    }
    if (TERMINAL.has(status.value)) {
      const ok = await confirmDialog({
        title: t('reservations.terminalTitle'),
        message: t('reservations.terminalMsg', { status: statusLabel(status.value) }),
        confirmLabel: t('common.confirm'),
        danger: true
      });
      if (!ok) return;
    }
    submit.disabled = true;
    try {
      await api.patch(`/api/admin/reservations/${reservation.id}/status`, { status: status.value });
      closeDialog();
      toast(t('msg.statusUpdated'), 'success');
      await onDone();
    } catch (err) {
      handleError(err);
      submit.disabled = false;
    }
  });
  openDialog({
    title: t('reservations.statusTitle', { number: reservation.reservationNumber }),
    body: field(t('col.status'), status),
    footerButtons: [
      el('button', { type: 'button', className: 'btn btn-secondary', text: t('common.cancel'), onClick: () => closeDialog() }),
      submit
    ]
  });
}
