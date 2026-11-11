# Database setup

Create the database once, without creating application tables manually:

```sql
CREATE DATABASE devsphere CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

At startup, `DatabaseInitializer` performs additive, idempotent initialization. It creates missing tables and adds supported columns without dropping tables or resetting existing records.

The current schema covers:

- `users` and developer profile fields
- `projects` and `project_members`
- `project_files`
- `issues`, `issue_comments`, and `issue_activity`
- `contribution_activities`

Use these environment variables:

```text
DEVSPHERE_DB_URL=jdbc:mysql://localhost:3306/devsphere
DEVSPHERE_DB_USER=your_mysql_user
DEVSPHERE_DB_PASSWORD=your_mysql_password
```

The `.env.example` file is a safe reference only. The application reads operating-system environment variables with `System.getenv`; it does not load `.env` automatically. Never commit real credentials.

Project files default to `devsphere-storage` and are constrained by the configured file-size/type and permission checks. Keep that local directory out of Git.

## Connection test

After building and copying dependencies, run:

```powershell
mvn dependency:copy-dependencies
java -cp "target/classes;target/dependency/*" com.devsphere.DatabaseTest
```

The test verifies the connection and selected database with `SELECT DATABASE()` and never prints the password.
