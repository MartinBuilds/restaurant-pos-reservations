export function el(tag, props = {}, children = []) {
  const node = document.createElement(tag);
  Object.entries(props || {}).forEach(([key, value]) => {
    if (value === undefined || value === null || value === false) return;
    if (key === 'className') {
      node.className = value;
    } else if (key === 'text') {
      node.textContent = value;
    } else if (key === 'html') {
      // Only for fully static trusted snippets — callers must not pass API data.
      node.innerHTML = value;
    } else if (key.startsWith('on') && typeof value === 'function') {
      node.addEventListener(key.slice(2).toLowerCase(), value);
    } else if (key === 'dataset' && typeof value === 'object') {
      Object.entries(value).forEach(([dk, dv]) => {
        node.dataset[dk] = String(dv);
      });
    } else if (value === true) {
      node.setAttribute(key, '');
    } else {
      node.setAttribute(key, String(value));
    }
  });
  (Array.isArray(children) ? children : [children]).forEach((child) => {
    if (child === null || child === undefined || child === false) return;
    if (typeof child === 'string' || typeof child === 'number') {
      node.appendChild(document.createTextNode(String(child)));
    } else {
      node.appendChild(child);
    }
  });
  return node;
}

export function clear(node) {
  while (node.firstChild) node.removeChild(node.firstChild);
}

/** Build a data table that becomes labeled cards under 640px (see responsive-records.css). */
export function responsiveDataTable(headers, rows, { caption } = {}) {
  const labels = (headers || []).map((h) => String(h || ''));
  const tableEl = el('table', { className: 'data responsive-data-table' });
  if (caption) tableEl.appendChild(el('caption', { text: caption }));
  tableEl.appendChild(el('thead', {}, [
    el('tr', {}, labels.map((h) => el('th', { text: h || ' ' })))
  ]));
  tableEl.appendChild(el('tbody', {}, (rows || []).map((cells) =>
    el('tr', {}, (cells || []).map((cell, index) => {
      const label = labels[index] || '';
      const isActions = !label || /actions|действия/i.test(label);
      const td = el('td', {
        className: isActions ? 'is-actions' : undefined,
        'data-label': label
      });
      if (cell == null) td.textContent = '—';
      else if (typeof cell === 'string' || typeof cell === 'number') td.textContent = String(cell);
      else td.appendChild(cell);
      return td;
    }))
  )));
  return el('div', { className: 'table-wrap' }, [tableEl]);
}

export function fragment(children) {
  const frag = document.createDocumentFragment();
  (Array.isArray(children) ? children : [children]).forEach((child) => {
    if (child) frag.appendChild(child);
  });
  return frag;
}