# DevSphere

DevSphere is a Java desktop application for developer collaboration and project intelligence.
The current phase includes the Swing authentication screen, developer profiles, project management, issue tracking, and contribution intelligence.

## Technology

- Java 21
- Java Swing
- Maven
- MySQL 8.4
- MySQL Connector/J
- BCrypt

## Prerequisites

- Java 21 installed and available on `PATH`
- Maven installed and available on `PATH`
- A MySQL 8.4 server, if database connectivity is needed

## Database configuration

The application reads these environment variables:

- `DEVSPHERE_DB_URL` (default: `jdbc:mysql://localhost:3306/devsphere`)
- `DEVSPHERE_DB_USER` (required)
- `DEVSPHERE_DB_PASSWORD` (required)
- `DEVSPHERE_STORAGE_DIR` (optional; default: `devsphere-storage`)
- `DEVSPHERE_MAX_FILE_SIZE_MB` (optional; default: `25`)

No database password is stored in the source code.

For PowerShell, configure them for the current terminal session, for example:

```powershell
$env:DEVSPHERE_DB_URL = "jdbc:mysql://localhost:3306/devsphere"
$env:DEVSPHERE_DB_USER = "your_mysql_user"
$env:DEVSPHERE_DB_PASSWORD = "your_mysql_password"
```

These variables must be set in the same VS Code terminal or launch environment used to start the application. DevSphere does not use anonymous MySQL credentials and never stores or prints the password.

Project files are stored outside MySQL under:

```text
<DEVSPHERE_STORAGE_DIR>/projects/<project-id>/uploaded-files/
```

MySQL stores only file metadata. Internal filenames are UUID-based, original filenames are validated, supported extensions are restricted, and resolved paths are checked against the configured storage root.

## Build

From the project directory, run:

```powershell
mvn clean compile
```

## Run

After compiling, copy the runtime dependency and start the Swing application:

```powershell
mvn dependency:copy-dependencies
java -cp "target/classes;target/dependency/*" com.devsphere.Main
```

On startup, DevSphere creates the `users` table if it does not already exist. The application then opens the login screen.

Registration stores only a BCrypt password hash. Login accepts either a username or email and creates an in-memory session containing the authenticated user's ID and profile details.

## Developer profiles

On startup, DevSphere safely adds missing profile columns to the existing `users` table without dropping data or recreating the table. Existing accounts are preserved.

After signing in, use the dashboard navigation to open `My Profile`. You can update your headline, biography, comma-separated technical skills, social links, and profile visibility. Username and email remain read-only profile identifiers.

The profile page shows completion percentage, completed fields, and recommended missing fields. Password data is never included in the completion calculation or profile preview. `Public preview` only shows profile details when visibility is `PUBLIC`; private profiles show a privacy message instead.

## Test registration and login

With MySQL running and the environment variables configured, use the application to test:

1. Open `Create an account` and register a new user.
2. Register the same username or email again; the application should reject it.
3. Sign in with the new credentials; a welcome message confirms authentication.
4. Try an incorrect password; the application should show an invalid-credentials message.
5. Verify the `users.password_hash` value in MySQL. It should be a BCrypt hash beginning with `$2a$`, `$2b$`, or `$2y$`, never the original password.

## Test Phase 3

With the same database environment variables configured:

1. Start the application and sign in with an existing account.
2. Open `My Profile` and confirm existing full name, username, email, and skills load.
3. Enter a headline, biography, skills such as `Java, JDBC, MySQL`, and valid `https://` social URLs.
4. Save, log out, sign in again, and confirm the saved profile data remains.
5. Try an invalid URL such as `example.com`; saving should be rejected.
6. Remove a skill from the comma-separated skills field, save, and confirm it is gone after reload.
7. Set visibility to `PRIVATE` and open `Public preview`; no profile details should be shown.
8. Set visibility to `PUBLIC`, save, and open `Public preview`; the saved profile details should be shown.
9. Confirm the completion percentage changes as fields are completed.

## Project management

On startup, DevSphere safely creates these additive tables if they do not already exist:

