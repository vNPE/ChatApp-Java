# ChatApp-Java
A simple Java TCP group chat school project.

# Features
- Multi-client group chat over TCP (server listens on port 3000)
- Each client sends their name as the first line upon connecting
- Server broadcasts:
    - "<name> has joined" when a new client connects
    - "<name> has left" when a client disconnects
    - "<name>: <message>" for chat messages
- /exit command to disconnect from the server
- Server logging with severity levels: INFO, WARNING, ERROR
- Command-line flags:
    - -v, --verbose enables console logging
    - -c, --color enables ANSI colors for log output
    - -h, --help prints help
- Moderation for client names:
    - Loads banned substrings from a file (banned-names.txt)
    - Checks names case-insensitively using substring matching
    - Rejects disallowed names before the client joins the chat

