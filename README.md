# Arcturus

Arcturus is a Spring Boot notification service. It sends email through SMTP,
records notification history, and rejects notifications to blocked targets.
SMS and push delivery are currently simulations.

## Requirements

- Java 21
- An SMTP server for email delivery, such as a local [Mailpit](https://mailpit.axllent.org/) instance
- Docker, if using the Mailpit example below

The Gradle wrapper downloads the required Gradle version and dependencies.

## Run locally

Start a local SMTP test server:

```powershell
docker run --rm -p 127.0.0.1:1025:1025 -p 127.0.0.1:8025:8025 axllent/mailpit
```

In another terminal, start the application from the project root:

```powershell
.\gradlew.bat bootRun
```

Mailpit's inbox is available at `http://localhost:8025`. The application
defaults to SMTP at `localhost:1025`, with `notifications@example.com` as the
sender. Set `spring.mail.host`, `spring.mail.port`, and `arcturus.mail.from`
to use a different server.

The default H2 database is in memory. Data does not survive a restart, and
`schema.sql` drops and recreates tables during initialization. Do not use this
schema script with a database containing data you need to keep.

## API

Spring Security's default configuration requires authentication. The username
is `user`; a generated password is printed at startup. POST requests also
require a CSRF token and its session cookie. For example, obtain them from the
login page before using an HTTP client.

| Method | Path | Purpose |
| --- | --- | --- |
| POST | `/api/notifications/send` | Send or reject a notification |
| GET | `/api/notifications/history` | Return up to 20 newest notifications |
| GET | `/api/notifications/latest` | Return the newest notification |
| POST | `/api/notifications/update/{id}` | Update an existing notification's type, target, and payload |

Example request body for `/api/notifications/send`:

```json
{
  "type": "EMAIL",
  "target": "user@example.com",
  "payload": "Hello from Arcturus"
}
```

Types are `EMAIL`, `SMS`, and `PUSH`. The target must contain 3 to 255
characters; the payload must be nonblank and at most 5,000 characters.
Validation failures return HTTP 400. SMTP failures are recorded as `ERROR`
and return an error response; automatic retries are not implemented.
The update endpoint uses the same request body, preserves the notification's
status and delivery history, and returns HTTP 404 if the ID does not exist.
Sent notifications cannot be updated; attempting to do so returns HTTP 409.
Updating a notification does not send it.

## Blocking and statuses

An active block rejects a notification without sending it. A block with no end
date is indefinite. At midnight in the application's local time zone, the
cleanup job marks rejected notifications for expired temporary blocks
`IN_REVIEW`, then removes those blocks in the same transaction. Notifications
in review need manual attention; they are not automatically resent.

Status codes include `N` (new), `S` (sent), `E` (error), `R` (rejected),
`I` (in review), and `U` (unknown).

## Tests

```powershell
.\gradlew.bat test
```

Tests cover notification history, email delivery with a mocked mail sender,
error handling, and block cleanup.
