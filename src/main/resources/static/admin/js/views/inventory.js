import { api } from '../api.js';
import { quantity } from '../format.js';
import {
  setPageMeta, mount, el, panel, table, badge, loading, errorBox, emptyState,
  openDialog, closeDialog, toast, handleError, field
} from '../ui.js';
import { t } from '/shared/js/i18n/i18n.js?v=pr19-1';

const UNITS = ['GRAM', 'MILLILITER', 'PIECE'];

export async function renderInventory() {
  setPageMeta(t('page.inventory.title'), t('page.inventory.subtitle'));
  mount(loading(t('common.loading')));
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
      badge(ing.active ? t('common.active') : t('common.inactive'), ing.active ? 'ok' : 'muted'),
      badge(ing.lowStock ? t('inventory.lowStock') : t('inventory.stockOk'), ing.lowStock ? 'warn' : 'ok'),
      el('div', { className: 'row-actions' }, [
        el('button', { type: 'button', className: 'btn btn-secondary', text: t('common.edit'), onClick: () => openIngredientDialog(ing, () => reload()) }),
        el('button', { type: 'button', className: 'btn btn-secondary', text: t('action.stock'), onClick: () => openStockDialog(ing, () => reload()) }),
        el('button', {
          type: 'button', className: 'btn btn-secondary', text: ing.active ? t('action.disable') : t('action.enable'),
          onClick: async () => {
            try {
              await api.patch(`/api/admin/inventory/ingredients/${ing.id}/status`, { active: !ing.active });
              toast(t('msg.statusUpdated'), 'success');
              await reload();
            } catch (err) { handleError(err); }
          }
        })
      ])
    ]);

    mount(el('div', { className: 'stack' }, [
      panel(t('inventory.ingredients'), [
        ingredients.length
          ? table(t('inventory.ingredients'), [t('col.id'), t('col.name'), t('col.unit'), t('col.stock'), t('col.min'), t('col.activeFem'), t('col.signal'), t('common.actions')], rows)
          : emptyState(t('common.empty'))
      ], [
        el('button', { type: 'button', className: 'btn', text: t('action.newIngredient'), onClick: () => openIngredientDialog(null, () => reload()) }),
        el('button', { type: 'button', className: 'btn btn-secondary', text: t('action.reload'), onClick: () => reload() })
      ]),
      panel(t('inventory.recipes'), [
        el('p', { className: 'muted', text: t('inventory.recipeNote') }),
        recipePicker(items, ingredients)
      ])
    ]));
  } catch (err) {
    mount(errorBox(handleError(err), () => reload()));
  }
}

function recipePicker(items, ingredients) {
  const select = el('select', {}, [
    el('option', { value: '', text: t('inventory.selectItem') }),
    ...items.map((i) => el('option', { value: String(i.id), text: i.name }))
  ]);
  const area = el('div', { className: 'stack' });
  const loadBtn = el('button', {
    type: 'button', className: 'btn btn-secondary', text: t('action.loadRecipe'),
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
    el('div', { className: 'filters' }, [field(t('col.menuItem'), select), loadBtn]),
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
        el('option', { value: '', text: t('col.ingredient') }),
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
        field(t('col.ingredient'), ingSelect),
        field(t('col.quantity'), qty),
        el('button', {
          type: 'button', className: 'btn btn-ghost', text: t('common.delete'),
          onClick: () => { rowsState.splice(idx, 1); redraw(); }
        })
      ]));
    });
  };
  redraw();

  const save = el('button', {
    type: 'button', className: 'btn', text: t('common.save'),
    onClick: async () => {
      save.disabled = true;
      try {
        const components = rowsState
          .filter((r) => r.ingredientId && r.quantityRequired !== '')
          .map((r) => ({ ingredientId: Number(r.ingredientId), quantityRequired: r.quantityRequired }));
        await api.put(`/api/admin/menu/items/${menuItemId}/recipe`, { components });
        toast(t('msg.saved'), 'success');
        await onDone();
      } catch (err) {
        handleError(err);
        save.disabled = false;
      }
    }
  });

  const clearBtn = el('button', {
    type: 'button', className: 'btn btn-danger', text: t('common.delete'),
    onClick: async () => {
      try {
        await api.delete(`/api/admin/menu/items/${menuItemId}/recipe`);
        toast(t('msg.saved'), 'success');
        await onDone();
      } catch (err) { handleError(err); }
    }
  });

  area.appendChild(el('div', { className: 'stack' }, [
    el('h3', { text: recipe.menuItemName || t('inventory.itemFallback', { id: menuItemId }) }),
    list,
    el('div', { className: 'row-actions' }, [
      el('button', {
        type: 'button', className: 'btn btn-secondary', text: t('action.addRow'),
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
  const submit = el('button', { type: 'button', className: 'btn', text: existing ? t('common.save') : t('common.create') });
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
      toast(existing ? t('msg.saved') : t('msg.created'), 'success');
      await onDone();
    } catch (err) {
      handleError(err);
      submit.disabled = false;
    }
  });
  openDialog({
    title: existing ? t('inventory.editIngredient') : t('inventory.newIngredient'),
    body: el('div', { className: 'stack' }, [
      field(t('col.name'), name),
      field(t('col.unit'), unit),
      existing ? null : field(t('inventory.initialStock'), stockQuantity),
      field(t('inventory.minStock'), minimumStockLevel)
    ].filter(Boolean)),
    footerButtons: [
      el('button', { type: 'button', className: 'btn btn-secondary', text: t('common.cancel'), onClick: () => closeDialog() }),
      submit
    ]
  });
}

function openStockDialog(ing, onDone) {
  const quantityChange = el('input', { type: 'number', step: '0.001', value: '0' });
  const note = el('input', { type: 'text' });
  const submit = el('button', { type: 'button', className: 'btn', text: t('common.confirm') });
  submit.addEventListener('click', async () => {
    submit.disabled = true;
    try {
      await api.patch(`/api/admin/inventory/ingredients/${ing.id}/stock`, {
        quantityChange: quantityChange.value,
        note: note.value.trim() || null
      });
      closeDialog();
      toast(t('msg.saved'), 'success');
      await onDone();
    } catch (err) {
      handleError(err);
      submit.disabled = false;
    }
  });
  openDialog({
    title: t('inventory.adjustTitle', { name: ing.name }),
    body: el('div', { className: 'stack' }, [
      el('p', { className: 'muted', text: t('inventory.currentStock', { qty: quantity(ing.stockQuantity), unit: ing.unit }) }),
      field(t('inventory.qtyChange'), quantityChange),
      field(t('col.note'), note)
    ]),
    footerButtons: [
      el('button', { type: 'button', className: 'btn btn-secondary', text: t('common.cancel'), onClick: () => closeDialog() }),
      submit
    ]
  });
}
