/**
 * Open/close overlay dialogs with mobile bottom-sheet motion.
 * Visibility uses [hidden]; sheet slide uses .is-open.
 */

function isMobileSheet() {
  return window.matchMedia('(max-width: 640px)').matches;
}

function prefersReducedMotion() {
  return window.matchMedia('(prefers-reduced-motion: reduce)').matches;
}

export function openOverlayDialog(dialog, overlay) {
  if (!dialog || !overlay) return;
  overlay.hidden = false;
  dialog.hidden = false;
  overlay.classList.add('is-open');
  dialog.classList.remove('is-closing');
  // Double rAF so the off-screen transform paints before sliding up.
  requestAnimationFrame(() => {
    requestAnimationFrame(() => {
      dialog.classList.add('is-open');
    });
  });
}

export function closeOverlayDialog(dialog, overlay) {
  if (!dialog || !overlay) return Promise.resolve();

  const finish = () => {
    dialog.classList.remove('is-open', 'is-closing');
    overlay.classList.remove('is-open');
    dialog.hidden = true;
    overlay.hidden = true;
  };

  if (!dialog.classList.contains('is-open')) {
    finish();
    return Promise.resolve();
  }

  if (!isMobileSheet() || prefersReducedMotion()) {
    finish();
    return Promise.resolve();
  }

  return new Promise((resolve) => {
    let done = false;
    const complete = () => {
      if (done) return;
      done = true;
      dialog.removeEventListener('transitionend', onEnd);
      finish();
      resolve();
    };
    const onEnd = (event) => {
      if (event.target !== dialog) return;
      if (event.propertyName !== 'transform') return;
      complete();
    };
    dialog.classList.add('is-closing');
    dialog.classList.remove('is-open');
    overlay.classList.remove('is-open');
    dialog.addEventListener('transitionend', onEnd);
    window.setTimeout(complete, 420);
  });
}
