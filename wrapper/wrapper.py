#!/usr/bin/env python3
"""
Wrapper Service for WireMock-based UI Integration Testing.

A lightweight HTTP server that translates high-level test commands
(e.g., "send agent message") into WireMock Admin API calls, injecting
messages into active WebSocket connections.

Usage:
    python3 wrapper.py [--port 9090] [--wiremock-port 8080] [--stubs-dir ./stubs]

Endpoints:
    POST /wrapper/send/agentMessage   — Send an agent text message
    POST /wrapper/send/agentTyping    — Send an agent typing indicator
    GET  /wrapper/health              — Health check
    GET  /wrapper/channels            — List active WireMock WebSocket channels
"""

import argparse
import json
import os
import sys
import uuid
from datetime import datetime, timezone
from http.server import ThreadingHTTPServer, BaseHTTPRequestHandler
from urllib.request import urlopen, Request
from urllib.error import URLError


WEBSOCKET_PATH = "/v1"


class WrapperHandler(BaseHTTPRequestHandler):

    def do_GET(self):
        if self.path == "/wrapper/health":
            self._handle_health()
        elif self.path == "/wrapper/channels":
            self._handle_list_channels()
        else:
            self._respond(404, {"error": f"Not found: {self.path}"})

    def do_POST(self):
        if self.path == "/wrapper/send/agentMessage":
            self._handle_send("agentMessage")
        elif self.path == "/wrapper/send/agentTyping":
            self._handle_send("agentTyping")
        else:
            self._respond(404, {"error": f"Not found: {self.path}"})

    # ── Endpoint handlers ──────────────────────────────────────────

    def _handle_health(self):
        wiremock_ok = self._wiremock_reachable()
        status = 200 if wiremock_ok else 503
        self._respond(status, {
            "status": "ok" if wiremock_ok else "degraded",
            "wrapper": "running",
            "wiremock": "reachable" if wiremock_ok else "unreachable",
            "wiremockUrl": self.server.wiremock_base_url,
        })

    def _handle_list_channels(self):
        try:
            result = self._wiremock_get("/__admin/channels")
            self._respond(200, result)
        except Exception as exc:
            self._respond(502, {"error": f"WireMock error: {exc}"})

    def _handle_send(self, endpoint_name):
        body = self._read_body()
        payload = json.loads(body) if body else {}

        message_id = uuid.uuid4().hex[:8]
        timestamp = datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%S.000Z")

        stub_path = os.path.join(
            self.server.stubs_dir, endpoint_name, "v1", "stub.json"
        )

        if not os.path.isfile(stub_path):
            self._respond(404, {"error": f"Stub not found: {stub_path}"})
            return

        with open(stub_path, "r") as f:
            template = f.read()

        message_text = payload.get("text", "Hello from the agent!")
        rendered = (
            template
            .replace("{{messageText}}", message_text)
            .replace("{{messageId}}", message_id)
            .replace("{{timestamp}}", timestamp)
        )

        rendered_json = json.loads(rendered)
        serialized = json.dumps(rendered_json, separators=(",", ":"))

        wiremock_payload = {
            "type": "websocket",
            "initiatingRequest": {
                "urlPath": WEBSOCKET_PATH,
            },
            "message": {
                "body": {
                    "data": serialized,
                }
            }
        }

        try:
            result = self._wiremock_post("/__admin/channels/send", wiremock_payload)
            sent_count = result.get("channelsMessaged", 0)
            self._respond(200, {
                "status": "sent",
                "endpoint": endpoint_name,
                "sentCount": sent_count,
                "messageId": message_id,
            })
        except Exception as exc:
            self._respond(502, {"error": f"WireMock send failed: {exc}"})

    # ── WireMock communication ─────────────────────────────────────

    def _wiremock_reachable(self):
        try:
            self._wiremock_get("/__admin")
            return True
        except Exception:
            return False

    def _wiremock_get(self, path):
        url = f"{self.server.wiremock_base_url}{path}"
        req = Request(url)
        with urlopen(req, timeout=5) as resp:
            return json.loads(resp.read().decode())

    def _wiremock_post(self, path, data):
        url = f"{self.server.wiremock_base_url}{path}"
        body = json.dumps(data).encode()
        req = Request(url, data=body, method="POST")
        req.add_header("Content-Type", "application/json")
        with urlopen(req, timeout=5) as resp:
            return json.loads(resp.read().decode())

    # ── HTTP helpers ───────────────────────────────────────────────

    def _read_body(self):
        length = int(self.headers.get("Content-Length", 0))
        return self.rfile.read(length).decode() if length > 0 else ""

    def _respond(self, status, data):
        body = json.dumps(data, indent=2).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.send_header("Access-Control-Allow-Origin", "*")
        self.end_headers()
        self.wfile.write(body)

    def log_message(self, fmt, *args):
        ts = datetime.now().strftime("%H:%M:%S")
        sys.stderr.write(f"[wrapper {ts}] {fmt % args}\n")


class WrapperServer(ThreadingHTTPServer):
    def __init__(self, port, wiremock_port, stubs_dir):
        super().__init__(("", port), WrapperHandler)
        self.wiremock_base_url = f"http://localhost:{wiremock_port}"
        self.stubs_dir = stubs_dir


def main():
    parser = argparse.ArgumentParser(description="Wrapper service for WireMock UI testing")
    parser.add_argument("--port", type=int, default=9090, help="Wrapper port (default: 9090)")
    parser.add_argument("--wiremock-port", type=int, default=8080, help="WireMock port (default: 8080)")
    parser.add_argument("--stubs-dir", default=None, help="Path to stubs directory")
    args = parser.parse_args()

    stubs_dir = args.stubs_dir or os.path.join(os.path.dirname(__file__), "stubs")

    server = WrapperServer(args.port, args.wiremock_port, stubs_dir)

    print(f"Wrapper service starting on port {args.port}")
    print(f"  WireMock target: http://localhost:{args.wiremock_port}")
    print(f"  Stubs directory: {stubs_dir}")
    print()
    print("Endpoints:")
    print(f"  POST http://localhost:{args.port}/wrapper/send/agentMessage")
    print(f"  POST http://localhost:{args.port}/wrapper/send/agentTyping")
    print(f"  GET  http://localhost:{args.port}/wrapper/health")
    print(f"  GET  http://localhost:{args.port}/wrapper/channels")
    print()
    print("Press Ctrl+C to stop.")

    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nWrapper service stopped.")
        server.server_close()


if __name__ == "__main__":
    main()
