import { api, ApiClientError } from '/operations/js/api.js';
import { el, clear } from '/operations/js/dom.js';
import { money, text } from '/operations/js/format.js';
import { handleError, toast } from '/operations/js/notifications.js';
import { closeDialog, openDialog } from './ui-shared.js';

async function loadMenu() {
  const items = await api.get('/api/public/menu');
  return (items || []).filter((i) => i.available);
}

function buildMenuPicker(menuItems, quantities) {
  const wrap = el('div', { className: 'menu-pick' });
  if (!menuItems.length) {
    wrap.appendChild(el('p', { className: 'muted', text: 'Няма налични артикули.' }));
    return wrap;
  }
  const byCat = new Map();
  menuItems.forEach((item) => {
    const key = item.categoryName || 'Други';
    if (!byCat.has(key)) byCat.set(key, []);
    byCat.get(key).push(item);
  });
  byCat.forEach((list, cat) => {
    wrap.appendChild(el('h3', { text: cat }));
    list.forEach((item) => {
      const qty = el('input', {
        type: 'number', min: '0', step: '1', value: String(quantities.get(item.id) || 0),
        'aria-label': `Количество за ${item.name}`
      });
      qty.addEventListener('change', () => {
        const n = Math.max(0, Math.floor(Number(qty.value) || 0));
        qty.value = String(n);
        if (n > 0) quantities.set(item.id, n);
        else quantities.delete(item.id);
      });
      wrap.appendChild(el('div', { className: 'menu-row' }, [
        el('div', {}, [
          el('strong', { text: text(item.name) }),
          el('div', { className: 'muted', text: money(item.price) })
        ]),
        qty
      ]));
    });
  });
  return wrap;
}

function selectedItems(quantities) {
  return [...quantities.entries()]
    .filter(([, q]) => q > 0)
    .map(([menuItemId, quantity]) => ({ menuItemId, quantity }));
}

export async function openCreateOrderDialog(table, { onDone, openerEl }) {
  const quantities = new Map();
  let submitting = false;
  let menuItems = [];
  try {
    menuItems = await loadMenu();
  } catch (err) {
    handleError(err, 'Менюто не можа да се зареди.');
    return;
  }

  const body = el('div', { className: 'stack' }, [
    el('p', { text: `Маса ${table.tableNumber}${table.displayName ? ` — ${table.displayName}` : ''}` }),
    buildMenuPicker(menuItems, quantities)
  ]);

  const submitBtn = el('button', { type: 'button', className: 'btn btn-primary', text: 'Създай поръчка' });
  submitBtn.addEventListener('click', async () => {
    if (submitting) return;
    const items = selectedItems(quantities);
    if (!items.length) {
      toast('Изберете поне един артикул.', 'error');
      return;
    }
    submitting = true;
    submitBtn.disabled = true;
    try {
      await api.post('/api/waiter/orders', {
        diningTableId: table.id,
        items
      });
      toast('Поръчката е създадена.', 'success');
      closeDialog();
      if (onDone) await onDone();
    } catch (err) {
      if (err instanceof ApiClientError && err.status === 409) {
        handleError(err);
        try {
          menuItems = await loadMenu();
          clear(body);
          body.appendChild(el('p', { text: `Маса ${table.tableNumber}` }));
          body.appendChild(buildMenuPicker(menuItems, quantities));
        } catch { /* ignore */ }
        if (onDone) await onDone();
      } else {
        handleError(err, 'Създаването на поръчка неуспешно.');
      }
    } finally {
      submitting = false;
      submitBtn.disabled = false;
    }
  });

  openDialog({
    title: 'Нова поръчка',
    body,
    footer: el('div', { className: 'actions' }, [
      el('button', { type: 'button', className: 'btn', onClick: () => closeDialog(), text: 'Отказ' }),
      submitBtn
    ]),
    openerEl
  });
}

export async function openAddItemsDialog(order, { onDone, openerEl }) {
  const quantities = new Map();
  let submitting = false;
  let menuItems = [];
  try {
    menuItems = await loadMenu();
  } catch (err) {
    handleError(err, 'Менюто не можа да се зареди.');
    return;
  }

  const body = el('div', { className: 'stack' }, [
    el('p', { text: `Поръчка ${order.orderNumber}` }),
    buildMenuPicker(menuItems, quantities)
  ]);

  const submitBtn = el('button', { type: 'button', className: 'btn btn-primary', text: 'Добави артикули' });
  submitBtn.addEventListener('click', async () => {
    if (submitting) return;
    const items = selectedItems(quantities);
    if (!items.length) {
      toast('Изберете поне един артикул.', 'error');
      return;
    }
    submitting = true;
    submitBtn.disabled = true;
    try {
      await api.post(`/api/waiter/orders/${order.id}/items`, { items });
      toast('Артикулите са добавени.', 'success');
      closeDialog();
      if (onDone) await onDone();
    } catch (err) {
      if (err instanceof ApiClientError && err.status === 409) {
        handleError(err);
        try {
          menuItems = await loadMenu();
          clear(body);
          body.appendChild(el('p', { text: `Поръчка ${order.orderNumber}` }));
          body.appendChild(buildMenuPicker(menuItems, quantities));
        } catch { /* ignore */ }
        if (onDone) await onDone();
      } else {
        handleError(err, 'Добавянето на артикули неуспешно.');
      }
    } finally {
      submitting = false;
      submitBtn.disabled = false;
    }
  });

  openDialog({
    title: 'Добавяне на артикули',
    body,
    footer: el('div', { className: 'actions' }, [
      el('button', { type: 'button', className: 'btn', onClick: () => closeDialog(), text: 'Отказ' }),
      submitBtn
    ]),
    openerEl
  });
}