# 🔒 Secure Smart To-Do List and Reminder Application

A complete, production-grade, secure, and mobile-installable To-Do List and Reminder Application built with **Vanilla HTML5, CSS3, JavaScript** and **Java Spring Boot 3**.

---

## 🌟 Key Features

1. **Clean & Modern UI/UX**:
   - Minimalist, professional productivity design.
   - 6 Core Views: **Dashboard**, **My Tasks**, **Calendar & Yearly Planning**, **Upcoming**, **Completed**, and **Settings**.
   - Mobile-first layout with bottom navigation bar on mobile and sleek collapsible sidebar on desktop.
   - Smooth **Light Mode & Dark Mode** with instant theme toggle and system preference detection.
   - Accessibility-first with full keyboard navigation (`N` for New Task, `/` for search, `Esc` to close modals), focus outlines, and screen-reader ARIA tags.

2. **Smart Task Management**:
   - Supports scheduling tasks for **today, tomorrow, or years into the future (e.g., 2027, 2030+)**.
   - Interactive checkbox completion with instant UI update, optimistic UI, rollback on failure, and one-click **Undo**.
   - Priorities: `Low`, `Medium`, `High`, `Urgent`.
   - Categories: `College`, `Work`, `Personal`, `Health`, `Shopping`, and custom user-created categories.
   - Dynamic interactive subtasks checklist per task.
   - Full recurrence engine: `Daily`, `Weekly`, `Monthly`, `Yearly` with custom intervals and end dates.
   - Task duplication, rescheduling, and deletion with confirmation dialog.

3. **Calendar & Multi-Year Planning**:
   - **Monthly View**: Interactive calendar grid with task pills, day count badges, priority dots, and date selection.
   - **Yearly View**: 12-month annual overview showing progress bars, completed vs pending metrics, and instant month navigation.
   - Seamless navigation between months and years into the distant future.

4. **Real Persistent Notifications & Schedulers**:
   - Real backend scheduler (`@Scheduled`) checking due reminders every 30 seconds.
   - Persistent SQL records: reminders survive server reboots and restart downtime seamlessly.
   - Deduplication key prevents duplicate notifications across re-runs.
   - Automatic cancellation of pending reminders when a task is marked complete.
   - In-app Notification Center bell with live unread counter, "Snooze 10m", "Mark Done", and "Dismiss".
   - Browser Notifications & Web Push integration with Service Worker actions.

5. **Security Built-In (OWASP Top 10 Defenses)**:
   - **Authentication**: JWT tokens (HMAC-SHA256) with secure expiry.
   - **Password Security**: BCrypt password hashing (strength 12).
   - **Strict Data Isolation (IDOR Prevention)**: All queries and mutations strictly verify ownership (`WHERE user_id = currentUser.getId()`). A user can never view or modify another user's tasks.
   - **Rate Limiting**: Sliding window rate limiting on sensitive endpoints (`/api/auth/login`, `/api/auth/register`, `/api/auth/reset-password`).
   - **SQL Injection Prevention**: 100% parameterized JPA/Hibernate queries.
   - **XSS & CORS Protection**: HTML escaping on clientside, CORS origin restrictions, security response headers.

6. **Progressive Web App (PWA)**:
   - Web App Manifest (`manifest.json`) and Service Worker (`service-worker.js`).
   - Installable on Android (Chrome/Edge/Brave) and iPhone (Safari "Add to Home Screen").
   - Offline caching of assets and offline friendly status.

---

## 🛠️ Technology Stack

- **Frontend**: HTML5, CSS3, Vanilla JavaScript (Zero React/Vue/Angular dependencies).
- **Backend**: Java 21 or newer, Spring Boot 3.3.4, Spring Security 6, Spring Data JPA, Hibernate, Bean Validation.
- **Database**:
  - **H2 Persistent File-Backed Database** (Default profile: zero-configuration, runs immediately).
  - **MySQL 8.0+** (`-Dspring.profiles.active=mysql`).
  - **PostgreSQL 14+** (`-Dspring.profiles.active=postgres`).
- **Documentation**: OpenAPI 3.0 / Swagger UI (`/swagger-ui.html`).

---

## 🚀 Quick Start Guide

### 1. Prerequisites
- Java Development Kit (JDK 21 or newer).
- Apache Maven 3.9+ (or use the included `./mvnw.cmd` wrapper).

### 2. Run with Zero Configuration (Out of the Box)
From the project root:
```powershell
.\mvnw.cmd -pl backend spring-boot:run
```
The application will launch on **`http://localhost:8080`**.