- `projects`: project name, description, owner, status, and timestamps.
- `project_members`: project membership, role, joined timestamp, foreign keys, indexes, and a unique project/user constraint.

Existing users, profiles, and authentication data are preserved. Project creation uses a transaction so the project and its owner membership are created together.

After signing in, open `Projects` from the dashboard navigation:

1. Create a project with a name, description, and status.
2. Create a second project and confirm both appear in the list.
3. Open a project to view its details, owner, status, creation date, and member count.
4. As the owner, edit the project and change its status.
5. As the owner, add an existing user by username or email.
6. Attempt to add the same user again; it should be rejected.
7. Attempt to add an unknown username/email; it should be rejected.
8. Remove a normal member. Attempting to remove the owner is rejected.
9. Sign in as a normal member; the project is visible, but edit and team-management controls are unavailable.
10. Restart the application and confirm project and membership data persists.

Project owners can also edit project name, description, category, technologies, GitHub repository URL, and status. Project details include Overview, Members, Issues, and Files tabs. Files can be uploaded, downloaded, renamed, described, replaced, or deleted; file operations run through SwingWorker and are restricted to project members, with owner/uploader checks for modifications.

Project deletion requires owner confirmation. Memberships, issues, comments, and project file metadata are removed through the existing foreign-key relationships. Contribution records are detached from the deleted project before deletion so historical user activity and points remain available without broken project references.

Project names are required and limited to 150 characters. Descriptions are limited to 5,000 characters. Project permissions are enforced in `ProjectService` and `ProjectDao`, not only in the Swing UI.

## Contribution intelligence

Phase 6 adds the `contribution_activities` table. It records only successful platform events with the user, optional project and issue, activity type, points, unique event key, and timestamp. The unique `event_key` prevents duplicate scoring. Existing historical actions are not backfilled.

Initial scoring is: project created 15, issue created 5, issue resolved 15, issue closed 5, comment added 2, project completed 20, and issue status changed 2. Scores represent recorded platform activity only; they are not verified coding skill or productivity measurements.

Open `Contribution` after signing in to view total score, activity count, point breakdown, recent activity, a monthly Swing heatmap, leaderboard, and per-project platform activity statistics. The dashboard does not expose private profile fields.

Manual Phase 6 checks:

1. Create a project, issue, status change, resolution, closure, and comment; confirm the corresponding points and activity records appear.
2. Repeat a refresh or restart; confirm event counts and scores do not increase.
3. Complete a project and confirm the completion points appear once.
4. Compare the heatmap date counts with the recent activity records.
5. Verify leaderboard ordering by score and activity count.
6. Compare project analytics with the project's issue/comment/activity records.
7. Attempt actions as a non-member and confirm no contribution activity is recorded.

## Issue tracking

Phase 5 adds these additive tables:

- `issues`: project issues, type, priority, status, reporter, optional assignee, due date, and timestamps.
- `issue_comments`: issue comments with author and timestamp.
- `issue_activity`: status, priority, assignee, creation, and comment activity history.

All tables use foreign keys and indexes. Existing users, profiles, projects, and memberships are preserved.

After signing in, open `Issues`:

1. Choose a project and create a BUG, TASK, or FEATURE issue.
2. Set priority, status, optional assignee, and optional due date.
3. Search by title or issue ID and filter by status, priority, type, or assignee.
4. Open an issue to view details, comments, and chronological activity.
5. Update status through the supported workflow: `OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED`; resolved or closed issues can be reopened to `OPEN`.
6. Change priority or assignee. Assignees must be members of the same project; unassignment is supported.
7. Add comments as a project member.
8. Sign in as a non-member and confirm the issue is not visible or accessible.
9. Restart the application and verify issues, comments, and activity persist.

## Test the database connection

Set the database environment variables in PowerShell, compile the project, and run the JDBC test:

```powershell
$env:DEVSPHERE_DB_URL = "jdbc:mysql://localhost:3306/devsphere"
$env:DEVSPHERE_DB_USER = "your_mysql_user"
$env:DEVSPHERE_DB_PASSWORD = "your_mysql_password"

mvn compile
mvn dependency:copy-dependencies
java -cp "target/classes;target/dependency/*" com.devsphere.DatabaseTest
```

