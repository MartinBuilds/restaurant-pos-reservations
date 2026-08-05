import { api } from '../api.js';
import { money, boolLabel } from '../format.js';
import {
  setPageMeta, mount, el, panel, table, badge, loading, errorBox, emptyState,
  openDialog, closeDialog, toast, handleError, field, confirmDialog
} from '../ui.js';

export async function renderMenu() {
  setPageMeta('Меню', 'Категории, ястия, активност и наличност');
  mount(loading());
  await reload();
}

async function reload() {
  try {
    const [categories, items] = await Promise.all([
      api.get('/api/admin/menu/categories'),
      api.get('/api/admin/menu/items')
    ]);

    const catRows = categories.map((c) => [
      String(c.id),
      c.name || '—',
      c.description || '—',
      badge(c.active ? 'Активна' : 'Неактивна', c.active ? 'ok' : 'muted'),
      el('div', { className: 'row-actions' }, [
        el('button', {
          type: 'button', className: 'btn btn-secondary', text: 'Редакция',
          onClick: () => openCategoryDialog(c, () => reload())
        }),
        el('button', {
          type: 'button', className: 'btn btn-secondary', text: c.active ? 'Деактивирай' : 'Активирай',
          onClick: async () => {
            try {
              await api.patch(`/api/admin/menu/categories/${c.id}/status`, { active: !c.active });
              toast('Статусът на категорията е обновен.', 'success');
              await reload();
            } catch (err) { handleError(err); }
          }
        })
      ])
    ]);

    const itemRows = items.map((item) => [
      String(item.id),
      item.name || '—',
      item.categoryName || String(item.categoryId),
      money(item.price),
      badge(item.active ? 'Активно' : 'Неактивно', item.active ? 'ok' : 'muted'),
      badge(item.manualAvailable ? 'Ръчно: да' : 'Ръчно: не', item.manualAvailable ? 'info' : 'warn'),
      badge(item.available ? 'Ефективно: да' : 'Ефективно: не', item.available ? 'ok' : 'danger'),
      item.availabilityReason || '—',
      el('div', { className: 'row-actions' }, [
        el('button', {
          type: 'button', className: 'btn btn-secondary', text: 'Редакция',
          onClick: () => openItemDialog(item, categories, () => reload())
        }),
        el('button', {
          type: 'button', className: 'btn btn-secondary', text: item.active ? 'Деактивирай' : 'Активирай',
          onClick: async () => {
            try {
              await api.patch(`/api/admin/menu/items/${item.id}/status`, { active: !item.active });
              toast('Статусът на ястието е обновен.', 'success');
              await reload();
            } catch (err) { handleError(err); }
          }
        }),
        el('button', {
          type: 'button', className: 'btn btn-secondary', text: 'Ръчна наличност',
          onClick: async () => {
            try {
              await api.patch(`/api/admin/menu/items/${item.id}/availability`, {
                available: !item.manualAvailable
              });
              toast('Ръчната наличност е обновена.', 'success');
              await reload();
            } catch (err) { handleError(err); }
          }
        })
      ])
    ]);

    mount(el('div', { className: 'stack' }, [
      panel('Категории', [
        categories.length
          ? table('Категории', ['ID', 'Име', 'Описание', 'Статус', 'Действия'], catRows)
          : emptyState('Няма категории.')
      ], [
        el('button', { type: 'button', className: 'btn', text: 'Нова категория', onClick: () => openCategoryDialog(null, () => reload()) }),
        el('button', { type: 'button', className: 'btn btn-secondary', text: 'Презареди', onClick: () => reload() })
      ]),
      panel('Ястия', [
        el('p', { className: 'muted', text: 'Ефективната наличност идва от backend (рецепта + склад + ръчен флаг).' }),
        items.length
          ? table('Ястия', ['ID', 'Име', 'Категория', 'Цена', 'Активно', 'Ръчно', 'Ефективно', 'Причина', 'Действия'], itemRows)
          : emptyState('Няма ястия.')
      ], [
        el('button', {
          type: 'button', className: 'btn', text: 'Ново ястие',
          onClick: () => openItemDialog(null, categories, () => reload())
        }),
        el('button', {
          type: 'button', className: 'btn btn-secondary', text: 'Преизчисли наличност',
          onClick: async () => {
            try {
              await api.post('/api/admin/menu/availability/recalculate', {});
              toast('Наличността е преизчислена.', 'success');
              await reload();
            } catch (err) { handleError(err); }
          }
        })
      ])
    ]));
  } catch (err) {
    mount(errorBox(handleError(err), () => reload()));
  }
}

function openCategoryDialog(existing, onDone) {
  const name = el('input', { type: 'text', value: existing?.name || '', required: 'true' });
  const description = el('textarea', {}, existing?.description || '');
  const submit = el('button', { type: 'button', className: 'btn', text: existing ? 'Запази' : 'Създай' });
  submit.addEventListener('click', async () => {
    submit.disabled = true;
    try {
      const body = { name: name.value.trim(), description: description.value.trim() || null };
      if (existing) await api.put(`/api/admin/menu/categories/${existing.id}`, body);
      else await api.post('/api/admin/menu/categories', body);
      closeDialog();
      toast('Категорията е записана.', 'success');
      await onDone();
    } catch (err) {
      handleError(err);
      submit.disabled = false;
    }
  });
  openDialog({
    title: existing ? 'Редакция на категория' : 'Нова категория',
    body: el('div', { className: 'stack' }, [field('Име', name), field('Описание', description)]),
    footerButtons: [
      el('button', { type: 'button', className: 'btn btn-secondary', text: 'Отказ', onClick: () => closeDialog() }),
      submit
    ]
  });
}

function openItemDialog(existing, categories, onDone) {
  const name = el('input', { type: 'text', value: existing?.name || '' });
  const description = el('textarea', {}, existing?.description || '');
  const price = el('input', { type: 'number', step: '0.01', min: '0', value: existing?.price ?? '' });
  const categoryId = el('select', {}, [
    el('option', { value: '', text: 'Изберете категория' }),
    ...categories.map((c) => el('option', {
      value: String(c.id),
      text: c.name,
      selected: existing && existing.categoryId === c.id ? 'true' : null
    }))
  ]);
  const available = el('input', { type: 'checkbox' });
  available.checked = existing ? existing.manualAvailable !== false : true;

  const submit = el('button', { type: 'button', className: 'btn', text: existing ? 'Запази' : 'Създай' });
  submit.addEventListener('click', async () => {
    submit.disabled = true;
    try {
      const body = {
        name: name.value.trim(),
        description: description.value.trim() || null,
        price: price.value,
        categoryId: Number(categoryId.value),
        available: available.checked
      };
      if (existing) await api.put(`/api/admin/menu/items/${existing.id}`, body);
      else await api.post('/api/admin/menu/items', body);
      closeDialog();
      toast('Ястието е записано.', 'success');
      await onDone();
    } catch (err) {
      handleError(err);
      submit.disabled = false;
    }
  });

  openDialog({
    title: existing ? 'Редакция на ястие' : 'Ново ястие',
    body: el('div', { className: 'stack' }, [
      field('Име', name),
      field('Описание', description),
      field('Цена', price),
      field('Категория', categoryId),
      el('label', { className: 'checkbox-row' }, [available, document.createTextNode('Ръчно налично (manualAvailable)')])
    ]),
    footerButtons: [
      el('button', { type: 'button', className: 'btn btn-secondary', text: 'Отказ', onClick: () => closeDialog() }),
      submit
    ]
  });
}