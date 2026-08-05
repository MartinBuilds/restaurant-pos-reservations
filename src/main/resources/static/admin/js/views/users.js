import { api } from '../api.js';
import {
  setPageMeta, mount, el, panel, table, badge, loading, errorBox, emptyState,
  openDialog, closeDialog, toast, handleError, field, confirmDialog
} from '../ui.js';

const ROLES = ['ADMIN', 'WAITER', 'COOK', 'CLIENT'];
let abortController = null;

export async function renderUsers() {
  setPageMeta('Потребители', 'Създаване, роли и enable/disable');
  mount(loading('Зареждане на потребители...'));
  await reload();
}

async function reload() {
  if (abortController) abortController.abort();
  abortController = new AbortController();
  try {
    const users = await api.get('/api/admin/users', { signal: abortController.signal });
    const createBtn = el('button', {
      type: 'button', className: 'btn', text: 'Нов потребител',
      onClick: () => openCreateDialog(() => reload())
    });
    const reloadBtn = el('button', {
      type: 'button', className: 'btn btn-secondary', text: 'Презареди',
      onClick: () => reload()
    });

    if (!users.length) {
      mount(panel('Потребители', [emptyState('Няма потребители.')], [createBtn, reloadBtn]));
      return;
    }

    const rows = users.map((u) => [
      String(u.id),
      u.fullName || '—',
      u.email || '—',
      el('div', { className: 'row-actions' }, (u.roles || []).map((r) => badge(r, 'info'))),
      badge(u.enabled ? 'Активен' : 'Неактивен', u.enabled ? 'ok' : 'danger'),
      el('div', { className: 'row-actions' }, [
        el('button', {
          type: 'button', className: 'btn btn-secondary', text: 'Роли',
          onClick: () => openRolesDialog(u, () => reload())
        }),
        el('button', {
          type: 'button', className: 'btn btn-secondary',
          text: u.enabled ? 'Деактивирай' : 'Активирай',
          onClick: async () => {
            const ok = await confirmDialog({
              title: u.enabled ? 'Деактивиране' : 'Активиране',
              message: `Промяна на статус за ${u.email}?`,
              confirmLabel: 'Потвърди'
            });
            if (!ok) return;
            try {
              await api.patch(`/api/admin/users/${u.id}/status`, { enabled: !u.enabled });
              toast('Статусът е обновен.', 'success');
              await reload();
            } catch (err) {
              handleError(err);
            }
          }
        })
      ])
    ]);

    mount(panel('Потребители', [
      el('p', { className: 'muted', text: 'Паролите и hash стойностите не се показват. Няма endpoint за редакция на име/email.' }),
      table('Списък с потребители', ['ID', 'Име', 'Email', 'Роли', 'Статус', 'Действия'], rows)
    ], [createBtn, reloadBtn]));
  } catch (err) {
    if (err.name === 'AbortError') return;
    mount(errorBox(handleError(err, 'Неуспешно зареждане на потребители.'), () => reload()));
  }
}

function openCreateDialog(onDone) {
  const email = el('input', { type: 'email', autocomplete: 'off', required: 'true' });
  const fullName = el('input', { type: 'text', autocomplete: 'name', required: 'true' });
  const password = el('input', { type: 'password', autocomplete: 'new-password', required: 'true' });
  const roleBoxes = ROLES.map((role) => {
    const input = el('input', { type: 'checkbox', value: role });
    if (role === 'CLIENT') input.checked = true;
    return el('label', {}, [input, document.createTextNode(role)]);
  });

  const submit = el('button', { type: 'button', className: 'btn', text: 'Създай' });
  submit.addEventListener('click', async () => {
    const roles = roleBoxes
      .map((label) => label.querySelector('input'))
      .filter((i) => i.checked)
      .map((i) => i.value);
    submit.disabled = true;
    submit.textContent = 'Запис...';
    try {
      await api.post('/api/admin/users', {
        email: email.value.trim(),
        fullName: fullName.value.trim(),
        password: password.value,
        roles
      });
      password.value = '';
      closeDialog();
      toast('Потребителят е създаден.', 'success');
      await onDone();
    } catch (err) {
      handleError(err);
      submit.disabled = false;
      submit.textContent = 'Създай';
    }
  });

  openDialog({
    title: 'Нов потребител',
    body: el('div', { className: 'stack' }, [
      field('Име', fullName),
      field('Email', email),
      field('Парола', password),
      el('div', { className: 'field' }, [
        el('span', { text: 'Роли' }),
        el('div', { className: 'checkbox-row' }, roleBoxes)
      ])
    ]),
    footerButtons: [
      el('button', { type: 'button', className: 'btn btn-secondary', text: 'Отказ', onClick: () => closeDialog() }),
      submit
    ]
  });
}

function openRolesDialog(user, onDone) {
  const roleBoxes = ROLES.map((role) => {
    const input = el('input', { type: 'checkbox', value: role });
    input.checked = (user.roles || []).includes(role);
    return el('label', {}, [input, document.createTextNode(role)]);
  });
  const submit = el('button', { type: 'button', className: 'btn', text: 'Запази роли' });
  submit.addEventListener('click', async () => {
    const roles = roleBoxes.map((l) => l.querySelector('input')).filter((i) => i.checked).map((i) => i.value);
    submit.disabled = true;
    try {
      await api.put(`/api/admin/users/${user.id}/roles`, { roles });
      closeDialog();
      toast('Ролите са обновени.', 'success');
      await onDone();
    } catch (err) {
      handleError(err);
      submit.disabled = false;
    }
  });
  openDialog({
    title: `Роли — ${user.email}`,
    body: el('div', { className: 'checkbox-row' }, roleBoxes),
    footerButtons: [
      el('button', { type: 'button', className: 'btn btn-secondary', text: 'Отказ', onClick: () => closeDialog() }),
      submit
    ]
  });
}