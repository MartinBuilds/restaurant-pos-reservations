import { api } from '/operations/js/api.js';
import { clear, el } from '/operations/js/dom.js';
import { text } from '/operations/js/format.js';
import { handleError, setBanner } from '/operations/js/notifications.js';
import { openCreateOrderDialog } from './order-form.js';
import { badge, emptyBox, errorBox, loadingBox, setPageMeta } from './ui-shared.js';
import { t, statusLabel } from '/shared/js/i18n/i18n.js?v=fix-refresh-1';
import { setPageRefresh } from '/shared/js/page-refresh.js?v=fix-refresh-4';

let abort = null;

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
    const grid = el('div', { className: 'grid grid-cards' });
    tables.forEach((table) => {
      const canCreate = table.status === 'AVAILABLE';
      const actions = el('div', { className: 'actions' });
      if (canCreate) {
        const btn = el('button', {
          type: 'button',
          className: 'btn btn-primary',
          text: t('action.newOrder')
        });
        btn.addEventListener('click', () => {
          openCreateOrderDialog(table, {
            openerEl: btn,
            onDone: () => renderTables()
          });
        });
        actions.appendChild(btn);
      } else {
        actions.appendChild(el('span', {
          className: 'muted',
          text: t('tables.orderRequiresAvailable', { status: statusLabel('AVAILABLE') })
        }));
      }
      grid.appendChild(el('article', { className: 'card' }, [
        el('h3', { text: t('label.table', { number: text(table.tableNumber) }) }),
        el('p', { text: text(table.displayName) }),
        el('p', { className: 'muted', text: t('label.capacity', { n: text(table.capacity) }) }),
        badge(table.status),
        el('p', { className: 'muted', text: table.active ? t('common.active') : t('common.inactive') }),
        actions
      ]));
    });
    content.appendChild(grid);
  } catch (err) {
    if (err && err.name === 'AbortError') return;
    clear(content);
    handleError(err, t('tables.loadError'));
    content.appendChild(errorBox(err.message || t('common.error'), () => renderTables()));
  }
}
