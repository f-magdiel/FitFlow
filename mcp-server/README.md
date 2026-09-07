# MCP Server (Python)

The Python MCP SDK exposes Streamable HTTP at `/mcp` on container port
8000. Docker publishes it at `http://localhost:7777/mcp`.
The separate FastAPI demo bridge runs on container port 8080; its REST
routes are not MCP endpoints.

## Start with Docker

With the project's Compose network and backend services already running:

```bash
sh mcp-server/start.sh start
```

Alternatively, use `docker compose up -d --build mcp-server` from the
project root. Use only one launcher at a time because both publish the
same host ports. The standalone script rebuilds and recreates its container.

## Claude Desktop

Merge the `mcpServers` entry from the repository's
`claude_desktop_config.json` into Claude Desktop's configuration, preserving
other settings. On macOS this is
`~/Library/Application Support/Claude/claude_desktop_config.json`.
Node.js and `npx` must be available on the Mac: Claude launches `mcp-remote`
locally to translate stdio to the container's HTTP endpoint.
Fully quit and reopen Claude Desktop after changing its configuration.

## Inspector

The container starts the official MCP Inspector. Open its UI on host port
6274 using the session token from the container logs. To test the HTTP
endpoint, select Streamable HTTP and enter `http://127.0.0.1:8000/mcp`:
the Inspector proxy runs inside the container. A successful stdio connection
through `mcp dev` tests a separate process, not the published HTTP endpoint.
Verify initialization, tool discovery, and an `add` call.

## Run Python directly

```bash
pip install -r mcp-server/requirements.txt
python mcp-server/src/server.py
```

This defaults to HTTP on `0.0.0.0:8000`. `MCP_TRANSPORT_HOST` and
`MCP_TRANSPORT_PORT` override the listener. If you change the container
port, also update the Docker port mapping. The tools include `add`,
`get_available_classes`, `create_booking`, and `cancel_booking`, plus the
`greeting://{name}` resource template.
