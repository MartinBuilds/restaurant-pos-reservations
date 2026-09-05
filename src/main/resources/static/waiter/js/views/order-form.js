import { api, ApiClientError } from '/operations/js/api.js';
import { el, clear } from '/operations/js/dom.js';
import { money, text } from '/operations/js/format.js';
import { handleError, toast } from '/operations/js/notifications.js';
import { closeDialog, openDialog } from './ui-shared.js';
import { t } from '/shared/js/i18n/i18n.js?v=pr17-4';

async function loadMenu() {
  const items = await api.get('/api/public/menu');
  return (items || []).filter((i) => i.available);
}

function buildMenuPicker(menuItems, quantities) {
  const wrap = el('div', { className: 'menu-pick' });
  if (!menuItems.length) {
    wrap.appendChild(el('p', { className: 'muted', text: t('common.empty') }));
    return wrap;
  }
  const byCat = new Map();
  menuItems.forEach((item) => {
    const key = item.categoryName || t('menu.uncategorized');
    if (!byCat.has(key)) byCat.set(key, []);
    byCat.get(key).push(item);
  });
  byCat.forEach((list, cat) => {
    wrap.appendChild(el('h3', { text: cat }));
    list.forEach((item) => {
      const qty = el('input', {
        type: 'number', min: '0', step: '1', value: String(quantities.get(item.id) || 0),
        'aria-label': t('orders.qtyAria', { name: item.name })
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
    handleError(err, t('orders.menuLoadError'));
    return;
  }

  const body = el('div', { className: 'stack' }, [
    el('p', {
      text: table.displayName
        ? t('label.tableNamed', { number: table.tableNumber, name: table.displayName })
        : t('label.table', { number: table.tableNumber })
    }),
    buildMenuPicker(menuItems, quantities)
  ]);

  const submitBtn = el('button', { type: 'button', className: 'btn btn-primary', text: t('action.newOrder') });
  submitBtn.addEventListener('click', async () => {
    if (submitting) return;
    const items = selectedItems(quantities);
    if (!items.length) {
      toast(t('msg.selectItem'), 'error');
      return;
    }
    submitting = true;
    submitBtn.disabled = true;
    try {
      await api.post('/api/waiter/orders', {
        diningTableId: table.id,
        items
      });
      toast(t('msg.orderCreated'), 'success');
      closeDialog();
      if (onDone) await onDone();
    } catch (err) {
      if (err instanceof ApiClientError && err.status === 409) {
        handleError(err);
        try {
          menuItems = await loadMenu();
          clear(body);
          body.appendChild(el('p', { text: t('label.table', { number: table.tableNumber }) }));
          body.appendChild(buildMenuPicker(menuItems, quantities));
        } catch { /* ignore */ }
        if (onDone) await onDone();
      } else {
        handleError(err, t('orders.createError'));
      }
    } finally {
      submitting = false;
      submitBtn.disabled = false;
    }
  });

  openDialog({
    title: t('action.newOrder'),
    body,
    footer: el('div', { className: 'actions' }, [
      el('button', { type: 'button', className: 'btn', onClick: () => closeDialog(), text: t('common.cancel') }),
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
    handleError(err, t('orders.menuLoadError'));
    return;
  }

  const body = el('div', { className: 'stack' }, [
    el('p', { text: t('label.order', { number: order.orderNumber }) }),
    buildMenuPicker(menuItems, quantities)
  ]);

  const submitBtn = el('button', { type: 'button', className: 'btn btn-primary', text: t('action.addItems') });
  submitBtn.addEventListener('click', async () => {
    if (submitting) return;
    const items = selectedItems(quantities);
    if (!items.length) {
      toast(t('msg.selectItem'), 'error');
      return;
    }
    submitting = true;
    submitBtn.disabled = true;
    try {
      await api.post(`/api/waiter/orders/${order.id}/items`, { items });
      toast(t('msg.itemsAdded'), 'success');
      closeDialog();
      if (onDone) await onDone();
    } catch (err) {
      if (err instanceof ApiClientError && err.status === 409) {
        handleError(err);
        try {
          menuItems = await loadMenu();
          clear(body);
          body.appendChild(el('p', { text: t('label.order', { number: order.orderNumber }) }));
          body.appendChild(buildMenuPicker(menuItems, quantities));
        } catch { /* ignore */ }
        if (onDone) await onDone();
      } else {
        handleError(err, t('orders.addItemsError'));
      }
    } finally {
      submitting = false;
      submitBtn.disabled = false;
    }
  });

  openDialog({
    title: t('action.addItems'),
    body,
    footer: el('div', { className: 'actions' }, [
      el('button', { type: 'button', className: 'btn', onClick: () => closeDialog(), text: t('common.cancel') }),
      submitBtn
    ]),
    openerEl
  });
}
