/**
 * Secure Smart To-Do List - API Client
 * Handles HTTP requests, JWT token attachment, and error formatting.
 */

const API = (() => {
  const TOKEN_KEY = 'secure_todo_jwt';
  const BASE_URL = '/api';

  function getToken() {
    return localStorage.getItem(TOKEN_KEY);
  }

  function setToken(token) {
    if (token) {
      localStorage.setItem(TOKEN_KEY, token);
    } else {
      localStorage.removeItem(TOKEN_KEY);
    }
  }

  function clearToken() {
    localStorage.removeItem(TOKEN_KEY);
  }

  function isAuthenticated() {
    return !!getToken();
  }

  async function request(endpoint, options = {}) {
    const url = `${BASE_URL}${endpoint}`;
    const headers = options.headers || {};

    // Don't set Content-Type for FormData
    if (!(options.body instanceof FormData) && !headers['Content-Type']) {
      headers['Content-Type'] = 'application/json';
    }

    const token = getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const config = {
      ...options,
      headers
    };

    try {
      const response = await fetch(url, config);

      if (response.status === 401) {
        // Unauthorized - session expired or invalid token
        clearToken();
        window.dispatchEvent(new CustomEvent('auth:expired'));
        throw new Error('Your session has expired. Please sign in again.');
      }

      // Handle binary/file downloads (e.g. CSV or JSON export)
      const disposition = response.headers.get('content-disposition');
      if (disposition && disposition.includes('attachment')) {
        const blob = await response.blob();
        return { isBlob: true, blob, response };
      }

      const contentType = response.headers.get('content-type');
      let data = null;
      if (contentType && contentType.includes('application/json')) {
        data = await response.json();
      } else {
        const text = await response.text();
        data = text ? { message: text } : {};
      }

      if (!response.ok) {
        const errorMessage = (data && data.message) ? data.message : `Request failed (${response.status})`;
        throw new Error(errorMessage);
      }

      return data;
    } catch (err) {
      console.error(`API Error [${endpoint}]:`, err);
      throw err;
    }
  }

  return {
    getToken,
    setToken,
    clearToken,
    isAuthenticated,

    // Auth
    register: (data) => request('/auth/register', { method: 'POST', body: JSON.stringify(data) }),
    login: (data) => request('/auth/login', { method: 'POST', body: JSON.stringify(data) }),
    logout: () => {
      clearToken();
      return request('/auth/logout', { method: 'POST' }).catch(() => ({}));
    },
    resetPassword: (data) => request('/auth/reset-password', { method: 'POST', body: JSON.stringify(data) }),
    getMe: () => request('/auth/me'),

    // Tasks
    getTasks: (params = {}) => {
      const query = new URLSearchParams();
      Object.entries(params).forEach(([k, v]) => {
        if (v !== undefined && v !== null && v !== '') query.append(k, v);
      });
      return request(`/tasks?${query.toString()}`);
    },
    getDashboard: () => request('/tasks/dashboard'),
    getTask: (id) => request(`/tasks/${id}`),
    createTask: (data) => request('/tasks', { method: 'POST', body: JSON.stringify(data) }),
    updateTask: (id, data) => request(`/tasks/${id}`, { method: 'PUT', body: JSON.stringify(data) }),
    completeTask: (id) => request(`/tasks/${id}/complete`, { method: 'PATCH' }),
    reopenTask: (id) => request(`/tasks/${id}/reopen`, { method: 'PATCH' }),
    deleteTask: (id) => request(`/tasks/${id}`, { method: 'DELETE' }),
    duplicateTask: (id) => request(`/tasks/${id}/duplicate`, { method: 'POST' }),
    rescheduleTask: (id, newDate, newTime) => {
      const q = new URLSearchParams({ newDate });
      if (newTime) q.append('newTime', newTime);
      return request(`/tasks/${id}/reschedule?${q.toString()}`, { method: 'PATCH' });
    },
    toggleSubtask: (subtaskId) => request(`/tasks/subtasks/${subtaskId}/toggle`, { method: 'PATCH' }),

    // Calendar
    getMonthCalendar: (year, month) => {
      const q = new URLSearchParams();
      if (year) q.append('year', year);
      if (month) q.append('month', month);
      return request(`/calendar/month?${q.toString()}`);
    },
    getYearSummary: (year) => {
      const q = new URLSearchParams();
      if (year) q.append('year', year);
      return request(`/calendar/year?${q.toString()}`);
    },

    // Reminders & Notifications
    getReminders: () => request('/reminders'),
    getPendingReminders: () => request('/reminders/pending'),
    dismissReminder: (id) => request(`/reminders/${id}/dismiss`, { method: 'POST' }),
    snoozeReminder: (id, minutes = 10) => request(`/reminders/${id}/snooze?minutes=${minutes}`, { method: 'POST' }),

    // Categories
    getCategories: () => request('/categories'),
    createCategory: (data) => request('/categories', { method: 'POST', body: JSON.stringify(data) }),
    deleteCategory: (id) => request(`/categories/${id}`, { method: 'DELETE' }),

    // User Profile
    getProfile: () => request('/users/profile'),
    updateProfile: (data) => request('/users/profile', { method: 'PUT', body: JSON.stringify(data) }),
    deleteAccount: () => request('/users/account', { method: 'DELETE' }),

    // Export / Import
    exportJson: () => request('/export/json'),
    exportCsv: () => request('/export/csv'),
    importJson: (formData) => request('/import/json', { method: 'POST', body: formData })
  };
})();

window.API = API;

