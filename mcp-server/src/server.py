from mcp.server.fastmcp import FastMCP
import threading
import sys
import io


def safe_write(msg: str, err: bool = False) -> None:
    """Write message to stdout/stderr, fall back to /dev/std{out,err} if streams are closed."""
    try:
        stream = sys.stderr if err else sys.stdout
        stream.write(msg + "\n")
        stream.flush()
        return
    except Exception:
        # fallback to /dev/stdout or /dev/stderr which should always be available in containers
        path = "/dev/stderr" if err else "/dev/stdout"
        try:
            with open(path, "w") as f:
                f.write(msg + "\n")
                f.flush()
        except Exception:
            # last resort: ignore
            pass


mcp = FastMCP("MCP Server")


@mcp.tool()
def add(a: int, b: int) -> int:
    """Add two integers and return the result."""
    return a + b


@mcp.resource("greeting://{name}")
def get_greeting(name: str) -> str:
    """Return a simple greeting for the requested name."""
    return f"Hello, {name}!"


def run_mcp() -> None:
    """Run the MCP server (blocking). Intended to be used from a background thread."""
    safe_write("MCP run starting")
    try:
        mcp.run()
    except Exception as e:
        safe_write(f"MCP run error: {e}", err=True)
    finally:
        safe_write("MCP run finished")


if __name__ == "__main__":
    safe_write("Starting MCP Server")
    try:
        mcp.run()
    except Exception as e:
        # show errors in container logs and continue to keep container alive for debugging
        safe_write(f"MCP run error: {e}", err=True)

    # Keep container alive so the inspector or user can attach; MCP servers often
    # communicate over stdin/stdout and may exit if not managed by the CLI.
    safe_write("Server started — entering keep-alive. Press Ctrl+C to exit.")
    try:
        threading.Event().wait()
    except KeyboardInterrupt:
        safe_write("Shutting down MCP Server")
