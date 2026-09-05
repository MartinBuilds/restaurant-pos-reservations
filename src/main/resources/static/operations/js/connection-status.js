import { clear, el } from './dom.js';
import { t } from '/shared/js/i18n/i18n.js?v=pr17-4';

export function setConnectionStatus(state, detail) {
  const node = document.getElementById('connection-status');
  if (!node) return;
  clear(node);
  const label = t(`conn.${state}`) || String(state);
  node.dataset.state = state;
  node.appendChild(el('span', { className: `conn-dot conn-${state}`, 'aria-hidden': 'true' }));
  node.appendChild(el('span', { className: 'conn-label', text: detail ? `${label} — ${detail}` : label }));
}
