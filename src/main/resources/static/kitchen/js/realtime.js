import { setUnauthorizedHandler } from '/operations/js/api.js';
import { loadCsrf } from '/operations/js/csrf.js';
import { setConnectionStatus } from '/operations/js/connection-status.js';
import { createStompClient } from '/operations/js/stomp-client.js?v=pr18-1';
import { toast } from '/operations/js/notifications.js';
import { t } from '/shared/js/i18n/i18n.js?v=fix-kitchen-1';
import { markOrderHighlight } from './queue.js?v=fix-kitchen-ready-1';

const seenEvents = [];
const SEEN_MAX = 500;
let refreshTimer = null;
let refreshHandler = null;
let client = null;
let stopped = false;

function rememberEvent(eventId) {
  if (!eventId) return false;
  const id = String(eventId);
  if (seenEvents.includes(id)) return true;
  seenEvents.push(id);
  if (seenEvents.length > SEEN_MAX) seenEvents.splice(0, seenEvents.length - SEEN_MAX);
  return false;
}

function scheduleRefresh(reason) {
  if (typeof refreshHandler !== 'function') return;
  if (refreshTimer) clearTimeout(refreshTimer);
  refreshTimer = setTimeout(() => {
    refreshTimer = null;
    refreshHandler(reason).catch(() => {});
  }, 220);
}

export function onRealtimeRefresh(handler) {
  refreshHandler = handler;
}

export function stopRealtime() {
  stopped = true;
  if (client) {
    client.stopReconnect();
    client.disconnect(true);
  }
}

export async function startKitchenRealtime() {
  stopped = false;
  setUnauthorizedHandler(() => {
    stopRealtime();
    window.location.assign('/login');
  });

  client = createStompClient({
    onConnecting: () => setConnectionStatus('connecting'),
    onConnected: () => {
      setConnectionStatus('online');
      scheduleRefresh('connected');
    },
    onDisconnected: () => {
      if (!stopped) setConnectionStatus('disconnected');
    },
    onReconnecting: () => setConnectionStatus('reconnecting'),
    onDenied: () => {
      setConnectionStatus('denied');
      stopRealtime();
      window.location.assign('/login');
    },
    onError: () => {}
  });

  client.subscribe('/topic/kitchen/orders', (payload) => {
    if (!payload || typeof payload !== 'object') return;
    if (rememberEvent(payload.eventId)) return;
    const type = payload.eventType;
    if (type === 'ORDER_CREATED' || type === 'ORDER_STATUS_CHANGED') {
      const label = payload.order?.orderNumber || '';
      const orderId = payload.order?.id;
      if (orderId != null) markOrderHighlight(orderId);
      toast(`${t(type === 'ORDER_CREATED' ? 'realtime.newOrder' : 'realtime.status')}: ${label}`, 'info');
      scheduleRefresh('event');
      return;
    }
  });

  const csrf = await loadCsrf();
  if (!stopped) {
    client.connect({ headerName: csrf.headerName, token: csrf.token });
  }
}