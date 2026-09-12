import { t, onLanguageChange } from '/shared/js/i18n/i18n.js?v=fix-login-11';

const EYE_OPEN = `
<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
  <path fill="currentColor" d="M12 5c-5 0-9.27 3.11-11 7.5C2.73 16.89 7 20 12 20s9.27-3.11 11-7.5C21.27 8.11 17 5 12 5zm0 12.5c-2.76 0-5-2.24-5-5s2.24-5 5-5 5 2.24 5 5-2.24 5-5 5zm0-8c-1.66 0-3 1.34-3 3s1.34 3 3 3 3-1.34 3-3-1.34-3-3-3z"/>
</svg>`;

const EYE_OFF = `
<svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
  <path fill="currentColor" d="M12 6.5c2.76 0 5 2.24 5 5 0 .65-.13 1.26-.36 1.83l1.5 1.5C19.27 13.7 20.27 12.7 21 12.5 19.27 8.11 15 5 12 5c-1.1 0-2.15.2-3.12.56l1.56 1.56c.5-.2 1.01-.31 1.56-.31zM3.27 2.5 2 3.77l2.1 2.1C2.7 7.3 1.61 8.8 1 10.5 2.73 14.89 7 18 12 18c1.52 0 2.97-.3 4.3-.83l2.4 2.4 1.27-1.27L3.27 2.5zM12 15.5c-2.76 0-5-2.24-5-5 0-.7.15-1.36.42-1.95l1.55 1.55c-.03.13-.05.27-.05.4 0 1.66 1.34 3 3 3 .13 0 .27-.02.4-.05l1.55 1.55c-.59.27-1.25.42-1.95.42zm2.97-3.45c.1-.3.15-.62.15-.95 0-1.66-1.34-3-3-3-.33 0-.65.05-.95.15l3.8 3.8z"/>
</svg>`;

function labelFor(visible) {
  return visible ? t('password.hide') : t('password.show');
}

function setToggleVisual(button, visible) {
  button.setAttribute('aria-pressed', visible ? 'true' : 'false');
  button.setAttribute('aria-label', labelFor(visible));
  button.title = labelFor(visible);
  button.innerHTML = visible ? EYE_OFF : EYE_OPEN;
}

/**
 * Wraps a password input with a show/hide toggle. Safe to call more than once.
 * @param {HTMLInputElement} input
 * @returns {HTMLInputElement}
 */
export function enhancePasswordInput(input) {
  if (!(input instanceof HTMLInputElement)) return input;
  if (input.dataset.passwordToggle === '1') return input;

  input.dataset.passwordToggle = '1';
  const wrap = document.createElement('div');
  wrap.className = 'password-field';
  if (input.parentNode) {
    input.parentNode.insertBefore(wrap, input);
  }
  wrap.appendChild(input);

  const button = document.createElement('button');
  button.type = 'button';
  button.className = 'password-toggle';
  setToggleVisual(button, false);
  button.addEventListener('click', () => {
    const visible = input.type === 'password';
    input.type = visible ? 'text' : 'password';
    setToggleVisual(button, visible);
  });
  wrap.appendChild(button);
  return input;
}

/**
 * Enhance all password inputs under root (default: document).
 * @param {ParentNode} [root]
 */
export function enhancePasswordFields(root = document) {
  root.querySelectorAll('input[type="password"]').forEach((input) => {
    enhancePasswordInput(input);
  });
}

onLanguageChange(() => {
  document.querySelectorAll('.password-toggle').forEach((button) => {
    const visible = button.getAttribute('aria-pressed') === 'true';
    button.setAttribute('aria-label', labelFor(visible));
    button.title = labelFor(visible);
  });
});
