/**
 * Minimal STOMP 1.2 client over native WebSocket.
 * Supports CONNECT/SUBSCRIBE/UNSUBSCRIBE/DISCONNECT + heartbeats.
 * No business SEND /app API.
 */

function escapeHeader(value) {
  return String(value)
    .replace(/\\/g, '\\\\')
    .replace(/\r/g, '\\r')
    .replace(/\n/g, '\\n')
    .replace(/:/g, '\\c');
}

function unescapeHeader(value) {
  let out = '';
  for (let i = 0; i < value.length; i += 1) {
    const ch = value[i];
    if (ch === '\\' && i + 1 < value.length) {
      const next = value[i + 1];
      if (next === 'c') { out += ':'; i += 1; continue; }
      if (next === 'n') { out += '\n'; i += 1; continue; }
      if (next === 'r') { out += '\r'; i += 1; continue; }
      if (next === '\\') { out += '\\'; i += 1; continue; }
    }
    out += ch;
  }
  return out;
}

function buildFrame(command, headers = {}, body = '') {
  let frame = `${command}\n`;
  Object.entries(headers).forEach(([k, v]) => {
    if (v === undefined || v === null) return;
    frame += `${escapeHeader(k)}:${escapeHeader(v)}\n`;
  });
  frame += '\n';
  if (body) frame += body;
  frame += '\0';
  return frame;
}

function parseFrames(raw) {
  const frames = [];
  let data = raw;
  while (data.length) {
    if (data === '\n' || data === '\r\n') {
      frames.push({ command: 'HEARTBEAT', headers: {}, body: '' });
      break;
    }
    if (data.startsWith('\n')) {
      frames.push({ command: 'HEARTBEAT', headers: {}, body: '' });
      data = data.slice(1);
      continue;
    }
    const nul = data.indexOf('\0');
    if (nul < 0) break;
    const chunk = data.slice(0, nul);
    data = data.slice(nul + 1);
    if (!chunk || chunk === '\n') {
      frames.push({ command: 'HEARTBEAT', headers: {}, body: '' });
      continue;
    }
    const splitAt = chunk.indexOf('\n\n');
    if (splitAt < 0) continue;
    const head = chunk.slice(0, splitAt);
    const body = chunk.slice(splitAt + 2);
    const lines = head.split('\n');
    const command = lines[0];
    const headers = {};
    for (let i = 1; i < lines.length; i += 1) {
      const line = lines[i];
      if (!line) continue;
      const idx = line.indexOf(':');
      if (idx < 0) continue;
      const key = unescapeHeader(line.slice(0, idx));
      const val = unescapeHeader(line.slice(idx + 1));
      headers[key] = val;
    }
    frames.push({ command, headers, body });
  }
  return frames;
}

function negotiateHeartbeat(clientHb, serverHb) {
  const [cx, cy] = clientHb;
  const [sx, sy] = serverHb;
  const outgoing = (cx === 0 || sy === 0) ? 0 : Math.max(cx, sy);
  const incoming = (cy === 0 || sx === 0) ? 0 : Math.max(cy, sx);
  return { outgoing, incoming };
}

