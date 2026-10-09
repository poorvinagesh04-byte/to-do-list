/**
 * Secure Smart To-Do List - Calendar & Yearly Planning Module
 */

const CalendarManager = (() => {
  let currentYear = new Date().getFullYear();
  let currentMonth = new Date().getMonth() + 1; // 1-12
  let currentViewMode = 'month'; // 'month' or 'year'
  let cachedMonthData = [];

  const MONTH_NAMES = [
    'January', 'February', 'March', 'April', 'May', 'June',
    'July', 'August', 'September', 'October', 'November', 'December'
  ];

  async function init() {
    setupEventListeners();
    await render();
  }

  function setupEventListeners() {
    document.getElementById('cal-prev-btn').addEventListener('click', () => {
      if (currentViewMode === 'month') {
        currentMonth--;
        if (currentMonth < 1) {
          currentMonth = 12;
          currentYear--;
        }
      } else {
        currentYear--;
      }
      render();
    });

    document.getElementById('cal-next-btn').addEventListener('click', () => {
      if (currentViewMode === 'month') {
        currentMonth++;
        if (currentMonth > 12) {
          currentMonth = 1;
          currentYear++;
        }
      } else {
        currentYear++;
      }
      render();
    });

    document.getElementById('cal-today-btn').addEventListener('click', () => {
      const now = new Date();
      currentYear = now.getFullYear();
      currentMonth = now.getMonth() + 1;
      render();
    });

    document.getElementById('cal-view-toggle').addEventListener('change', (e) => {
      currentViewMode = e.target.value;
      render();
    });
  }

  async function render() {
    const titleEl = document.getElementById('cal-header-title');
    const monthViewContainer = document.getElementById('cal-month-view');
    const yearViewContainer = document.getElementById('cal-year-view');

    if (currentViewMode === 'month') {
      titleEl.textContent = `${MONTH_NAMES[currentMonth - 1]} ${currentYear}`;
      monthViewContainer.style.display = 'block';
      yearViewContainer.style.display = 'none';
      await renderMonthView();
    } else {
      titleEl.textContent = `Yearly Planning — ${currentYear}`;
      monthViewContainer.style.display = 'none';
      yearViewContainer.style.display = 'block';
      await renderYearView();
    }
  }

  async function renderMonthView() {
    const grid = document.getElementById('cal-grid-cells');
    grid.innerHTML = '<div style="grid-column: span 7; padding: 40px; text-align: center; color: var(--text-muted);">Loading calendar...</div>';

    try {
      cachedMonthData = await API.getMonthCalendar(currentYear, currentMonth);
      grid.innerHTML = '';

      // Determine day of week of the 1st day of the month
      const firstDay = new Date(currentYear, currentMonth - 1, 1).getDay(); // 0 (Sun) to 6 (Sat)
      const daysInPrevMonth = new Date(currentYear, currentMonth - 1, 0).getDate();

      // Leading blank / previous month cells
      for (let i = firstDay - 1; i >= 0; i--) {
        const cell = document.createElement('div');
        cell.className = 'calendar-day-cell other-month';
        cell.style.opacity = '0.35';
        cell.innerHTML = `
          <div class="day-cell-top">
            <span class="day-num">${daysInPrevMonth - i}</span>
          </div>
        `;
        grid.appendChild(cell);
      }

      const today = new Date();
      const isCurrentMonth = (today.getFullYear() === currentYear && (today.getMonth() + 1) === currentMonth);
      const todayDate = today.getDate();

      // Render days of the current month
      cachedMonthData.forEach(day => {
        const cell = document.createElement('div');
        const isToday = isCurrentMonth && day.dayOfMonth === todayDate;
        cell.className = `calendar-day-cell ${isToday ? 'today' : ''}`;
        cell.setAttribute('data-date', day.date);

        let pillsHtml = '';
        if (day.tasks && day.tasks.length > 0) {
          day.tasks.slice(0, 3).forEach(t => {
            pillsHtml += `
              <div class="cal-task-pill ${t.status === 'COMPLETED' ? 'completed' : ''}" 
                   style="border-left-color: ${t.categoryColor || 'var(--primary)'};" 
                   title="${TaskManager.escapeHtml(t.title)}">
                ${TaskManager.escapeHtml(t.title)}
              </div>
            `;
          });
          if (day.tasks.length > 3) {
            pillsHtml += `<div style="font-size: 0.65rem; color: var(--text-muted); text-align: center;">+${day.tasks.length - 3} more</div>`;
          }
        }

        cell.innerHTML = `
          <div class="day-cell-top">
            <span class="day-num">${day.dayOfMonth}</span>
            ${day.totalCount > 0 ? `<span class="day-task-count ${day.hasHighPriority ? 'high-priority' : ''}">${day.totalCount}</span>` : ''}
          </div>
          <div class="day-pills-list">${pillsHtml}</div>
          <div class="day-dots">
            ${day.tasks ? day.tasks.slice(0, 5).map(t => `<span class="day-dot" style="background-color: ${t.categoryColor || 'var(--primary)'};"></span>`).join('') : ''}
          </div>
        `;

        // Click day to view/add tasks for this date
        cell.addEventListener('click', () => openDayDetail(day));

        grid.appendChild(cell);
      });

      // Trailing days to fill out the last grid row
      const totalCells = firstDay + cachedMonthData.length;
      const remainingCells = (7 - (totalCells % 7)) % 7;
      for (let j = 1; j <= remainingCells; j++) {
        const cell = document.createElement('div');
        cell.className = 'calendar-day-cell other-month';
        cell.style.opacity = '0.35';
        cell.innerHTML = `
          <div class="day-cell-top">
            <span class="day-num">${j}</span>
          </div>
        `;
        grid.appendChild(cell);
      }

    } catch (err) {
      grid.innerHTML = `<div style="grid-column: span 7; padding: 40px; text-align: center; color: var(--danger);">Failed to load calendar: ${err.message}</div>`;
    }
  }

  async function renderYearView() {
    const grid = document.getElementById('cal-year-grid');
    grid.innerHTML = '<div style="grid-column: span 3; padding: 40px; text-align: center; color: var(--text-muted);">Loading yearly overview...</div>';

    try {
      const summary = await API.getYearSummary(currentYear);
      grid.innerHTML = '';

      summary.months.forEach(m => {
        const card = document.createElement('div');
        card.className = 'month-summary-card';
        const progress = m.totalTasks > 0 ? Math.round((m.completedTasks / m.totalTasks) * 100) : 0;

        card.innerHTML = `
          <div style="display: flex; justify-content: space-between; align-items: baseline;">
            <h4>${m.monthName}</h4>
            <span style="font-size: 0.85rem; font-weight: 700; color: var(--primary);">${m.totalTasks} tasks</span>
          </div>
          <div style="display: flex; justify-content: space-between; font-size: 0.8rem; color: var(--text-muted); margin: 8px 0 4px;">
            <span>Completed: ${m.completedTasks}</span>
            <span>Pending: ${m.pendingTasks}</span>
          </div>
          <div style="width: 100%; height: 6px; background-color: var(--bg-tertiary); border-radius: 3px; overflow: hidden;">
            <div style="width: ${progress}%; height: 100%; background-color: var(--success); transition: width 0.3s ease;"></div>
          </div>
          <p style="font-size: 0.72rem; color: var(--text-muted); text-align: right; margin-top: 4px;">${progress}% done</p>
        `;

        card.addEventListener('click', () => {
          currentMonth = m.month;
          currentViewMode = 'month';
          document.getElementById('cal-view-toggle').value = 'month';
          render();
        });

        grid.appendChild(card);
      });
    } catch (err) {
      grid.innerHTML = `<div style="grid-column: span 3; padding: 40px; text-align: center; color: var(--danger);">${err.message}</div>`;
    }
  }

  function openDayDetail(day) {
    const modal = document.getElementById('day-detail-modal');
    const titleEl = document.getElementById('day-modal-title');
    const listEl = document.getElementById('day-tasks-list');
    const dateObj = new Date(day.date + 'T00:00:00');

    titleEl.textContent = dateObj.toLocaleDateString(undefined, { weekday: 'long', month: 'long', day: 'numeric', year: 'numeric' });
    
    // Setup "+ Add Task for this day" button
    document.getElementById('day-add-task-btn').onclick = () => {
      modal.classList.remove('active');
      TaskManager.openTaskModal(null, () => render());
      // Pre-fill date
      document.getElementById('task-date-input').value = day.date;
    };

    TaskManager.renderTaskList(listEl, day.tasks, () => {
      // Re-render when task status toggles
      render();
    }, 'No tasks scheduled for this day.');

    modal.classList.add('active');
  }

  return {
    init,
    render,
    currentYear,
    currentMonth
  };
})();

window.CalendarManager = CalendarManager;

