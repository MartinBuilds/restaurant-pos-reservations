import { ensureCsrf, logout, setUnauthorizedHandler } from '/operations/js/api.js';
import { handleError, setBanner, toast } from '/operations/js/notifications.js';
import { onRealtimeRefresh, startKitchenRealtime, stopRealtime } from './realtime.js';
import { renderQueue } from './queue.js';

async function boot() {
  setUnauthorizedHandler(() => {
    stopRealtime();
    window.location.assign('/login');
  });

  document.getElementById('logout-btn').addEventListener('click', async () => {
    try {
      stopRealtime();
      await logout();
    } catch { /* ignore */ }
    window.location.assign('/login');
  });
  document.getElementById('refresh-btn').addEventListener('click', () => {
    renderQueue().catch((err) => handleError(err));
  });

  try {
    await ensureCsrf();
    setBanner('Сесията е активна.', 'success');
  } catch (err) {
    handleError(err, 'CSRF не можа да се зареди.');
    return;
  }

  onRealtimeRefresh(async () => {
    await renderQueue();
  });

  await renderQueue();

  try {
    await startKitchenRealtime();
  } catch (err) {
    handleError(err, 'WebSocket връзката не стартира.');
  }
}

boot().catch((err) => {
  console.error(err);
  toast('Кухненският панел не можа да стартира.', 'error');
});