export function createStompClient(options = {}) {
  const clientSendHb = options.heartbeatOutgoing ?? 10000;
  const clientRecvHb = options.heartbeatIncoming ?? 10000;
  let socket = null;
  let connected = false;
  let stopped = false;
  let subCounter = 0;
  const subscriptions = new Map();
  let outgoingTimer = null;
  let incomingWatchdog = null;
  let lastIncoming = 0;
  let negotiated = { outgoing: 0, incoming: 0 };
  let reconnectAttempt = 0;
  let reconnectTimer = null;
  let explicitClose = false;

  const listeners = {
    onConnecting: options.onConnecting || (() => {}),
    onConnected: options.onConnected || (() => {}),
    onDisconnected: options.onDisconnected || (() => {}),
    onReconnecting: options.onReconnecting || (() => {}),
    onError: options.onError || (() => {}),
    onDenied: options.onDenied || (() => {}),
    onMessage: options.onMessage || (() => {})
  };

  function clearTimers() {
    if (outgoingTimer) { clearInterval(outgoingTimer); outgoingTimer = null; }
    if (incomingWatchdog) { clearInterval(incomingWatchdog); incomingWatchdog = null; }
    if (reconnectTimer) { clearTimeout(reconnectTimer); reconnectTimer = null; }
  }

  function markIncoming() {
    lastIncoming = Date.now();
  }

  function startHeartbeats() {
    if (outgoingTimer) { clearInterval(outgoingTimer); outgoingTimer = null; }
    if (incomingWatchdog) { clearInterval(incomingWatchdog); incomingWatchdog = null; }
    if (negotiated.outgoing > 0 && socket && socket.readyState === WebSocket.OPEN) {
      outgoingTimer = setInterval(() => {
        if (socket && socket.readyState === WebSocket.OPEN) {
          socket.send('\n');
        }
      }, negotiated.outgoing);
    }
    if (negotiated.incoming > 0) {
      const timeout = negotiated.incoming * 2.75;
      incomingWatchdog = setInterval(() => {
        if (Date.now() - lastIncoming > timeout) {
          if (socket) {
            try { socket.close(); } catch { /* ignore */ }
          }
        }
      }, Math.max(1000, Math.floor(negotiated.incoming / 2)));
    }
  }

  function wsUrl() {
    const proto = window.location.protocol === 'https:' ? 'wss:' : 'ws:';
    return `${proto}//${window.location.host}/ws`;
  }

  function backoffDelay(attempt) {
    const steps = [1000, 2000, 5000, 10000, 30000];
    const base = steps[Math.min(attempt, steps.length - 1)];
    const jitter = Math.floor(Math.random() * 250);
    return base + jitter;
  }

  function scheduleReconnect() {
    if (stopped || explicitClose) return;
    if (reconnectTimer) return;
    const delay = backoffDelay(reconnectAttempt);
    reconnectAttempt += 1;
    listeners.onReconnecting(delay);
    reconnectTimer = setTimeout(() => {
      reconnectTimer = null;
      connect();
    }, delay);
  }

  function handleFrame(frame) {
    markIncoming();
    if (frame.command === 'HEARTBEAT') return;
    if (frame.command === 'CONNECTED') {
      connected = true;
      reconnectAttempt = 0;
      const serverHb = (frame.headers['heart-beat'] || '0,0').split(',').map((n) => Number(n) || 0);
      negotiated = negotiateHeartbeat([clientSendHb, clientRecvHb], serverHb);
      startHeartbeats();
      subscriptions.forEach((sub) => {
        if (socket && socket.readyState === WebSocket.OPEN) {
          socket.send(buildFrame('SUBSCRIBE', {
            id: sub.id,
            destination: sub.destination,
            ack: 'auto'
          }));
        }
      });
      listeners.onConnected(frame);
      return;
    }
    if (frame.command === 'MESSAGE') {
      let payload = frame.body;
      try {
        payload = frame.body ? JSON.parse(frame.body) : null;
      } catch {
        payload = null;
      }
      const dest = frame.headers.destination;
      const sub = [...subscriptions.values()].find((s) => s.destination === dest)
        || subscriptions.get(frame.headers.subscription);
      if (sub && typeof sub.callback === 'function') {
        sub.callback(payload, frame);
      }
      listeners.onMessage(payload, frame);
      return;
    }
    if (frame.command === 'ERROR') {
      const msg = frame.headers.message || frame.body || 'STOMP ERROR';
      listeners.onError(msg, frame);
      if (/access|denied|forbidden|unauthorized|csrf/i.test(String(msg))) {
        explicitClose = true;
        listeners.onDenied(msg);
        disconnect(true);
      }
      return;
    }
    if (frame.command === 'RECEIPT') {
      // acknowledged; nothing else required
    }
  }

  function connect(csrf) {
    if (stopped) return;
    explicitClose = false;
    if (socket && (socket.readyState === WebSocket.OPEN || socket.readyState === WebSocket.CONNECTING)) {
      return;
    }
    listeners.onConnecting();
    socket = new WebSocket(wsUrl());
    socket.binaryType = 'arraybuffer';

    socket.addEventListener('open', () => {
      const headers = {
        'accept-version': '1.2',
        host: window.location.hostname || 'localhost',
        'heart-beat': `${clientSendHb},${clientRecvHb}`
      };
      if (csrf && csrf.headerName && csrf.token) {
        headers[csrf.headerName] = csrf.token;
      }
      socket.send(buildFrame('CONNECT', headers));
    });

    socket.addEventListener('message', (ev) => {
      let text;
      if (typeof ev.data === 'string') {
        text = ev.data;
      } else if (ev.data instanceof ArrayBuffer) {
        text = new TextDecoder('utf-8').decode(ev.data);
      } else {
        return;
      }
      try {
        parseFrames(text).forEach(handleFrame);
      } catch (err) {
        listeners.onError('Malformed STOMP frame', err);
      }
    });

    socket.addEventListener('close', () => {
      const wasConnected = connected;
      connected = false;
      clearTimers();
      socket = null;
      listeners.onDisconnected(wasConnected);
      if (!explicitClose && !stopped) {
        scheduleReconnect();
      }
    });

    socket.addEventListener('error', () => {
      // close handler will reconnect
    });
  }

  function subscribe(destination, callback) {
    subCounter += 1;
    const id = `sub-${subCounter}`;
    const sub = { id, destination, callback };
    subscriptions.set(id, sub);
    if (connected && socket && socket.readyState === WebSocket.OPEN) {
      socket.send(buildFrame('SUBSCRIBE', { id, destination, ack: 'auto' }));
    }
    return id;
  }

  function unsubscribe(id) {
    const sub = subscriptions.get(id);
    if (!sub) return;
    subscriptions.delete(id);
    if (connected && socket && socket.readyState === WebSocket.OPEN) {
      socket.send(buildFrame('UNSUBSCRIBE', { id }));
    }
  }

  function disconnect(permanent = false) {
    if (permanent) {
      stopped = true;
      explicitClose = true;
    } else {
      explicitClose = true;
    }
    clearTimers();
    if (socket && socket.readyState === WebSocket.OPEN) {
      try {
        socket.send(buildFrame('DISCONNECT'));
      } catch { /* ignore */ }
      try { socket.close(); } catch { /* ignore */ }
    } else if (socket) {
      try { socket.close(); } catch { /* ignore */ }
    }
    socket = null;
    connected = false;
  }

  function stopReconnect() {
    explicitClose = true;
    stopped = true;
    clearTimers();
  }

  function allowReconnect() {
    stopped = false;
    explicitClose = false;
    reconnectAttempt = 0;
  }

  window.addEventListener('beforeunload', () => {
    disconnect(true);
  });

  return {
    connect,
    subscribe,
    unsubscribe,
    disconnect,
    stopReconnect,
    allowReconnect,
    isConnected: () => connected
  };
}