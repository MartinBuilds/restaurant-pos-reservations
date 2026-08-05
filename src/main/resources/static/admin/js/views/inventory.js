import { api } from '../api.js';
import { quantity } from '../format.js';
import {
  setPageMeta, mount, el, panel, table, badge, loading, errorBox, emptyState,
  openDialog, closeDialog, toast, handleError, field
} from '../ui.js';

const UNITS = ['GRAM', 'MILLILITER', 'PIECE'];

export async function renderInventory() {
  setPageMeta('Склад и рецепти', 'Съставки, запас и рецепти по ястия');
  mount(loading());
  await reload();
}

async function reload() {
  try {
    const [ingredients, items] = await Promise.all([
      api.get('/api/admin/inventory/ingredients'),
      api.get('/api/admin/menu/items')
    ]);

    const rows = ingredients.map((ing) => [
      String(ing.id),
      ing.name || '—',
      ing.unit || '—',
      quantity(ing.stockQuantity),
      quantity(ing.minimumStockLevel),
      badge(ing.active ? 'Активна' : 'Неактивна', ing.active ? 'ok' : 'muted'),
      badge(ing.lowStock ? 'Нисък запас' : 'OK', ing.lowStock ? 'warn' : 'ok'),
      el('div', { className: 'row-actions' }, [
        el('button', { type: 'button', className: 'btn btn-secondary', text: 'Редакция', onClick: () => openIngredientDialog(ing, () => reload()) }),
        el('button', { type: 'button', className: 'btn btn-secondary', text: 'Запас', onClick: () => openStockDialog(ing, () => reload()) }),
        el('button', {
          type: 'button', className: 'btn btn-secondary', text: ing.active ? 'Деактивирай' : 'Активирай',
          onClick: async () => {
            try {
              await api.patch(`/api/admin/inventory/ingredients/${ing.id}/status`, { active: !ing.active });
              toast('Статусът е обновен.', 'success');
              await reload();
            } catch (err) { handleError(err); }
          }
        })
      ])
    ]);

    mount(el('div', { className: 'stack' }, [
      panel('Съставки', [
        ingredients.length
          ? table('Съставки', ['ID', 'Име', 'Единица', 'Запас', 'Минимум', 'Активна', 'Сигнал', 'Действия'], rows)
          : emptyState('Няма съставки.')
      ], [
        el('button', { type: 'button', className: 'btn', text: 'Нова съставка', onClick: () => openIngredientDialog(null, () => reload()) }),
        el('button', { type: 'button', className: 'btn btn-secondary', text: 'Презареди', onClick: () => reload() })
      ]),
      panel('Рецепти', [
        el('p', { className: 'muted', text: 'Изберете ястие, за да заредите/запишете рецепта. Frontend не преизчислява availability.' }),
        recipePicker(items, ingredients)
      ])
    ]));
  } catch (err) {
    mount(errorBox(handleError(err), () => reload()));
  }
}

function recipePicker(items, ingredients) {
  const select = el('select', {}, [
    el('option', { value: '', text: 'Изберете ястие' }),
    ...items.map((i) => el('option', { value: String(i.id), text: i.name }))
  ]);
  const area = el('div', { className: 'stack' });
  const loadBtn = el('button', {
    type: 'button', className: 'btn btn-secondary', text: 'Зареди рецепта',
    onClick: async () => {
      if (!select.value) return;
      try {
        const recipe = await api.get(`/api/admin/menu/items/${select.value}/recipe`);
        renderRecipeEditor(area, Number(select.value), recipe, ingredients, () => reload());
      } catch (err) {
        if (err.status === 404) {
          renderRecipeEditor(area, Number(select.value), { components: [] }, ingredients, () => reload());
        } else handleError(err);
      }
    }
  });
  return el('div', { className: 'stack' }, [
    el('div', { className: 'filters' }, [field('Ястие', select), loadBtn]),
    area
  ]);
}

