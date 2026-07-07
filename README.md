# ChatApp-Java

A simple Java TCP group chat: a server (port `3000`) broadcasts messages to all connected clients, a JavaFX client provides the UI, moderation is included, and message history is persisted in PostgreSQL.

## Features

- **Multi-client group chat over TCP**
  - The server broadcasts each message to all connected clients.
- **Join protocol**
  - After connecting, each client sends their name as the **first line**.
- **Join/leave announcements**
  - `"<name> has joined"` when a client connects
  - `"<name> has left"` when a client disconnects
- **Chat messages**
  - Format: `"<name>: <message>"`
  - Broadcast to everyone
- **Client commands**
  - `/exit` — disconnect from the server
  - `/users` — list currently connected users
  - `/history` — resend recent history
- **Message persistence (PostgreSQL)**
  - Messages are stored in a PostgreSQL table (`messages`) using **HikariCP**
  - On join, the server sends the most recent **200** messages by default
- **Server logging**
  - Severity levels: `INFO`, `WARNING`, `ERROR`
- **CLI flags**
  - `-v`, `--verbose` — enable console logging
  - `-c`, `--color` — ANSI colored logs
  - `-h`, `--help` — show help
- **Client name moderation**
  - Loads banned substrings from `banned-names.txt`
  - Case-insensitive substring matching
  - Rejects disallowed names before adding the client to the chat

## Run

1. Start the server:
   - `./gradlew run`
2. Start the JavaFX client:
   - `./gradlew runClient`

## Database configuration

`Db.java` contains the PostgreSQL connection settings:

- **JDBC URL:** `jdbc:postgresql://127.0.0.1:5432/chatdb`
- **User:** `chatapp`
- **Password:** `chat`
