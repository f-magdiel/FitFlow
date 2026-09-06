from mcp.server.fastmcp import FastMCP
import threading
import sys
import io
import os
import time
from typing import Optional, Tuple
import requests


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


# --- Service discovery via Consul (default) ---
_CONSUL_CACHE: dict = {}
_CONSUL_TTL = int(os.environ.get("MCP_CONSUL_TTL", "30"))
_DEFAULT_PORTS = {
    "booking-svc": 8001,
    "users-svc": 8003,
    "notif-svc": 8002,
}


def _resolve_service_via_consul(service_name: str) -> Optional[str]:
    """Return base URL for a service using Consul catalog, e.g. http://addr:port.
    Caches results for a short TTL.
    """
    now = time.time()
    cached = _CONSUL_CACHE.get(service_name)
    if cached:
        url, ts = cached
        if now - ts < _CONSUL_TTL:
            return url

    consul_addr = os.environ.get("CONSUL_ADDR", "http://consul:8500")
    try:
        resp = requests.get(f"{consul_addr}/v1/catalog/service/{service_name}", timeout=2)
        resp.raise_for_status()
        data = resp.json()
        if not data:
            raise ValueError("empty consul response")
        entry = data[0]
        # Prefer tagged lan ipv4 address if available
        addr = None
        port = None
        tagged = entry.get("ServiceTaggedAddresses") or {}
        lan = tagged.get("lan_ipv4") or tagged.get("lan")
        if isinstance(lan, dict):
            addr = lan.get("Address")
            port = lan.get("Port")
        if not addr:
            addr = entry.get("ServiceAddress") or entry.get("Address")
        if not port:
            port = entry.get("ServicePort")
        if addr and port:
            url = f"http://{addr}:{port}"
            _CONSUL_CACHE[service_name] = (url, now)
            safe_write(f"Resolved {service_name} via Consul -> {url}")
            return url
    except Exception as e:
        safe_write(f"Consul discovery failed for {service_name}: {e}", err=True)

    # Fallback to docker network hostname and default port
    default_port = _DEFAULT_PORTS.get(service_name, 80)
    fb = f"http://{service_name}:{default_port}"
    safe_write(f"Falling back to {fb}")
    _CONSUL_CACHE[service_name] = (fb, now)
    return fb


# --- Booking-related MCP tools ---


@mcp.tool()
def get_available_classes() -> list:
    """Return list of available classes from booking-svc."""
    base = _resolve_service_via_consul("booking-svc")
    try:
        r = requests.get(f"{base}/api/classes/available", timeout=5)
        r.raise_for_status()
        return r.json()
    except Exception as e:
        safe_write(f"get_available_classes error: {e}", err=True)
        return []


@mcp.tool()
def create_booking(user_id: str, class_id: str) -> dict:
    """Create a booking for a user and class. Returns BookingResponse JSON."""
    base = _resolve_service_via_consul("booking-svc")
    payload = {"userId": user_id, "classId": class_id}
    try:
        r = requests.post(f"{base}/api/bookings", json=payload, timeout=5)
        r.raise_for_status()
        return r.json()
    except Exception as e:
        safe_write(f"create_booking error: {e}", err=True)
        return {"error": str(e)}


@mcp.tool()
def cancel_booking(booking_id: str) -> dict:
    """Cancel booking by id. Returns BookingResponse JSON."""
    base = _resolve_service_via_consul("booking-svc")
    try:
        r = requests.delete(f"{base}/api/bookings/{booking_id}", timeout=5)
        r.raise_for_status()
        return r.json()
    except Exception as e:
        safe_write(f"cancel_booking error: {e}", err=True)
        return {"error": str(e)}


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
