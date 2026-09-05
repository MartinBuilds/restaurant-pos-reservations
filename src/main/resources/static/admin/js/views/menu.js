import { api } from '../api.js';
import { money } from '../format.js';
import {
  setPageMeta, mount, el, panel, table, badge, loading, errorBox, emptyState,
  openDialog, closeDialog, toast, handleError, field
} from '../ui.js';
import { t } from '/shared/js/i18n/i18n.js?v=pr17-4';

export async function renderMenu() {
  setPageMeta(t('page.menu.title'), t('page.menu.subtitle'));
  mount(loading(t('common.loading')));
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
      badge(c.active ? t('common.active') : t('common.inactive'), c.active ? 'ok' : 'muted'),
      el('div', { className: 'row-actions' }, [
        el('button', {
          type: 'button', className: 'btn btn-secondary', text: t('common.edit'),
          onClick: () => openCategoryDialog(c, () => reload())
        }),
        el('button', {
          type: 'button', className: 'btn btn-secondary', text: c.active ? t('action.disable') : t('action.enable'),
          onClick: async () => {
            try {
              await api.patch(`/api/admin/menu/categories/${c.id}/status`, { active: !c.active });
              toast(t('msg.statusUpdated'), 'success');
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
      badge(item.active ? t('common.active') : t('common.inactive'), item.active ? 'ok' : 'muted'),
      badge(item.manualAvailable ? t('menu.manualYes') : t('menu.manualNo'), item.manualAvailable ? 'info' : 'warn'),
      badge(item.available ? t('menu.effectiveYes') : t('menu.effectiveNo'), item.available ? 'ok' : 'danger'),
      item.availabilityReason || '—',
      el('div', { className: 'row-actions' }, [
        el('button', {
          type: 'button', className: 'btn btn-secondary', text: t('common.edit'),
          onClick: () => openItemDialog(item, categories, () => reload())
        }),
        el('button', {
          type: 'button', className: 'btn btn-secondary', text: item.active ? t('action.disable') : t('action.enable'),
          onClick: async () => {
            try {
              await api.patch(`/api/admin/menu/items/${item.id}/status`, { active: !item.active });
              toast(t('msg.statusUpdated'), 'success');
              await reload();
            } catch (err) { handleError(err); }
          }
        }),
        el('button', {
          type: 'button', className: 'btn btn-secondary', text: t('action.manualAvailability'),
          onClick: async () => {
            try {
              await api.patch(`/api/admin/menu/items/${item.id}/availability`, {
                available: !item.manualAvailable
              });
              toast(t('msg.saved'), 'success');
              await reload();
            } catch (err) { handleError(err); }
          }
        })
      ])
    ]);

    mount(el('div', { className: 'stack' }, [
      panel(t('menu.categories'), [
        categories.length
          ? table(t('menu.categories'), [t('col.id'), t('col.name'), t('col.description'), t('col.status'), t('common.actions')], catRows)
          : emptyState(t('common.empty'))
      ], [
        el('button', { type: 'button', className: 'btn', text: t('action.newCategory'), onClick: () => openCategoryDialog(null, () => reload()) }),
        el('button', { type: 'button', className: 'btn btn-secondary', text: t('action.reload'), onClick: () => reload() })
      ]),
      panel(t('menu.items'), [
        el('p', { className: 'muted', text: t('menu.availabilityNote') }),
        items.length
          ? table(t('menu.items'), [t('col.id'), t('col.name'), t('col.category'), t('col.price'), t('col.active'), t('col.manual'), t('col.effective'), t('col.reason'), t('common.actions')], itemRows)
          : emptyState(t('common.empty'))
      ], [
        el('button', {
          type: 'button', className: 'btn', text: t('action.newItem'),
          onClick: () => openItemDialog(null, categories, () => reload())
        }),
        el('button', {
          type: 'button', className: 'btn btn-secondary', text: t('action.recalcAvailability'),
          onClick: async () => {
            try {
              await api.post('/api/admin/menu/availability/recalculate', {});
              toast(t('msg.saved'), 'success');
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
  const submit = el('button', { type: 'button', className: 'btn', text: existing ? t('common.save') : t('common.create') });
  submit.addEventListener('click', async () => {
    submit.disabled = true;
    try {
      const body = { name: name.value.trim(), description: description.value.trim() || null };
      if (existing) await api.put(`/api/admin/menu/categories/${existing.id}`, body);
      else await api.post('/api/admin/menu/categories', body);
      closeDialog();
      toast(existing ? t('msg.saved') : t('msg.created'), 'success');
      await onDone();
    } catch (err) {
      handleError(err);
      submit.disabled = false;
    }
  });
  openDialog({
    title: existing ? t('menu.editCategory') : t('menu.newCategory'),
    body: el('div', { className: 'stack' }, [field(t('col.name'), name), field(t('col.description'), description)]),
    footerButtons: [
      el('button', { type: 'button', className: 'btn btn-secondary', text: t('common.cancel'), onClick: () => closeDialog() }),
      submit
    ]
  });
}

function openItemDialog(existing, categories, onDone) {
  const name = el('input', { type: 'text', value: existing?.name || '' });
  const description = el('textarea', {}, existing?.description || '');
  const price = el('input', { type: 'number', step: '0.01', min: '0', value: existing?.price ?? '' });
  const categoryId = el('select', {}, [
    el('option', { value: '', text: t('menu.selectCategory') }),
    ...categories.map((c) => el('option', {
      value: String(c.id),
      text: c.name,
      selected: existing && existing.categoryId === c.id ? 'true' : null
    }))
  ]);
  const available = el('input', { type: 'checkbox' });
  available.checked = existing ? existing.manualAvailable !== false : true;

  const submit = el('button', { type: 'button', className: 'btn', text: existing ? t('common.save') : t('common.create') });
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
      toast(existing ? t('msg.saved') : t('msg.created'), 'success');
      await onDone();
    } catch (err) {
      handleError(err);
      submit.disabled = false;
    }
  });

  openDialog({
    title: existing ? t('menu.editItem') : t('menu.newItem'),
    body: el('div', { className: 'stack' }, [
      field(t('col.name'), name),
      field(t('col.description'), description),
      field(t('col.price'), price),
      field(t('col.category'), categoryId),
      el('label', { className: 'checkbox-row' }, [available, document.createTextNode(t('menu.manualAvailableFlag'))])
    ]),
    footerButtons: [
      el('button', { type: 'button', className: 'btn btn-secondary', text: t('common.cancel'), onClick: () => closeDialog() }),
      submit
    ]
  });
}
