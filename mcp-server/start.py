import threading
import time
import uvicorn
import sys
import os
import subprocess
import re

from src import server as mcp_server
from bridge import app

# use safe_write from server to avoid ValueError when stdout/stderr are closed
try:
    from src.server import safe_write
except Exception:
    def safe_write(msg: str, err: bool = False) -> None:
        try:
            stream = sys.stderr if err else sys.stdout
            stream.write(msg + "\n")
            stream.flush()
            return
        except Exception:
            path = "/dev/stderr" if err else "/dev/stdout"
            try:
                with open(path, "w") as f:
                    f.write(msg + "\n")
                    f.flush()
            except Exception:
                pass



def start_mcp_in_thread() -> threading.Thread:
    t = threading.Thread(target=mcp_server.run_mcp, daemon=True)
    t.start()
    return t


if __name__ == "__main__":
    safe_write("Launching MCP server in background thread")
    th = start_mcp_in_thread()
    # give the MCP server a moment to start
    time.sleep(1)
    safe_write("Starting HTTP bridge on 0.0.0.0:8080")
    # Optionally start the official MCP inspector CLI inside the container so
    # you get the interactive inspector UI. It will run on INSPECTOR_PORT.
    INSPECTOR_PORT = int(os.environ.get("MCP_INSPECTOR_PORT", "3000"))
    try:
        safe_write(f"Starting MCP inspector on 0.0.0.0:{INSPECTOR_PORT}")

        def inspector_worker():
            # Ensure no stale inspector/mcp dev processes are running
            # that could hold the native ports, then start a fresh
            # inspector process and stream its output.
            try:
                for kill_cmd in ("pkill -f 'mcp dev'", "pkill -f '@modelcontextprotocol/inspector'"):
                    try:
                        subprocess.run(kill_cmd, shell=True, stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                    except Exception:
                        pass

                # Start the inspector process with environment overrides so it
                # binds to the container network interface. This lets Docker's
                # published port reach the inspector without in-container
                # forwarders.
                env = os.environ.copy()
                env.setdefault("MCP_INSPECTOR_HOST", "0.0.0.0")
                env.setdefault("MCP_INSPECTOR_PORT", os.environ.get("MCP_INSPECTOR_PORT", "6274"))
                # Ensure secrets fallback to memory (avoid keychain permission errors)
                env.setdefault("MCP_INSPECTOR_SECRET_STORE", "memory")
                proc = subprocess.Popen(["mcp", "dev", "src/server.py"], stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True, env=env)
            except Exception as e:
                safe_write(f"Failed to spawn mcp dev: {e}", err=True)
                return

            # Stream output and detect when the web UI is available. Once
            # the inspector prints the UI URL we start in-container socat
            # forwarders so Docker's published ports can reach the UI.
            ui_forwarders_started = False
            try:
                for line in proc.stdout:
                    text = line.rstrip()
                    safe_write(text)
                    if (not ui_forwarders_started) and ("MCP Inspector Web is up" in text or "MCP Inspector Web is up and running" in text or "Web is up" in text):
                        try:
                            # Listen on alternate container ports and forward to
                            # the inspector's loopback ports to avoid trying to
                            # bind INADDR_ANY where the inspector already owns
                            # the original ports.
                            subprocess.Popen(["socat", "TCP-LISTEN:16274,fork,bind=0.0.0.0", "TCP:127.0.0.1:6274"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                            subprocess.Popen(["socat", "TCP-LISTEN:16277,fork,bind=0.0.0.0", "TCP:127.0.0.1:6277"], stdout=subprocess.DEVNULL, stderr=subprocess.DEVNULL)
                            safe_write("Started socat forwarders on container ports 16274/16277 -> 127.0.0.1:6274/6277")
                            ui_forwarders_started = True
                        except Exception as e:
                            safe_write(f"Failed to start socat forwarders: {e}", err=True)
            except Exception:
                pass

        t = threading.Thread(target=inspector_worker, daemon=True)
        t.start()
    except Exception as e:
        safe_write(f"Failed to start MCP inspector: {e}", err=True)

    # Ensure stdout/stderr are open for uvicorn logging (avoid ValueError on closed files)
    try:
        if getattr(sys.stdout, "closed", False):
            sys.stdout = open(os.devnull if os.name == 'nt' else '/dev/stdout', 'w')
        if getattr(sys.stderr, "closed", False):
            sys.stderr = open(os.devnull if os.name == 'nt' else '/dev/stderr', 'w')
    except Exception:
        pass

    uvicorn.run(app, host="0.0.0.0", port=8080)
