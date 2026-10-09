/**
 * Secure Smart To-Do List - Main Application Controller
 * Manages routing, views, auth, themes, and global events.
 */

const App = (() => {
  let currentUser = null;
  let currentView = 'dashboard';
  let toastTimeout = null;

  async function init() {
    initTheme();
    setupNavigation();
    setupGlobalShortcuts();
    registerServiceWorker();

    window.addEventListener('auth:expired', () => {
      showToast('Session expired. Please log in.', { isError: true });
      openAuthModal('login');
    });

    if (API.isAuthenticated()) {
      try {
        currentUser = await API.getMe();
        onUserAuthenticated();
      } catch (e) {
        openAuthModal('login');
      }
    } else {
      openAuthModal('login');
    }
  }

  function initTheme() {
    const savedTheme = localStorage.getItem('secure_todo_theme') || 'system';
    applyTheme(savedTheme);

    const toggleBtn = document.getElementById('theme-toggle-btn');
    if (toggleBtn) {
      toggleBtn.addEventListener('click', () => {
        const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
        const newTheme = currentTheme === 'dark' ? 'light' : 'dark';
        applyTheme(newTheme);
      });
    }
  }

  function applyTheme(theme) {
    let effective = theme;
    if (theme === 'system') {
      effective = window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    }
    document.documentElement.setAttribute('data-theme', effective);
    localStorage.setItem('secure_todo_theme', theme);
    
    const icon = document.getElementById('theme-icon');
    if (icon) {
      icon.textContent = effective === 'dark' ? '☀️' : '🌙';
    }
  }

  function setupNavigation() {
    // Desktop sidebar links
    document.querySelectorAll('.sidebar .nav-item').forEach(link => {
      link.addEventListener('click', (e) => {
        e.preventDefault();
        const view = link.getAttribute('data-view');
        switchView(view);
      });
    });

    // Mobile bottom nav links
    document.querySelectorAll('.mobile-bottom-nav .mobile-nav-item').forEach(link => {
      link.addEventListener('click', (e) => {
        e.preventDefault();
        const view = link.getAttribute('data-view');
        switchView(view);
      });
    });

    // Quick add task buttons (Topbar, Dashboard, FAB)
    const addBtns = [
      document.getElementById('topbar-add-task-btn'),
      document.getElementById('dash-add-task-btn'),
      document.getElementById('mobile-fab-btn')
    ];
    addBtns.forEach(btn => {
      if (btn) btn.addEventListener('click', () => TaskManager.openTaskModal(null, refreshCurrentView));
    });

    // Logout button
    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
      logoutBtn.addEventListener('click', () => {
        API.logout();
        currentUser = null;
        updateUserUI();
        openAuthModal('login');
        showToast('Logged out successfully');
      });
    }

    // Modal close buttons
    document.querySelectorAll('.modal-close-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        document.querySelectorAll('.modal-overlay').forEach(m => m.classList.remove('active'));
      });
    });

    // Filter controls in My Tasks view
    const searchInput = document.getElementById('tasks-search-input');
    if (searchInput) {
      let debounce;
      searchInput.addEventListener('input', (e) => {
        clearTimeout(debounce);
        debounce = setTimeout(() => {
          TaskManager.filterState.keyword = e.target.value.trim();
          loadTasksView();
        }, 300);
      });
    }

    const catFilter = document.getElementById('tasks-category-filter');
    if (catFilter) {
      catFilter.addEventListener('change', (e) => {
        TaskManager.filterState.categoryId = e.target.value;
        loadTasksView();
      });
    }

    const priorityFilter = document.getElementById('tasks-priority-filter');
    if (priorityFilter) {
      priorityFilter.addEventListener('change', (e) => {
        TaskManager.filterState.priority = e.target.value;
        loadTasksView();
      });
    }

    const sortSelect = document.getElementById('tasks-sort-select');
    if (sortSelect) {
      sortSelect.addEventListener('change', (e) => {
        const val = e.target.value;
        const [by, ord] = val.split('_');
        TaskManager.filterState.sortBy = by;
        TaskManager.filterState.sortOrder = ord;
        loadTasksView();
      });
    }
  }

  function setupGlobalShortcuts() {
    window.addEventListener('keydown', (e) => {
      // Don't trigger if user is typing in an input
      if (['INPUT', 'TEXTAREA', 'SELECT'].includes(e.target.tagName)) {
        if (e.key === 'Escape') {
          document.querySelectorAll('.modal-overlay').forEach(m => m.classList.remove('active'));
        }
        return;
      }

      if (e.key === 'n' || e.key === 'N') {
        e.preventDefault();
        TaskManager.openTaskModal(null, refreshCurrentView);
      } else if (e.key === '/') {
        e.preventDefault();
        const search = document.getElementById('tasks-search-input');
        if (search) {
          switchView('tasks');
          search.focus();
        }
      } else if (e.key === 'Escape') {
        document.querySelectorAll('.modal-overlay').forEach(m => m.classList.remove('active'));
      }
    });
  }

  function switchView(viewName) {
    currentView = viewName;

    // Update active nav links
    document.querySelectorAll('.sidebar .nav-item').forEach(el => {
      el.classList.toggle('active', el.getAttribute('data-view') === viewName);
    });
    document.querySelectorAll('.mobile-bottom-nav .mobile-nav-item').forEach(el => {
      el.classList.toggle('active', el.getAttribute('data-view') === viewName);
    });

    // Update title
    const titleMap = {
      dashboard: 'Dashboard',
      tasks: 'My Tasks',
      calendar: 'Calendar & Planning',
      upcoming: 'Upcoming Tasks',
      completed: 'Completed Tasks',
      settings: 'Settings'
    };
    document.getElementById('topbar-view-title').textContent = titleMap[viewName] || 'To-Do List';

    // Show panel
    document.querySelectorAll('.view-panel').forEach(p => p.classList.remove('active'));
    const targetPanel = document.getElementById(`panel-${viewName}`);
    if (targetPanel) targetPanel.classList.add('active');

    // Load data for view
    refreshCurrentView();
  }

  async function onUserAuthenticated() {
    updateUserUI();
    document.getElementById('auth-modal').classList.remove('active');
    await TaskManager.loadCategories();
    NotificationManager.init();
    CalendarManager.init();
    switchView('dashboard');
  }

  function updateUserUI() {
    const nameEl = document.getElementById('user-display-name');
    const emailEl = document.getElementById('user-display-email');
    const avatarEl = document.getElementById('user-avatar-initials');

    if (currentUser) {
      if (nameEl) nameEl.textContent = currentUser.name;
      if (emailEl) emailEl.textContent = currentUser.email;
      if (avatarEl) {
        avatarEl.textContent = currentUser.name.charAt(0).toUpperCase();
      }
    } else {
      if (nameEl) nameEl.textContent = 'Guest';
      if (emailEl) emailEl.textContent = 'Sign in to sync';
      if (avatarEl) avatarEl.textContent = '?';
    }
  }

  function refreshCurrentView() {
    if (!API.isAuthenticated()) return;

    switch (currentView) {
      case 'dashboard':
        loadDashboard();
        break;
      case 'tasks':
        loadTasksView();
        break;
      case 'calendar':
        CalendarManager.render();
        break;
      case 'upcoming':
        loadUpcomingView();
        break;
      case 'completed':
        loadCompletedView();
        break;
      case 'settings':
        loadSettingsView();
        break;
    }
  }

  // Dashboard Loader
  async function loadDashboard() {
    try {
      const data = await API.getDashboard();

      document.getElementById('dash-greeting').textContent = `${data.greeting}, ${currentUser ? currentUser.name : 'there'}!`;
      document.getElementById('dash-date').textContent = data.formattedDate;

      // Progress percentage
      document.getElementById('dash-progress-num').textContent = `${data.dailyProgressPercentage}%`;
      document.getElementById('dash-progress-label').textContent = `${data.todayCompletedCount} of ${data.todayTotalCount} tasks completed`;

      // Metrics
      document.getElementById('metric-today-total').textContent = data.todayTotalCount;
      document.getElementById('metric-today-pending').textContent = data.todayPendingCount;
      document.getElementById('metric-overdue').textContent = data.overdueCount;
      document.getElementById('metric-high-priority').textContent = data.highPriorityCount;

      // 7-Day strip
      renderWeeklyStrip(data.weeklyTaskCounts);

      // Tasks lists
      const todayList = document.getElementById('dash-today-tasks');
      TaskManager.renderTaskList(todayList, data.todayTasks, loadDashboard, 'No tasks scheduled for today.');

      const overdueList = document.getElementById('dash-overdue-tasks');
      TaskManager.renderTaskList(overdueList, data.overdueTasks, loadDashboard, 'No overdue tasks. Great job!');

      const highPriorityList = document.getElementById('dash-urgent-tasks');
      TaskManager.renderTaskList(highPriorityList, data.highPriorityTasks, loadDashboard, 'No high-priority tasks pending.');

    } catch (e) {
      console.error('Failed to load dashboard:', e);
    }
  }

  function renderWeeklyStrip(weeklyCounts) {
    const strip = document.getElementById('dash-weekly-strip');
    if (!strip || !weeklyCounts) return;
    strip.innerHTML = '';

    const todayStr = new Date().toISOString().split('T')[0];

    Object.entries(weeklyCounts).forEach(([dateStr, count]) => {
      const date = new Date(dateStr + 'T00:00:00');
      const dayName = date.toLocaleDateString(undefined, { weekday: 'short' });
      const dayNum = date.getDate();

      const pill = document.createElement('div');
      pill.className = `day-pill ${dateStr === todayStr ? 'active' : ''}`;
      pill.innerHTML = `
        <div class="day-pill-name">${dayName}</div>
        <div class="day-pill-number">${dayNum}</div>
        <span class="day-pill-badge">${count}</span>
      `;

      pill.addEventListener('click', () => {
        // Jump to tasks scheduled for that day
        TaskManager.filterState.date = dateStr;
        switchView('tasks');
      });

      strip.appendChild(pill);
    });
  }

  // My Tasks View Loader
  async function loadTasksView() {
    const list = document.getElementById('tasks-view-list');
    list.innerHTML = '<div style="padding: 40px; text-align: center; color: var(--text-muted);">Loading tasks...</div>';

    try {
      const params = {
        keyword: TaskManager.filterState.keyword,
        categoryId: TaskManager.filterState.categoryId,
        priority: TaskManager.filterState.priority,
        sortBy: TaskManager.filterState.sortBy,
        sortOrder: TaskManager.filterState.sortOrder
      };
      if (TaskManager.filterState.date) {
        params.date = TaskManager.filterState.date;
        TaskManager.filterState.date = null; // reset filter after applying
      }

      const tasks = await API.getTasks(params);
      TaskManager.renderTaskList(list, tasks, loadTasksView, 'No tasks match your filter criteria.');
    } catch (e) {
      list.innerHTML = `<div style="padding: 40px; text-align: center; color: var(--danger);">${e.message}</div>`;
    }
  }

  // Upcoming View Loader
  async function loadUpcomingView() {
    const list = document.getElementById('upcoming-view-list');
    list.innerHTML = '<div style="padding: 40px; text-align: center; color: var(--text-muted);">Loading upcoming tasks...</div>';

    try {
      const today = new Date();
      const tomorrow = new Date(today);
      tomorrow.setDate(tomorrow.getDate() + 1);
      const tomorrowStr = tomorrow.toISOString().split('T')[0];

      // Fetch pending tasks from tomorrow up to any future year
      const tasks = await API.getTasks({
        status: 'PENDING',
        startDate: tomorrowStr,
        endDate: '2099-12-31',
        sortBy: 'scheduledDate',
        sortOrder: 'asc'
      });

      TaskManager.renderTaskList(list, tasks, loadUpcomingView, 'No upcoming tasks found. Plan ahead!');
    } catch (e) {
      list.innerHTML = `<div style="padding: 40px; text-align: center; color: var(--danger);">${e.message}</div>`;
    }
  }

  // Completed View Loader
  async function loadCompletedView() {
    const list = document.getElementById('completed-view-list');
    list.innerHTML = '<div style="padding: 40px; text-align: center; color: var(--text-muted);">Loading completed tasks...</div>';

    try {
      const tasks = await API.getTasks({ status: 'COMPLETED', sortBy: 'completedAt', sortOrder: 'desc' });
      TaskManager.renderTaskList(list, tasks, loadCompletedView, 'No completed tasks yet.');
    } catch (e) {
      list.innerHTML = `<div style="padding: 40px; text-align: center; color: var(--danger);">${e.message}</div>`;
    }
  }

  // Settings View Loader
  async function loadSettingsView() {
    if (!currentUser) return;
    document.getElementById('settings-name-input').value = currentUser.name || '';
    document.getElementById('settings-email-input').value = currentUser.email || '';
    document.getElementById('settings-tz-select').value = currentUser.timezone || 'UTC';
    document.getElementById('settings-theme-select').value = localStorage.getItem('secure_todo_theme') || 'system';

    setupSettingsHandlers();
  }

  function setupSettingsHandlers() {
    // Save profile form
    const profileForm = document.getElementById('settings-profile-form');
    profileForm.onsubmit = async (e) => {
      e.preventDefault();
      const name = document.getElementById('settings-name-input').value.trim();
      const timezone = document.getElementById('settings-tz-select').value;
      const theme = document.getElementById('settings-theme-select').value;

      try {
        currentUser = await API.updateProfile({ name, timezone, theme });
        applyTheme(theme);
        updateUserUI();
        showToast('Profile updated successfully');
      } catch (err) {
        showToast(err.message, { isError: true });
      }
    };

    // Password change form
    const pwdForm = document.getElementById('settings-password-form');
    pwdForm.onsubmit = async (e) => {
      e.preventDefault();
      const currentPassword = document.getElementById('settings-curr-pwd').value;
      const newPassword = document.getElementById('settings-new-pwd').value;

      try {
        await API.updateProfile({ currentPassword, newPassword });
        pwdForm.reset();
        showToast('Password changed successfully');
      } catch (err) {
        showToast(err.message, { isError: true });
      }
    };

    // Category Management
    const catForm = document.getElementById('settings-new-cat-form');
    catForm.onsubmit = async (e) => {
      e.preventDefault();
      const name = document.getElementById('new-cat-name').value.trim();
      const color = document.getElementById('new-cat-color').value;

      try {
        await API.createCategory({ name, color });
        catForm.reset();
        await TaskManager.loadCategories();
        renderSettingsCategories();
        showToast('Category created');
      } catch (err) {
        showToast(err.message, { isError: true });
      }
    };
    renderSettingsCategories();

    // Data Backup Export JSON & CSV
    document.getElementById('export-json-btn').onclick = async () => {
      try {
        const res = await API.exportJson();
        if (res.isBlob) {
          downloadBlob(res.blob, `tasks-backup-${new Date().toISOString().split('T')[0]}.json`);
        }
        showToast('JSON Backup downloaded');
      } catch (e) {
        showToast(e.message, { isError: true });
      }
    };

    document.getElementById('export-csv-btn').onclick = async () => {
      try {
        const res = await API.exportCsv();
        if (res.isBlob) {
          downloadBlob(res.blob, `tasks-backup-${new Date().toISOString().split('T')[0]}.csv`);
        }
        showToast('CSV Backup downloaded');
      } catch (e) {
        showToast(e.message, { isError: true });
      }
    };

    // Import JSON
    const importInput = document.getElementById('import-file-input');
    importInput.onchange = async () => {
      if (!importInput.files || importInput.files.length === 0) return;
      const file = importInput.files[0];
      const formData = new FormData();
      formData.append('file', file);

      try {
        const res = await API.importJson(formData);
        showToast(res.message || 'Tasks imported successfully');
        importInput.value = '';
        refreshCurrentView();
      } catch (e) {
        showToast(e.message, { isError: true });
      }
    };

    // Delete Account
    document.getElementById('delete-account-btn').onclick = () => {
      confirmModal('DANGER: Are you sure you want to permanently delete your account and all tasks? This action cannot be undone.', async () => {
        try {
          await API.deleteAccount();
          API.clearToken();
          currentUser = null;
          updateUserUI();
          openAuthModal('login');
          showToast('Account deleted.');
        } catch (e) {
          showToast(e.message, { isError: true });
        }
      });
    };
  }

  async function renderSettingsCategories() {
    const list = document.getElementById('settings-categories-list');
    list.innerHTML = '';
    const cats = await API.getCategories();

    cats.forEach(c => {
      const row = document.createElement('div');
      row.style.cssText = 'display: flex; align-items: center; justify-content: space-between; padding: 6px 12px; background: var(--bg-tertiary); border-radius: var(--radius-sm); margin-bottom: 6px;';
      row.innerHTML = `
        <span style="display: flex; align-items: center; gap: 8px;">
          <span style="width: 12px; height: 12px; border-radius: 50%; background-color: ${c.color};"></span>
          <span>${TaskManager.escapeHtml(c.name)}</span>
          ${c.default ? '<span style="font-size: 0.7rem; color: var(--text-muted);">(preset)</span>' : ''}
        </span>
        ${!c.default ? `<button type="button" class="action-btn" title="Delete" style="color: var(--danger);">🗑️</button>` : ''}
      `;

      if (!c.default) {
        row.querySelector('button').onclick = async () => {
          try {
            await API.deleteCategory(c.id);
            await TaskManager.loadCategories();
            renderSettingsCategories();
            showToast('Category deleted');
          } catch (e) {
            showToast(e.message, { isError: true });
          }
        };
      }

      list.appendChild(row);
    });
  }

  function downloadBlob(blob, filename) {
    const url = window.URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    window.URL.revokeObjectURL(url);
  }

  // Toast System
  function showToast(message, options = {}) {
    const container = document.getElementById('toast-container');
    const toast = document.createElement('div');
    toast.className = 'toast';
    if (options.isError) toast.style.backgroundColor = 'var(--danger)';

    toast.innerHTML = `
      <span>${TaskManager.escapeHtml(message)}</span>
      ${options.actionText ? `<button class="toast-btn">${TaskManager.escapeHtml(options.actionText)}</button>` : ''}
    `;

    if (options.onAction && options.actionText) {
      toast.querySelector('.toast-btn').onclick = () => {
        options.onAction();
        toast.remove();
      };
    }

    container.appendChild(toast);

    setTimeout(() => {
      toast.style.opacity = '0';
      toast.style.transition = 'opacity 0.3s ease';
      setTimeout(() => toast.remove(), 300);
    }, options.duration || 4000);
  }

  // Confirmation Modal
  function confirmModal(message, onConfirm) {
    const modal = document.getElementById('confirm-modal');
    document.getElementById('confirm-modal-msg').textContent = message;
    document.getElementById('confirm-modal-yes').onclick = () => {
      modal.classList.remove('active');
      if (onConfirm) onConfirm();
    };
    modal.classList.add('active');
  }

  // Auth Modal
  function openAuthModal(tab = 'login') {
    const modal = document.getElementById('auth-modal');
    modal.classList.add('active');
    switchAuthTab(tab);
    setupAuthForms();
  }

  function switchAuthTab(tab) {
    document.querySelectorAll('.auth-tab-btn').forEach(b => {
      b.classList.toggle('active', b.getAttribute('data-tab') === tab);
    });
    document.querySelectorAll('.auth-tab-content').forEach(c => {
      c.style.display = (c.id === `auth-tab-${tab}`) ? 'block' : 'none';
    });
  }

  function setupAuthForms() {
    // Tab toggles
    document.querySelectorAll('.auth-tab-btn').forEach(btn => {
      btn.onclick = () => switchAuthTab(btn.getAttribute('data-tab'));
    });

    // Login Form
    const loginForm = document.getElementById('login-form');
    loginForm.onsubmit = async (e) => {
      e.preventDefault();
      const email = document.getElementById('login-email').value.trim();
      const password = document.getElementById('login-password').value;

      try {
        const res = await API.login({ email, password });
        API.setToken(res.token);
        currentUser = res;
        loginForm.reset();
        onUserAuthenticated();
        showToast(`Welcome back, ${res.name}!`);
      } catch (err) {
        showToast(err.message, { isError: true });
      }
    };

    // Register Form
    const regForm = document.getElementById('register-form');
    regForm.onsubmit = async (e) => {
      e.preventDefault();
      const name = document.getElementById('reg-name').value.trim();
      const email = document.getElementById('reg-email').value.trim();
      const password = document.getElementById('reg-password').value;
      const timezone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';

      try {
        const res = await API.register({ name, email, password, timezone });
        API.setToken(res.token);
        currentUser = res;
        regForm.reset();
        onUserAuthenticated();
        showToast(`Account created! Welcome, ${res.name}!`);
      } catch (err) {
        showToast(err.message, { isError: true });
      }
    };

    // Password Reset Form
    const resetForm = document.getElementById('reset-form');
    resetForm.onsubmit = async (e) => {
      e.preventDefault();
      const email = document.getElementById('reset-email').value.trim();
      const newPassword = document.getElementById('reset-password').value;

      try {
        await API.resetPassword({ email, newPassword });
        resetForm.reset();
        switchAuthTab('login');
        showToast('Password updated! Please log in with your new password.');
      } catch (err) {
        showToast(err.message, { isError: true });
      }
    };
  }

  // Register PWA Service Worker
  function registerServiceWorker() {
    if ('serviceWorker' in navigator) {
      window.addEventListener('load', () => {
        navigator.serviceWorker.register('/service-worker.js')
          .then(reg => {
            console.log('[PWA] ServiceWorker registered with scope:', reg.scope);
          })
          .catch(err => {
            console.warn('[PWA] ServiceWorker registration failed:', err);
          });
      });
    }
  }

  return {
    init,
    showToast,
    confirmModal,
    switchView,
    refreshCurrentView
  };
})();

// Start App once DOM is loaded
document.addEventListener('DOMContentLoaded', () => {
  App.init();
});

window.App = App;

