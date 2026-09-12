import { api } from '../api.js';
import {
  setPageMeta, mount, el, panel, table, badge, loading, errorBox, emptyState,
  openDialog, closeDialog, toast, toastIfUnchanged, handleError, field, confirmDialog,
  reloadButton, setPageRefresh
} from '../ui.js';
import { t, statusLabel } from '/shared/js/i18n/i18n.js?v=fix-refresh-1';

const STATUSES = ['AVAILABLE', 'OCCUPIED', 'RESERVED', 'OUT_OF_SERVICE'];

function statusBadge(status) {
  const map = {
    AVAILABLE: 'ok',
    OCCUPIED: 'warn',
    RESERVED: 'info',
    OUT_OF_SERVICE: 'danger'
  };
  if (!status) return badge('—', 'muted');
  return badge(statusLabel(status) || '—', map[status] || 'muted');
}

export async function renderTables() {
  setPageMeta(t('page.tables.title'), t('page.tables.subtitle'));
  mount(loading(t('common.loading')));
  await reload();
}

async function reload() {
  try {
    const tables = await api.get('/api/admin/tables');
    const rows = tables.map((row) => [
      String(row.id),
      String(row.tableNumber),
      row.displayName || '—',
      String(row.capacity),
      statusBadge(row.status),
      badge(row.active ? t('common.active') : t('common.inactive'), row.active ? 'ok' : 'muted'),
      el('div', { className: 'row-actions' }, [
        el('button', { type: 'button', className: 'btn btn-info', text: t('common.edit'), onClick: () => openTableDialog(row, () => reload()) }),
        el('button', { type: 'button', className: 'btn btn-warn', text: t('action.status'), onClick: () => openStatusDialog(row, () => reload()) }),
        el('button', {
          type: 'button', className: `btn ${row.active ? 'btn-danger' : 'btn-ok'}`, text: row.active ? t('action.disable') : t('action.enable'),
          onClick: async () => {
            if (row.active) {
              const ok = await confirmDialog({
                title: t('tables.deactivateTitle'),
                message: t('tables.deactivateMsg'),
                confirmLabel: t('common.confirm'),
                danger: true
              });
              if (!ok) return;
            }
            try {
              await api.patch(`/api/admin/tables/${row.id}/active`, { active: !row.active });
              toast(t('msg.statusUpdated'), 'success');
              await reload();
            } catch (err) { handleError(err); }
          }
        })
      ])
    ]);

    mount(panel(t('page.tables.title'), [
      tables.length
        ? table(t('page.tables.title'), [t('col.id'), t('col.number'), t('col.name'), t('col.capacity'), t('col.status'), t('col.activeFem'), t('common.actions')], rows)
        : emptyState(t('msg.tablesEmpty'))
    ], [
      el('button', { type: 'button', className: 'btn', text: t('action.newTable'), onClick: () => openTableDialog(null, () => reload()) }),
      reloadButton(reload)
    ]));
    setPageRefresh(reload);
  } catch (err) {
    mount(errorBox(handleError(err), () => reload()));
  }
}

function openTableDialog(existing, onDone) {
  const tableNumber = el('input', { type: 'number', min: '1', value: existing?.tableNumber ?? '' });
  const displayName = el('input', { type: 'text', value: existing?.displayName || '' });
  const capacity = el('input', { type: 'number', min: '1', value: existing?.capacity ?? '' });
  const submit = el('button', { type: 'button', className: 'btn', text: existing ? t('common.save') : t('common.create') });
  submit.addEventListener('click', async () => {
    const body = {
      tableNumber: Number(tableNumber.value),
      displayName: displayName.value.trim() || null,
      capacity: Number(capacity.value)
    };
    if (existing) {
      const baseline = {
        tableNumber: Number(existing.tableNumber),
        displayName: existing.displayName || null,
        capacity: Number(existing.capacity)
      };
      if (toastIfUnchanged(baseline, body, t('msg.noChanges'))) return;
    }
    submit.disabled = true;
    try {
      if (existing) await api.put(`/api/admin/tables/${existing.id}`, body);
      else await api.post('/api/admin/tables', body);
      closeDialog();
      toast(existing ? t('msg.saved') : t('msg.created'), 'success');
      await onDone();
    } catch (err) {
      handleError(err);
      submit.disabled = false;
    }
  });
  openDialog({
    title: existing ? t('tables.edit') : t('tables.new'),
    body: el('div', { className: 'stack' }, [
      field(t('col.number'), tableNumber),
      field(t('col.name'), displayName),
      field(t('col.capacity'), capacity)
    ]),
    footerButtons: [
      el('button', { type: 'button', className: 'btn btn-secondary', text: t('common.cancel'), onClick: () => closeDialog() }),
      submit
    ]
  });
}

function openStatusDialog(tableRow, onDone) {
  const status = el('select', {}, STATUSES.map((s) => el('option', {
    value: s, text: statusLabel(s), selected: tableRow.status === s ? 'true' : null
  })));
  const submit = el('button', { type: 'button', className: 'btn', text: t('common.save') });
  submit.addEventListener('click', async () => {
    if (status.value === tableRow.status) {
      toast(t('msg.noChanges'), 'info');
      return;
    }
    if (status.value === 'OUT_OF_SERVICE') {
      const ok = await confirmDialog({
        title: statusLabel('OUT_OF_SERVICE'),
        message: t('tables.confirmOutOfService'),
        confirmLabel: t('common.confirm'),
        danger: true
      });
      if (!ok) return;
    }
    submit.disabled = true;
    try {
      await api.patch(`/api/admin/tables/${tableRow.id}/status`, { status: status.value });
      closeDialog();
      toast(t('msg.statusUpdated'), 'success');
      await onDone();
    } catch (err) {
      handleError(err);
      submit.disabled = false;
    }
  });
  openDialog({
    title: t('tables.statusTitle', { number: tableRow.tableNumber }),
    body: field(t('col.status'), status),
    footerButtons: [
      el('button', { type: 'button', className: 'btn btn-secondary', text: t('common.cancel'), onClick: () => closeDialog() }),
      submit
    ]
  });
}