Open your browser and navigate to:
- **Application UI**: [http://localhost:8080](http://localhost:8080)
- **Swagger API Docs**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **H2 Database Console**: [http://localhost:8080/h2-console](http://localhost:8080/h2-console) (JDBC URL: `jdbc:h2:file:./data/tododb`, User: `sa`, Password: empty)

---

## 🗄️ Database Setup (MySQL & PostgreSQL)

The database schema DDL is located in [`database/schema.sql`](file:///c:/Users/User/TO%20DO%20LIST/database/schema.sql).

### Running with MySQL:
1. Initialize the database:
   ```bash
   mysql -u root -p < database/mysql-init.sql
   ```
2. Start the application with MySQL profile:
   ```powershell
   .\mvnw.cmd -pl backend spring-boot:run -Dspring-boot.run.profiles=mysql
   ```
   *(Configure `SPRING_DATASOURCE_USERNAME` and `SPRING_DATASOURCE_PASSWORD` if your credentials differ from root/root).*

### Running with PostgreSQL:
1. Initialize the database:
   ```bash
   psql -U postgres -f database/postgres-init.sql
   ```
2. Start the application with PostgreSQL profile:
   ```powershell
   .\mvnw.cmd -pl backend spring-boot:run -Dspring-boot.run.profiles=postgres
   ```

---

## 📱 Mobile Installation (PWA) & Notifications

### On Android (Chrome / Brave / Edge)
1. Open `http://<your-host-ip>:8080` or production HTTPS domain in Chrome on your Android phone.
2. Tap the browser menu (⋮) -> Tap **"Install app"** or **"Add to Home screen"**.
3. The app icon will appear on your home screen and open in standalone fullscreen mode like a native app.
4. When prompted, tap **"Allow"** to enable real push and task notifications.

### On iPhone / iPad (Safari)
1. Open `https://<your-host-domain>` in Safari.
2. Tap the **Share** button (box with upward arrow).
3. Scroll down and tap **"Add to Home Screen"**.
4. Launch the app from your home screen.
5. In iOS 16.4+, web push notifications are supported for apps added to the home screen. Go to **Settings** in the app and tap **"Request Notification Permission"**.

---

## 📋 REST API Endpoints Overview

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/auth/register` | Register new user account |
| `POST` | `/api/auth/login` | Authenticate and obtain JWT |
| `POST` | `/api/auth/logout` | Invalidate session |
| `POST` | `/api/auth/reset-password` | Password reset |
| `GET` | `/api/auth/me` | Current user profile |
| `GET` | `/api/tasks` | Get tasks with filters & sorting |
| `GET` | `/api/tasks/dashboard` | Dashboard metrics & 7-day overview |
| `POST` | `/api/tasks` | Create scheduled task |
| `GET` | `/api/tasks/{id}` | Get task details |
| `PUT` | `/api/tasks/{id}` | Update task |
| `PATCH` | `/api/tasks/{id}/complete` | Mark task as completed |
| `PATCH` | `/api/tasks/{id}/reopen` | Reopen completed task |
| `DELETE` | `/api/tasks/{id}` | Delete task |
| `POST` | `/api/tasks/{id}/duplicate` | Duplicate task |
| `PATCH` | `/api/tasks/{id}/reschedule` | Reschedule task date/time |
| `PATCH` | `/api/tasks/subtasks/{id}/toggle`| Toggle subtask checklist item |
| `GET` | `/api/calendar/month` | Monthly calendar task breakdown |
| `GET` | `/api/calendar/year` | Yearly planning summary |
| `GET` | `/api/reminders` | User reminders |
| `GET` | `/api/reminders/pending` | In-app notification center items |
| `POST` | `/api/reminders/{id}/dismiss` | Dismiss reminder |
| `POST` | `/api/reminders/{id}/snooze` | Snooze reminder by X minutes |
| `GET` | `/api/categories` | Categories list |
| `POST` | `/api/categories` | Create custom category |
| `GET` | `/api/export/json` | Export task database as JSON |
| `GET` | `/api/export/csv` | Export task database as CSV |
| `POST` | `/api/import/json` | Import JSON backup file |
| `DELETE` | `/api/users/account` | Permanently delete account |

---

## 🧪 Automated Testing

Run the full automated test suite:
```powershell
.\mvnw.cmd test
```
The test suite validates:
- User registration, duplicate email rejection, BCrypt authentication, password reset.
- Task scheduling, future multi-year dates, completion, reopening, duplication, rescheduling.
- **Cross-user data isolation (IDOR protection)**: Verifies that unauthorized users cannot read, modify, or delete another user's tasks.
- Reminder persistence, deduplication keys, and scheduler restart recovery.
