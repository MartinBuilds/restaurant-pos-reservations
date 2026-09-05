import { api } from '../api.js';
import {
  setPageMeta, mount, el, panel, table, badge, loading, errorBox, emptyState,
  openDialog, closeDialog, toast, handleError, field, confirmDialog
} from '../ui.js';
import { t } from '/shared/js/i18n/i18n.js?v=pr17-4';

const ROLES = ['ADMIN', 'WAITER', 'COOK', 'CLIENT'];
let abortController = null;

export async function renderUsers() {
  setPageMeta(t('page.users.title'), t('page.users.subtitle'));
  mount(loading(t('msg.loadingUsers')));
  await reload();
}

async function reload() {
  if (abortController) abortController.abort();
  abortController = new AbortController();
  try {
    const users = await api.get('/api/admin/users', { signal: abortController.signal });
    const createBtn = el('button', {
      type: 'button', className: 'btn', text: t('action.newUser'),
      onClick: () => openCreateDialog(() => reload())
    });
    const reloadBtn = el('button', {
      type: 'button', className: 'btn btn-secondary', text: t('action.reload'),
      onClick: () => reload()
    });

    if (!users.length) {
      mount(panel(t('page.users.title'), [emptyState(t('msg.usersEmpty'))], [createBtn, reloadBtn]));
      return;
    }

    const rows = users.map((u) => [
      String(u.id),
      u.fullName || '—',
      u.email || '—',
      el('div', { className: 'row-actions' }, (u.roles || []).map((r) => badge(r, 'info'))),
      badge(u.enabled ? t('common.active') : t('common.inactive'), u.enabled ? 'ok' : 'danger'),
      el('div', { className: 'row-actions' }, [
        el('button', {
          type: 'button', className: 'btn btn-secondary', text: t('action.roles'),
          onClick: () => openRolesDialog(u, () => reload())
        }),
        el('button', {
          type: 'button', className: 'btn btn-secondary',
          text: u.enabled ? t('action.disable') : t('action.enable'),
          onClick: async () => {
            const ok = await confirmDialog({
              title: u.enabled ? t('users.confirmDisableTitle') : t('users.confirmEnableTitle'),
              message: t('users.confirmStatusMsg', { email: u.email }),
              confirmLabel: t('common.confirm')
            });
            if (!ok) return;
            try {
              await api.patch(`/api/admin/users/${u.id}/status`, { enabled: !u.enabled });
              toast(t('msg.statusUpdated'), 'success');
              await reload();
            } catch (err) {
              handleError(err);
            }
          }
        })
      ])
    ]);

    mount(panel(t('page.users.title'), [
      el('p', { className: 'muted', text: t('users.passwordNote') }),
      table(t('users.listTitle'), [t('col.id'), t('col.name'), t('col.email'), t('col.roles'), t('col.status'), t('common.actions')], rows)
    ], [createBtn, reloadBtn]));
  } catch (err) {
    if (err.name === 'AbortError') return;
    mount(errorBox(handleError(err, t('users.loadError')), () => reload()));
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

  const submit = el('button', { type: 'button', className: 'btn', text: t('common.create') });
  submit.addEventListener('click', async () => {
    const roles = roleBoxes
      .map((label) => label.querySelector('input'))
      .filter((i) => i.checked)
      .map((i) => i.value);
    submit.disabled = true;
    submit.textContent = t('common.loading');
    try {
      await api.post('/api/admin/users', {
        email: email.value.trim(),
        fullName: fullName.value.trim(),
        password: password.value,
        roles
      });
      password.value = '';
      closeDialog();
      toast(t('msg.created'), 'success');
      await onDone();
    } catch (err) {
      handleError(err);
      submit.disabled = false;
      submit.textContent = t('common.create');
    }
  });

  openDialog({
    title: t('action.newUser'),
    body: el('div', { className: 'stack' }, [
      field(t('col.name'), fullName),
      field(t('col.email'), email),
      field(t('col.password'), password),
      el('div', { className: 'field' }, [
        el('span', { text: t('col.roles') }),
        el('div', { className: 'checkbox-row' }, roleBoxes)
      ])
    ]),
    footerButtons: [
      el('button', { type: 'button', className: 'btn btn-secondary', text: t('common.cancel'), onClick: () => closeDialog() }),
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
  const submit = el('button', { type: 'button', className: 'btn', text: t('common.save') });
  submit.addEventListener('click', async () => {
    const roles = roleBoxes.map((l) => l.querySelector('input')).filter((i) => i.checked).map((i) => i.value);
    submit.disabled = true;
    try {
      await api.put(`/api/admin/users/${user.id}/roles`, { roles });
      closeDialog();
      toast(t('msg.updated'), 'success');
      await onDone();
    } catch (err) {
      handleError(err);
      submit.disabled = false;
    }
  });
  openDialog({
    title: t('users.rolesTitle', { email: user.email }),
    body: el('div', { className: 'checkbox-row' }, roleBoxes),
    footerButtons: [
      el('button', { type: 'button', className: 'btn btn-secondary', text: t('common.cancel'), onClick: () => closeDialog() }),
      submit
    ]
  });
}