function renderRecipeEditor(area, menuItemId, recipe, ingredients, onDone) {
  while (area.firstChild) area.removeChild(area.firstChild);
  const rowsState = (recipe.components || []).map((c) => ({
    ingredientId: c.ingredientId,
    quantityRequired: c.quantityRequired
  }));
  if (!rowsState.length) rowsState.push({ ingredientId: '', quantityRequired: '' });

  const list = el('div', { className: 'stack' });
  const redraw = () => {
    while (list.firstChild) list.removeChild(list.firstChild);
    rowsState.forEach((row, idx) => {
      const ingSelect = el('select', {}, [
        el('option', { value: '', text: 'Съставка' }),
        ...ingredients.map((ing) => el('option', {
          value: String(ing.id),
          text: `${ing.name} (${ing.unit})`,
          selected: String(ing.id) === String(row.ingredientId) ? 'true' : null
        }))
      ]);
      ingSelect.addEventListener('change', () => { row.ingredientId = ingSelect.value; });
      const qty = el('input', { type: 'number', step: '0.001', min: '0', value: row.quantityRequired ?? '' });
      qty.addEventListener('input', () => { row.quantityRequired = qty.value; });
      list.appendChild(el('div', { className: 'filters' }, [
        field('Съставка', ingSelect),
        field('Количество', qty),
        el('button', {
          type: 'button', className: 'btn btn-ghost', text: 'Премахни',
          onClick: () => { rowsState.splice(idx, 1); redraw(); }
        })
      ]));
    });
  };
  redraw();

  const save = el('button', {
    type: 'button', className: 'btn', text: 'Запази рецепта',
    onClick: async () => {
      save.disabled = true;
      try {
        const components = rowsState
          .filter((r) => r.ingredientId && r.quantityRequired !== '')
          .map((r) => ({ ingredientId: Number(r.ingredientId), quantityRequired: r.quantityRequired }));
        await api.put(`/api/admin/menu/items/${menuItemId}/recipe`, { components });
        toast('Рецептата е записана.', 'success');
        await onDone();
      } catch (err) {
        handleError(err);
        save.disabled = false;
      }
    }
  });

  const clearBtn = el('button', {
    type: 'button', className: 'btn btn-danger', text: 'Изтрий рецепта',
    onClick: async () => {
      try {
        await api.delete(`/api/admin/menu/items/${menuItemId}/recipe`);
        toast('Рецептата е изтрита.', 'success');
        await onDone();
      } catch (err) { handleError(err); }
    }
  });

  area.appendChild(el('div', { className: 'stack' }, [
    el('h3', { text: recipe.menuItemName || `Ястие #${menuItemId}` }),
    list,
    el('div', { className: 'row-actions' }, [
      el('button', {
        type: 'button', className: 'btn btn-secondary', text: 'Добави ред',
        onClick: () => { rowsState.push({ ingredientId: '', quantityRequired: '' }); redraw(); }
      }),
      save,
      clearBtn
    ])
  ]));
}

function openIngredientDialog(existing, onDone) {
  const name = el('input', { type: 'text', value: existing?.name || '' });
  const unit = el('select', {}, UNITS.map((u) => el('option', {
    value: u, text: u, selected: existing?.unit === u ? 'true' : null
  })));
  const stockQuantity = el('input', { type: 'number', step: '0.001', min: '0', value: existing?.stockQuantity ?? '0' });
  const minimumStockLevel = el('input', { type: 'number', step: '0.001', min: '0', value: existing?.minimumStockLevel ?? '0' });
  const submit = el('button', { type: 'button', className: 'btn', text: existing ? 'Запази' : 'Създай' });
  submit.addEventListener('click', async () => {
    submit.disabled = true;
    try {
      if (existing) {
        await api.put(`/api/admin/inventory/ingredients/${existing.id}`, {
          name: name.value.trim(),
          unit: unit.value,
          minimumStockLevel: minimumStockLevel.value
        });
      } else {
        await api.post('/api/admin/inventory/ingredients', {
          name: name.value.trim(),
          unit: unit.value,
          stockQuantity: stockQuantity.value,
          minimumStockLevel: minimumStockLevel.value
        });
      }
      closeDialog();
      toast('Съставката е записана.', 'success');
      await onDone();
    } catch (err) {
      handleError(err);
      submit.disabled = false;
    }
  });
  openDialog({
    title: existing ? 'Редакция на съставка' : 'Нова съставка',
    body: el('div', { className: 'stack' }, [
      field('Име', name),
      field('Единица', unit),
      existing ? null : field('Начален запас', stockQuantity),
      field('Минимален запас', minimumStockLevel)
    ].filter(Boolean)),
    footerButtons: [
      el('button', { type: 'button', className: 'btn btn-secondary', text: 'Отказ', onClick: () => closeDialog() }),
      submit
    ]
  });
}

function openStockDialog(ing, onDone) {
  const quantityChange = el('input', { type: 'number', step: '0.001', value: '0' });
  const note = el('input', { type: 'text' });
  const submit = el('button', { type: 'button', className: 'btn', text: 'Приложи' });
  submit.addEventListener('click', async () => {
    submit.disabled = true;
    try {
      await api.patch(`/api/admin/inventory/ingredients/${ing.id}/stock`, {
        quantityChange: quantityChange.value,
        note: note.value.trim() || null
      });
      closeDialog();
      toast('Запасът е обновен.', 'success');
      await onDone();
    } catch (err) {
      handleError(err);
      submit.disabled = false;
    }
  });
  openDialog({
    title: `Корекция на запас — ${ing.name}`,
    body: el('div', { className: 'stack' }, [
      el('p', { className: 'muted', text: `Текущ запас: ${quantity(ing.stockQuantity)} ${ing.unit}` }),
      field('Промяна (+/−)', quantityChange),
      field('Бележка', note)
    ]),
    footerButtons: [
      el('button', { type: 'button', className: 'btn btn-secondary', text: 'Отказ', onClick: () => closeDialog() }),
      submit
    ]
  });
}