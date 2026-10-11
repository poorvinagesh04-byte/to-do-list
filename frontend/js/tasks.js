/**
 * Secure Smart To-Do List - Task Management Module
 */

const TaskManager = (() => {
  let categoriesCache = [];
  let currentTasks = [];
  let filterState = {
    keyword: '',
    categoryId: '',
    priority: '',
    status: '',
    sortBy: 'scheduledDate',
    sortOrder: 'asc'
  };

  async function loadCategories() {
    try {
      categoriesCache = await API.getCategories();
      renderCategoryOptions();
    } catch (e) {
      console.warn('Failed to load categories:', e);
    }
  }

  function renderCategoryOptions() {
    const selects = document.querySelectorAll('.category-select');
    selects.forEach(select => {
      const currentValue = select.value;
      const isFilter = select.classList.contains('filter-select');
      
      select.innerHTML = isFilter 
        ? '<option value="">All Categories</option>'
        : '<option value="">Select Category (Optional)</option>';

      categoriesCache.forEach(cat => {
        const opt = document.createElement('option');
        opt.value = cat.id;
        opt.textContent = cat.name;
        select.appendChild(opt);
      });

      if (currentValue) select.value = currentValue;
    });
  }

  function getCategoryById(id) {
    return categoriesCache.find(c => c.id === id);
  }

  function formatDisplayDate(dateStr) {
    if (!dateStr) return '';
    const date = new Date(dateStr + 'T00:00:00');
    const today = new Date();
    today.setHours(0, 0, 0, 0);

    const diffDays = Math.round((date - today) / (1000 * 60 * 60 * 24));
    if (diffDays === 0) return 'Today';
    if (diffDays === 1) return 'Tomorrow';
    if (diffDays === -1) return 'Yesterday';

    return date.toLocaleDateString(undefined, { month: 'short', day: 'numeric', year: 'numeric' });
  }

  function formatTime(timeStr) {
    if (!timeStr) return '';
    const [h, m] = timeStr.split(':');
    const d = new Date();
    d.setHours(parseInt(h, 10), parseInt(m, 10));
    return d.toLocaleTimeString(undefined, { hour: 'numeric', minute: '2-digit' });
  }

  function createTaskElement(task, onUpdateCallback) {
    const card = document.createElement('div');
    card.className = `task-card ${task.status === 'COMPLETED' ? 'completed' : ''}`;
    card.id = `task-card-${task.id}`;

    const isCompleted = task.status === 'COMPLETED';

    // Subtask count
    const totalSubtasks = task.subtasks ? task.subtasks.length : 0;
    const completedSubtasks = task.subtasks ? task.subtasks.filter(s => s.completed).length : 0;

    card.innerHTML = `
      <label class="custom-checkbox" title="${isCompleted ? 'Mark as pending' : 'Mark as completed'}">
        <input type="checkbox" ${isCompleted ? 'checked' : ''} aria-label="Complete task: ${escapeHtml(task.title)}" />
        <span class="checkbox-visual"></span>
      </label>
      <div class="task-content">
        <div class="task-title-row">
          <span class="task-title">${escapeHtml(task.title)}</span>
          <div class="task-actions">
            <button class="action-btn edit-btn" title="Edit Task" aria-label="Edit task">✏️</button>
            <button class="action-btn duplicate-btn" title="Duplicate Task" aria-label="Duplicate task">📋</button>
            <button class="action-btn delete-btn" title="Delete Task" aria-label="Delete task">🗑️</button>
          </div>
        </div>
        ${task.description ? `<p class="task-desc">${escapeHtml(task.description)}</p>` : ''}
        <div class="task-meta-row">
          <span class="tag priority-badge ${task.priority}">${task.priority}</span>
          ${task.categoryName ? `<span class="tag" style="background-color: ${task.categoryColor}22; color: ${task.categoryColor}; border: 1px solid ${task.categoryColor}55;">🏷️ ${escapeHtml(task.categoryName)}</span>` : ''}
          <span class="tag" style="background-color: var(--bg-tertiary); color: var(--text-secondary);">📅 ${formatDisplayDate(task.scheduledDate)}${task.startTime ? ' at ' + formatTime(task.startTime) : ''}</span>
          ${totalSubtasks > 0 ? `<span class="tag" style="background-color: var(--bg-tertiary); color: var(--text-secondary);">☑️ ${completedSubtasks}/${totalSubtasks} subtasks</span>` : ''}
          ${task.recurrenceType && task.recurrenceType !== 'NONE' ? `<span class="tag" style="background-color: var(--info-light); color: var(--info);">🔁 ${task.recurrenceType.toLowerCase()}</span>` : ''}
          ${task.isOverdue ? `<span class="tag" style="background-color: var(--danger-light); color: var(--danger); font-weight: 700;">⚠️ Overdue</span>` : ''}
        </div>
      </div>
    `;

    // Checkbox toggle logic
    const checkbox = card.querySelector('input[type="checkbox"]');
    checkbox.addEventListener('change', async () => {
      const willComplete = checkbox.checked;
      // Optimistic update
      card.classList.toggle('completed', willComplete);

      try {
        if (willComplete) {
          await API.completeTask(task.id);
          App.showToast(`Completed "${task.title}"`, {
            actionText: 'Undo',
            onAction: async () => {
              try {
                await API.reopenTask(task.id);
                checkbox.checked = false;
                card.classList.remove('completed');
                if (onUpdateCallback) onUpdateCallback();
                App.showToast('Task marked pending');
              } catch (err) {
                App.showToast('Failed to undo completion: ' + err.message, { isError: true });
              }
            }
          });
        } else {
          await API.reopenTask(task.id);
          App.showToast(`Marked "${task.title}" as pending`);
        }
        if (onUpdateCallback) onUpdateCallback();
      } catch (err) {
        // Rollback on failure
        checkbox.checked = !willComplete;
        card.classList.toggle('completed', !willComplete);
        App.showToast(`Error: ${err.message}`, { isError: true });
      }
    });

    // Action buttons
    card.querySelector('.edit-btn').addEventListener('click', () => openTaskModal(task, onUpdateCallback));
    card.querySelector('.duplicate-btn').addEventListener('click', async () => {
      try {
        await API.duplicateTask(task.id);
        App.showToast('Task duplicated successfully');
        if (onUpdateCallback) onUpdateCallback();
      } catch (err) {
        App.showToast(err.message, { isError: true });
      }
    });
    card.querySelector('.delete-btn').addEventListener('click', () => {
      App.confirmModal(`Delete task "${task.title}"?`, async () => {
        try {
          await API.deleteTask(task.id);
          card.remove();
          App.showToast('Task deleted');
          if (onUpdateCallback) onUpdateCallback();
        } catch (err) {
          App.showToast(err.message, { isError: true });
        }
      });
    });

    return card;
  }

  function renderTaskList(container, tasks, onUpdateCallback, emptyMessage = 'No tasks found') {
    container.innerHTML = '';
    if (!tasks || tasks.length === 0) {
      container.innerHTML = `
        <div class="empty-state">
          <div class="empty-state-icon">📝</div>
          <h3>${emptyMessage}</h3>
          <p>Create a task to get organized and stay on track.</p>
        </div>
      `;
      return;
    }

    tasks.forEach(task => {
      const el = createTaskElement(task, onUpdateCallback);
      container.appendChild(el);
    });
  }

  // Modal Handling
  let currentSubtasks = [];

  function openTaskModal(existingTask = null, onSavedCallback = null) {
    const modal = document.getElementById('task-modal');
    const titleEl = document.getElementById('task-modal-title');
    const form = document.getElementById('task-form');

    form.reset();
    currentSubtasks = [];

    if (existingTask) {
      titleEl.textContent = 'Edit Task';
      document.getElementById('task-id-input').value = existingTask.id;
      document.getElementById('task-title-input').value = existingTask.title || '';
      document.getElementById('task-desc-input').value = existingTask.description || '';
      document.getElementById('task-date-input').value = existingTask.scheduledDate || '';
      document.getElementById('task-time-input').value = existingTask.startTime || '';
      document.getElementById('task-deadline-input').value = existingTask.deadline ? existingTask.deadline.substring(0, 16) : '';
      document.getElementById('task-priority-input').value = existingTask.priority || 'MEDIUM';
      document.getElementById('task-category-input').value = existingTask.categoryId || '';
      document.getElementById('task-recurrence-input').value = existingTask.recurrenceType || 'NONE';
      document.getElementById('task-interval-input').value = existingTask.recurrenceInterval || 1;
      document.getElementById('task-recurrence-end-input').value = existingTask.recurrenceEndDate || '';

      if (existingTask.subtasks) {
        currentSubtasks = existingTask.subtasks.map(s => ({ title: s.title, completed: s.completed }));
      }

      // Pre-select reminder checkboxes if existing
      if (existingTask.reminders) {
        const offsets = existingTask.reminders.map(r => r.offsetMinutes);
        document.querySelectorAll('input[name="reminder-offset"]').forEach(cb => {
          cb.checked = offsets.includes(parseInt(cb.value, 10));
        });
      }
    } else {
      titleEl.textContent = 'Create New Task';
      document.getElementById('task-id-input').value = '';
      // Default scheduled date to today
      const todayStr = new Date().toISOString().split('T')[0];
      document.getElementById('task-date-input').value = todayStr;
      document.getElementById('task-priority-input').value = 'MEDIUM';
      // Default reminder at scheduled time
      document.querySelectorAll('input[name="reminder-offset"]').forEach(cb => {
        cb.checked = (cb.value === '0');
      });
    }

    renderSubtasksList();
    toggleRecurrenceFields();

    form.onsubmit = async (e) => {
      e.preventDefault();
      const title = document.getElementById('task-title-input').value.trim();
      const scheduledDate = document.getElementById('task-date-input').value;

      if (!title || !scheduledDate) {
        App.showToast('Title and scheduled date are required.', { isError: true });
        return;
      }

      const reminderOffsets = Array.from(document.querySelectorAll('input[name="reminder-offset"]:checked'))
        .map(cb => parseInt(cb.value, 10));

      const payload = {
        title,
        description: document.getElementById('task-desc-input').value.trim(),
        scheduledDate,
        startTime: document.getElementById('task-time-input').value || null,
        deadline: document.getElementById('task-deadline-input').value ? document.getElementById('task-deadline-input').value + ':00' : null,
        priority: document.getElementById('task-priority-input').value,
        categoryId: document.getElementById('task-category-input').value ? parseInt(document.getElementById('task-category-input').value, 10) : null,
        recurrenceType: document.getElementById('task-recurrence-input').value,
        recurrenceInterval: parseInt(document.getElementById('task-interval-input').value, 10) || 1,
        recurrenceEndDate: document.getElementById('task-recurrence-end-input').value || null,
        subtasks: currentSubtasks,
        reminderOffsets
      };

      try {
        const taskId = document.getElementById('task-id-input').value;
        if (taskId) {
          await API.updateTask(taskId, payload);
          App.showToast('Task updated successfully');
        } else {
          await API.createTask(payload);
          App.showToast('Task created successfully');
        }

        closeTaskModal();
        if (onSavedCallback) onSavedCallback();
      } catch (err) {
        App.showToast(err.message, { isError: true });
      }
    };

    modal.classList.add('active');
  }

  function closeTaskModal() {
    const modal = document.getElementById('task-modal');
    modal.classList.remove('active');
  }

  function renderSubtasksList() {
    const container = document.getElementById('subtasks-container');
    container.innerHTML = '';
    currentSubtasks.forEach((st, idx) => {
      const row = document.createElement('div');
      row.className = 'subtask-item';
      row.innerHTML = `
        <span>${escapeHtml(st.title)}</span>
        <button type="button" class="action-btn" title="Remove" aria-label="Remove subtask" style="color: var(--danger);">✕</button>
      `;
      row.querySelector('button').addEventListener('click', () => {
        currentSubtasks.splice(idx, 1);
        renderSubtasksList();
      });
      container.appendChild(row);
    });
  }

  function addSubtask() {
    const input = document.getElementById('new-subtask-input');
    const val = input.value.trim();
    if (val) {
      currentSubtasks.push({ title: val, completed: false });
      input.value = '';
      renderSubtasksList();
    }
  }

  function toggleRecurrenceFields() {
    const val = document.getElementById('task-recurrence-input').value;
    const extraFields = document.getElementById('recurrence-extra-fields');
    if (val && val !== 'NONE') {
      extraFields.style.display = 'grid';
    } else {
      extraFields.style.display = 'none';
    }
  }

  function escapeHtml(str) {
    if (!str) return '';
    return str
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
      .replace(/"/g, '&quot;')
      .replace(/'/g, '&#039;');
  }

  return {
    loadCategories,
    getCategoryById,
    createTaskElement,
    renderTaskList,
    openTaskModal,
    closeTaskModal,
    addSubtask,
    toggleRecurrenceFields,
    filterState,
    escapeHtml
  };
})();

window.TaskManager = TaskManager;

