import { api } from '/operations/js/api.js';
import { clear, el } from '/operations/js/dom.js';
import { text } from '/operations/js/format.js';
import { handleError, setBanner } from '/operations/js/notifications.js';
import { openCreateOrderDialog } from './order-form.js';
import { badge, emptyBox, errorBox, loadingBox, setPageMeta } from './ui-shared.js';

let abort = null;

export async function renderTables() {
  setPageMeta('Маси', 'Активни маси и нова поръчка');
  const content = document.getElementById('content');
  clear(content);
  content.appendChild(loadingBox());

  if (abort) abort.abort();
  abort = new AbortController();
  try {
    const tables = await api.get('/api/waiter/tables', { signal: abort.signal });
    clear(content);
    setBanner('');
    if (!tables || !tables.length) {
      content.appendChild(emptyBox('Няма активни маси.'));
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
          text: 'Нова поръчка'
        });
        btn.addEventListener('click', () => {
          openCreateOrderDialog(table, {
            openerEl: btn,
            onDone: () => renderTables()
          });
        });
        actions.appendChild(btn);
      } else {
        actions.appendChild(el('span', { className: 'muted', text: 'Нова поръчка: само за AVAILABLE' }));
      }
      grid.appendChild(el('article', { className: 'card' }, [
        el('h3', { text: `Маса ${text(table.tableNumber)}` }),
        el('p', { text: text(table.displayName) }),
        el('p', { className: 'muted', text: `Капацитет: ${text(table.capacity)}` }),
        badge(table.status),
        el('p', { className: 'muted', text: table.active ? 'Активна' : 'Неактивна' }),
        actions
      ]));
    });
    content.appendChild(grid);
  } catch (err) {
    if (err && err.name === 'AbortError') return;
    clear(content);
    handleError(err, 'Масите не можаха да се заредят.');
    content.appendChild(errorBox(err.message || 'Грешка', () => renderTables()));
  }
}