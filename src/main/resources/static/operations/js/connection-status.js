import { clear, el } from './dom.js';

const LABELS = {
  connecting: 'Свързване',
  online: 'Онлайн',
  disconnected: 'Прекъсната връзка',
  reconnecting: 'Повторно свързване',
  denied: 'Session expired / Access denied'
};

export function setConnectionStatus(state, detail) {
  const node = document.getElementById('connection-status');
  if (!node) return;
  clear(node);
  const label = LABELS[state] || String(state);
  node.dataset.state = state;
  node.appendChild(el('span', { className: `conn-dot conn-${state}`, 'aria-hidden': 'true' }));
  node.appendChild(el('span', { className: 'conn-label', text: detail ? `${label} — ${detail}` : label }));
}