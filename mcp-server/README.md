# MCP Server (Python)

This folder contains a minimal MCP server example (Python) based on the workshop example.

Files:
- `src/server.py` — MCP server with a `add(a,b)` tool and a `greeting://{name}` resource.
- `requirements.txt` — Python dependency (`mcp[cli]`).
- `Dockerfile` — builds a small image that runs `python src/server.py`.

Run locally (inspector):

```bash
# install the MCP CLI (optional for local inspection)
pip install "mcp[cli]"

# run the server directly
python src/server.py

# in another terminal run the inspector
mcp dev src/server.py
```

Build and run in Docker:

```bash
cd mcp-server
docker build -t fitflow/mcp-server:latest .
docker run --rm -it fitflow/mcp-server:latest
```

Notes:
- The MCP server communicates via stdin/stdout; running it in container works with the inspector tool if you forward the process streams (running inspector locally and server in container is supported by connecting via stdio or using the MCP CLI).
- If you want the inspector to connect from your host to the container, consider running the server with `mcp dev` on the host or expose a network transport if implementing an HTTP bridge.
