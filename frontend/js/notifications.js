/**
 * Secure Smart To-Do List - Notification & Reminder Center
 * Manages browser permission, in-app notification center, and Web Push notifications.
 */

const NotificationManager = (() => {
  let pendingReminders = [];
  let pollInterval = null;
  const NOTIFIED_CACHE_KEY = 'notified_reminders_set';

  function getNotifiedSet() {
    try {
      const stored = localStorage.getItem(NOTIFIED_CACHE_KEY);
      return stored ? new Set(JSON.parse(stored)) : new Set();
    } catch (e) {
      return new Set();
    }
  }

  function markAsLocallyNotified(id) {
    const set = getNotifiedSet();
    set.add(id);
    localStorage.setItem(NOTIFIED_CACHE_KEY, JSON.stringify(Array.from(set)));
  }

  async function init() {
    setupUI();
    checkPermissionStatus();
    await fetchPendingReminders();
    startPolling();
  }

  function setupUI() {
    const bellBtn = document.getElementById('notification-bell-btn');
    if (bellBtn) {
      bellBtn.addEventListener('click', openNotificationCenter);
    }

    const reqBtn = document.getElementById('enable-notifications-btn');
    if (reqBtn) {
      reqBtn.addEventListener('click', requestPermission);
    }
  }

  function checkPermissionStatus() {
    const statusEl = document.getElementById('notif-permission-status');
    if (!('Notification' in window)) {
      if (statusEl) statusEl.textContent = 'Not supported in this browser';
      return;
    }

    const perm = Notification.permission;
    if (statusEl) {
      if (perm === 'granted') {
        statusEl.innerHTML = '<span style="color: var(--success); font-weight: 600;">✓ Notifications enabled</span>';
      } else if (perm === 'denied') {
        statusEl.innerHTML = '<span style="color: var(--danger); font-weight: 600;">✕ Notifications blocked in browser settings</span>';
      } else {
        statusEl.innerHTML = '<span style="color: var(--warning); font-weight: 600;">? Permission not requested yet</span>';
      }
    }
  }

  async function requestPermission() {
    if (!('Notification' in window)) {
      App.showToast('Browser notifications are not supported on this device/browser.', { isError: true });
      return;
    }

    try {
      const result = await Notification.requestPermission();
      checkPermissionStatus();

      if (result === 'granted') {
        App.showToast('Notification permission granted!');
        showTestNotification();
      } else if (result === 'denied') {
        App.showToast('Notifications were denied. You can enable them in your browser site permissions.', { isError: true });
      }
    } catch (err) {
      console.error('Permission request failed:', err);
    }
  }

  function showTestNotification() {
    if ('Notification' in window && Notification.permission === 'granted') {
      try {
        new Notification('Secure Todo', {
          body: 'Reminders are active! You will be alerted before your tasks are due.',
          icon: '/icons/icon-192.png'
        });
      } catch (e) {
        // Fallback for Android service worker notifications
        if (navigator.serviceWorker && navigator.serviceWorker.ready) {
          navigator.serviceWorker.ready.then(reg => {
            reg.showNotification('Secure Todo', {
              body: 'Reminders are active! You will be alerted before your tasks are due.',
              icon: '/icons/icon-192.png'
            });
          });
        }
      }
    }
  }

  async function fetchPendingReminders() {
    if (!API.isAuthenticated()) return;

    try {
      pendingReminders = await API.getPendingReminders();
      updateBadge();
      checkAndTriggerReminders();
    } catch (e) {
      console.warn('Failed to fetch pending reminders:', e);
    }
  }

  function updateBadge() {
    const badge = document.getElementById('notif-unread-badge');
    if (!badge) return;

    const count = pendingReminders.length;
    if (count > 0) {
      badge.textContent = count > 99 ? '99+' : count;
      badge.style.display = 'flex';
    } else {
      badge.style.display = 'none';
    }
  }

  function checkAndTriggerReminders() {
    const now = new Date();
    const notifiedSet = getNotifiedSet();

    pendingReminders.forEach(rem => {
      const remTime = new Date(rem.reminderDatetime);

      // If reminder is matured and hasn't been fired in this browser session
      if (remTime <= now && !notifiedSet.has(rem.id)) {
        triggerSystemNotification(rem);
        markAsLocallyNotified(rem.id);
      }
    });
  }

  function triggerSystemNotification(reminder) {
    const title = `Task Reminder: ${reminder.taskTitle}`;
    const body = `Scheduled task is due now!`;

    if ('Notification' in window && Notification.permission === 'granted') {
      if (navigator.serviceWorker && navigator.serviceWorker.controller) {
        navigator.serviceWorker.ready.then(reg => {
          reg.showNotification(title, {
            body,
            icon: '/icons/icon-192.png',
            data: { taskId: reminder.taskId, reminderId: reminder.id }
          });
        });
      } else {
        new Notification(title, {
          body,
          icon: '/icons/icon-192.png'
        });
      }
    }

    // In-app toast banner as well
    App.showToast(`⏰ Reminder: ${reminder.taskTitle}`, {
      actionText: 'View',
      onAction: () => {
        openNotificationCenter();
      }
    });
  }

  function openNotificationCenter() {
    const modal = document.getElementById('notification-modal');
    const listEl = document.getElementById('notification-list');
    listEl.innerHTML = '';

    if (pendingReminders.length === 0) {
      listEl.innerHTML = `
        <div class="empty-state" style="padding: 32px 16px;">
          <div class="empty-state-icon">🔔</div>
          <h3>No Pending Reminders</h3>
          <p>You're all caught up! New reminders will appear here.</p>
        </div>
      `;
    } else {
      pendingReminders.forEach(rem => {
        const item = document.createElement('div');
        item.className = 'task-card';
        item.style.marginBottom = '10px';

        const remDate = new Date(rem.reminderDatetime).toLocaleString();

        item.innerHTML = `
          <div style="font-size: 1.5rem; flex-shrink: 0;">⏰</div>
          <div style="flex: 1;">
            <div style="font-weight: 600; font-size: 1rem;">${TaskManager.escapeHtml(rem.taskTitle)}</div>
            <div style="font-size: 0.8rem; color: var(--text-muted); margin-top: 2px;">Due: ${remDate}</div>
            <div style="display: flex; gap: 8px; margin-top: 8px;">
              <button class="btn-secondary snooze-btn" style="padding: 4px 10px; font-size: 0.78rem;">Snooze 10m</button>
              <button class="btn-primary complete-btn" style="padding: 4px 10px; font-size: 0.78rem;">Mark Done</button>
              <button class="action-btn dismiss-btn" style="font-size: 0.78rem; padding: 4px 8px;" title="Dismiss">✕ Dismiss</button>
            </div>
          </div>
        `;

        item.querySelector('.snooze-btn').onclick = async () => {
          try {
            await API.snoozeReminder(rem.id, 10);
            App.showToast('Reminder snoozed for 10 minutes');
            await fetchPendingReminders();
            openNotificationCenter();
          } catch (e) {
            App.showToast(e.message, { isError: true });
          }
        };

        item.querySelector('.complete-btn').onclick = async () => {
          try {
            await API.completeTask(rem.taskId);
            await API.dismissReminder(rem.id);
            App.showToast('Task marked as completed');
            await fetchPendingReminders();
            App.refreshCurrentView();
            openNotificationCenter();
          } catch (e) {
            App.showToast(e.message, { isError: true });
          }
        };

        item.querySelector('.dismiss-btn').onclick = async () => {
          try {
            await API.dismissReminder(rem.id);
            await fetchPendingReminders();
            openNotificationCenter();
          } catch (e) {
            App.showToast(e.message, { isError: true });
          }
        };

        listEl.appendChild(item);
      });
    }

    modal.classList.add('active');
  }

  function startPolling() {
    if (pollInterval) clearInterval(pollInterval);
    // Poll every 30 seconds for real-time reminders
    pollInterval = setInterval(() => {
      fetchPendingReminders();
    }, 30000);
  }

  return {
    init,
    fetchPendingReminders,
    requestPermission,
    showTestNotification,
    openNotificationCenter
  };
})();

window.NotificationManager = NotificationManager;

