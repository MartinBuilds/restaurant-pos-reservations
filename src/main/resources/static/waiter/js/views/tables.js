import { api } from '/operations/js/api.js';
import { clear, el } from '/operations/js/dom.js';
import { text } from '/operations/js/format.js';
import { handleError, setBanner } from '/operations/js/notifications.js';
import { openCreateOrderDialog } from './order-form.js';
import { badge, emptyBox, errorBox, loadingBox, setPageMeta } from './ui-shared.js';
import { t, statusLabel } from '/shared/js/i18n/i18n.js?v=fix-table-live-1';
import { setPageRefresh } from '/shared/js/page-refresh.js?v=fix-refresh-4';

let abort = null;

function statusClass(status) {
  switch (status) {
    case 'AVAILABLE': return 'is-available';
    case 'OCCUPIED': return 'is-occupied';
    case 'RESERVED': return 'is-reserved';
    case 'OUT_OF_SERVICE': return 'is-oos';
    default: return 'is-muted';
  }
}

function countByStatus(tables, status) {
  return tables.filter((table) => table.status === status && table.active).length;
}

function buildSummary(tables) {
  const available = countByStatus(tables, 'AVAILABLE');
  const occupied = countByStatus(tables, 'OCCUPIED');
  const reserved = countByStatus(tables, 'RESERVED');
  return el('div', { className: 'tables-summary', role: 'status' }, [
    el('span', { className: 'tables-summary-item is-available', text: t('tables.summaryAvailable', { n: available }) }),
    el('span', { className: 'tables-summary-item is-occupied', text: t('tables.summaryOccupied', { n: occupied }) }),
    el('span', { className: 'tables-summary-item is-reserved', text: t('tables.summaryReserved', { n: reserved }) })
  ]);
}

function buildTile(table, onReload) {
  const canCreate = (table.status === 'AVAILABLE' || table.status === 'RESERVED') && table.active;
  const actions = el('div', { className: 'table-tile-actions' });

  if (canCreate) {
    const btn = el('button', {
      type: 'button',
      className: 'btn btn-primary table-tile-cta',
      text: t('action.newOrder')
    });
    btn.addEventListener('click', () => {
      openCreateOrderDialog(table, {
        openerEl: btn,
        onDone: () => onReload()
      });
    });
    actions.appendChild(btn);
  } else {
    actions.appendChild(el('p', {
      className: 'table-tile-hint muted',
      text: table.active
        ? t('tables.orderRequiresFreeOrReserved', {
          available: statusLabel('AVAILABLE'),
          reserved: statusLabel('RESERVED')
        })
        : t('common.inactive')
    }));
  }

  return el('article', {
    className: `table-tile ${statusClass(table.status)}${table.active ? '' : ' is-inactive'}`,
    'data-status': table.status
  }, [
    el('div', { className: 'table-tile-num', text: text(table.tableNumber) }),
    el('div', { className: 'table-tile-body' }, [
      el('div', { className: 'table-tile-top' }, [
        el('h3', { className: 'table-tile-title', text: t('label.table', { number: text(table.tableNumber) }) }),
        badge(table.status)
      ]),
      el('p', { className: 'table-tile-name', text: text(table.displayName) || '—' }),
      el('p', { className: 'table-tile-meta muted', text: t('tables.seats', { n: text(table.capacity) }) })
    ]),
    actions
  ]);
}

export async function renderTables({ soft = false } = {}) {
  setPageRefresh(() => renderTables({ soft: true }));
  setPageMeta(t('page.tables.title'), t('page.tables.waiterSubtitle'));
  const content = document.getElementById('content');
  if (!soft) {
    clear(content);
    content.appendChild(loadingBox());
  }

  if (abort) abort.abort();
  abort = new AbortController();
  try {
    const tables = await api.get('/api/waiter/tables', { signal: abort.signal });
    clear(content);
    setBanner('');
    if (!tables || !tables.length) {
      content.appendChild(emptyBox(t('msg.tablesEmpty')));
      return;
    }

    const board = el('div', { className: 'tables-board' });
    board.appendChild(buildSummary(tables));

    const floor = el('div', { className: 'tables-floor' });
    const sorted = [...tables].sort((a, b) => Number(a.tableNumber) - Number(b.tableNumber));
    sorted.forEach((table) => {
      floor.appendChild(buildTile(table, () => renderTables({ soft: true })));
    });
    board.appendChild(floor);
    content.appendChild(board);
  } catch (err) {
    if (err && err.name === 'AbortError') return;
    clear(content);
    handleError(err, t('tables.loadError'));
    content.appendChild(errorBox(err.message || t('common.error'), () => renderTables()));
  }
}
