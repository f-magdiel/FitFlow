FROM python:3.11-slim

WORKDIR /app

# Copy everything and install runtime dependencies
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# Copy application files
COPY . .

ENV PYTHONUNBUFFERED=1

# Install Node.js (required by the MCP inspector which uses npx) and expose ports
RUN apt-get update \
	&& apt-get install -y curl ca-certificates gnupg \
	&& curl -fsSL https://deb.nodesource.com/setup_22.x | bash - \
	&& apt-get install -y nodejs socat procps \
	&& rm -rf /var/lib/apt/lists/* \
	&& npm install -g uv @modelcontextprotocol/inspector@latest

# Provide a tiny `uv` shim so the inspector can spawn `uv run ...` inside
# the container. The real `uv` CLI isn't required for our simple use-case; the
# shim strips `--with <pkg>` tokens and execs the remaining command, e.g.
# `uv run --with mcp mcp run src/server.py` -> `mcp run src/server.py`.
COPY uv /usr/local/bin/uv
RUN chmod +x /usr/local/bin/uv

# Expose MCP, bridge, and inspector ports
EXPOSE 8000
EXPOSE 8080
EXPOSE 6274
EXPOSE 6277

CMD ["python", "start.py"]