The test executes `SELECT DATABASE()` and succeeds only when the selected database is `devsphere`.

## Project overview

DevSphere is a desktop collaboration workspace for developers who need one place to maintain a professional profile, collaborate on projects, track issues, and review transparent platform activity statistics.

### Problem statement and objectives

Developer collaboration is often split between project notes, issue lists, profiles, and activity records. DevSphere brings those workflows together in a local Java Swing application backed by MySQL. Its objectives are to provide secure authentication, durable project and issue data, profile privacy controls, and understandable contribution statistics without presenting activity points as verified productivity or coding skill.

### Feature summary

- BCrypt-protected registration and login with an in-memory session.
- Editable developer profiles with visibility controls and validated social links.
- Project creation, editing, membership management, and owner permissions.
- Issue types, priorities, statuses, assignees, comments, and activity history.
- Contribution activities, score breakdown, leaderboard, project analytics, and real-data heatmap.
- Swing-only desktop navigation with resizable, scrollable screens.

## Architecture and folder structure

The application uses simple separation of responsibilities:

```text
src/main/java/com/devsphere/
├── config/       Safe idempotent database initialization
├── dao/          JDBC queries and persistence mapping
├── model/        Records and enums used by the application
├── service/      Validation, authorization, and business rules
└── ui/           Java Swing frames, panels, and dialogs
```

`DatabaseConnection` reads configuration from the environment. `DatabaseInitializer` creates missing tables and profile columns with additive, idempotent operations. No application startup migration drops or recreates existing tables.

## Database setup

Create the `devsphere` database in MySQL 8.4, then configure the three environment variables described above. On startup, the application creates the `users`, `projects`, `project_members`, `issues`, `issue_comments`, `issue_activity`, and `contribution_activities` tables if they are missing, including foreign keys, indexes, unique constraints, and profile-column migrations. Existing data is preserved.

The schema is defined in `src/main/java/com/devsphere/config/DatabaseInitializer.java`; there is no separate destructive reset script.

## Running in VS Code or a terminal

From the project directory, configure the environment variables in the same terminal used to run Maven:

```powershell
$env:DEVSPHERE_DB_URL = "jdbc:mysql://localhost:3306/devsphere"
$env:DEVSPHERE_DB_USER = "your_mysql_user"
$env:DEVSPHERE_DB_PASSWORD = "your_mysql_password"
mvn clean package
mvn dependency:copy-dependencies
java -cp "target/classes;target/dependency/*" com.devsphere.Main
```

In VS Code, open the project folder, use `Terminal > New Terminal`, set the variables there, and run the same commands. The application entry point is `com.devsphere.Main`.

After login, use the sidebar to open My Profile, Projects, Issues, or Contribution. Create a project before creating project issues. The Contribution screen uses only stored `contribution_activities` rows; it does not generate sample history.

## Testing and demonstration checklist

Automated tests cover profile URL validation, profile completion exclusion of authentication fields, invalid registration email, and password confirmation validation. Run:

```powershell
mvn clean test
mvn clean package
```

With MySQL configured, manually verify registration/login, BCrypt storage, profile persistence and privacy, clickable HTTP/HTTPS links, owner/member permissions, issue workflow and comments, contribution duplicate prevention, heatmap dates, and persistence after restart. Use the detailed phase checklists above for a full demonstration.

## Screenshots

Screenshots are not included in this repository. Add presentation screenshots here after capturing the login screen, dashboard, profile, project details, issue details, and contribution heatmap from a configured local run.

## Known limitations

- The application performs JDBC work from Swing event handlers; a future performance pass could move long-running refreshes to `SwingWorker`.
- There are no automated MySQL integration tests; database and multi-user permission checks require a configured MySQL 8.4 instance and manual test accounts.
- Skills are currently stored as comma-separated text, so the UI does not invent skill categories or proficiency levels.
- The public profile preview is available from the authenticated profile screen; a separate public-directory browser is not implemented.
- Maven clean can fail on Windows if another running process locks dependency JARs under `target`; close the running application/IDE process and retry.
