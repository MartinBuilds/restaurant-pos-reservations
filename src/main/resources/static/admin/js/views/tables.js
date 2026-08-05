import { api } from '../api.js';
import {
  setPageMeta, mount, el, panel, table, badge, loading, errorBox, emptyState,
  openDialog, closeDialog, toast, handleError, field, confirmDialog
} from '../ui.js';

const STATUSES = ['AVAILABLE', 'OCCUPIED', 'RESERVED', 'OUT_OF_SERVICE'];

function statusBadge(status) {
  const map = {
    AVAILABLE: 'ok',
    OCCUPIED: 'warn',
    RESERVED: 'info',
    OUT_OF_SERVICE: 'danger'
  };
  return badge(status || '—', map[status] || 'muted');
}

export async function renderTables() {
  setPageMeta('Маси', 'Номера, капацитет, статус и активност');
  mount(loading());
  await reload();
}

async function reload() {
  try {
    const tables = await api.get('/api/admin/tables');
    const rows = tables.map((t) => [
      String(t.id),
      String(t.tableNumber),
      t.displayName || '—',
      String(t.capacity),
      statusBadge(t.status),
      badge(t.active ? 'Активна' : 'Неактивна', t.active ? 'ok' : 'muted'),
      el('div', { className: 'row-actions' }, [
        el('button', { type: 'button', className: 'btn btn-secondary', text: 'Редакция', onClick: () => openTableDialog(t, () => reload()) }),
        el('button', { type: 'button', className: 'btn btn-secondary', text: 'Статус', onClick: () => openStatusDialog(t, () => reload()) }),
        el('button', {
          type: 'button', className: 'btn btn-secondary', text: t.active ? 'Деактивирай' : 'Активирай',
          onClick: async () => {
            if (t.active) {
              const ok = await confirmDialog({
                title: 'Деактивиране на маса',
                message: 'Деактивирането може да бъде блокирано при отворена поръчка или бъдеща резервация.',
                confirmLabel: 'Деактивирай',
                danger: true
              });
              if (!ok) return;
            }
            try {
              await api.patch(`/api/admin/tables/${t.id}/active`, { active: !t.active });
              toast('Активността е обновена.', 'success');
              await reload();
            } catch (err) { handleError(err); }
          }
        })
      ])
    ]);

    mount(panel('Маси', [
      tables.length
        ? table('Маси', ['ID', 'Номер', 'Име', 'Капацитет', 'Статус', 'Активна', 'Действия'], rows)
        : emptyState('Няма маси.')
    ], [
      el('button', { type: 'button', className: 'btn', text: 'Нова маса', onClick: () => openTableDialog(null, () => reload()) }),
      el('button', { type: 'button', className: 'btn btn-secondary', text: 'Презареди', onClick: () => reload() })
    ]));
  } catch (err) {
    mount(errorBox(handleError(err), () => reload()));
  }
}

function openTableDialog(existing, onDone) {
  const tableNumber = el('input', { type: 'number', min: '1', value: existing?.tableNumber ?? '' });
  const displayName = el('input', { type: 'text', value: existing?.displayName || '' });
  const capacity = el('input', { type: 'number', min: '1', value: existing?.capacity ?? '' });
  const submit = el('button', { type: 'button', className: 'btn', text: existing ? 'Запази' : 'Създай' });
  submit.addEventListener('click', async () => {
    submit.disabled = true;
    try {
      const body = {
        tableNumber: Number(tableNumber.value),
        displayName: displayName.value.trim() || null,
        capacity: Number(capacity.value)
      };
      if (existing) await api.put(`/api/admin/tables/${existing.id}`, body);
      else await api.post('/api/admin/tables', body);
      closeDialog();
      toast('Масата е записана.', 'success');
      await onDone();
    } catch (err) {
      handleError(err);
      submit.disabled = false;
    }
  });
  openDialog({
    title: existing ? 'Редакция на маса' : 'Нова маса',
    body: el('div', { className: 'stack' }, [
      field('Номер', tableNumber),
      field('Име', displayName),
      field('Капацитет', capacity)
    ]),
    footerButtons: [
      el('button', { type: 'button', className: 'btn btn-secondary', text: 'Отказ', onClick: () => closeDialog() }),
      submit
    ]
  });
}

function openStatusDialog(tableRow, onDone) {
  const status = el('select', {}, STATUSES.map((s) => el('option', {
    value: s, text: s, selected: tableRow.status === s ? 'true' : null
  })));
  const submit = el('button', { type: 'button', className: 'btn', text: 'Запази статус' });
  submit.addEventListener('click', async () => {
    if (status.value === 'OUT_OF_SERVICE') {
      const ok = await confirmDialog({
        title: 'OUT_OF_SERVICE',
        message: 'Потвърдете преминаване към OUT_OF_SERVICE.',
        confirmLabel: 'Потвърди',
        danger: true
      });
      if (!ok) return;
    }
    submit.disabled = true;
    try {
      await api.patch(`/api/admin/tables/${tableRow.id}/status`, { status: status.value });
      closeDialog();
      toast('Статусът е обновен.', 'success');
      await onDone();
    } catch (err) {
      handleError(err);
      submit.disabled = false;
    }
  });
  openDialog({
    title: `Статус — маса ${tableRow.tableNumber}`,
    body: field('Статус', status),
    footerButtons: [
      el('button', { type: 'button', className: 'btn btn-secondary', text: 'Отказ', onClick: () => closeDialog() }),
      submit
    ]
  });
}