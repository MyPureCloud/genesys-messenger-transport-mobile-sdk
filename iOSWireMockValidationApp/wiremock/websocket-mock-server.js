const http = require("http");
const { WebSocketServer } = require("ws");

const PORT = process.env.WS_PORT || 8089;

const server = http.createServer((req, res) => {
  res.writeHead(200, { "Content-Type": "application/json" });
  res.end(JSON.stringify({ status: "WebSocket mock server running" }));
});

const wss = new WebSocketServer({ server, path: "/api/v2/webmessaging/messages" });

let connectionCounter = 0;
const activeSessions = new Map();

wss.on("connection", (ws) => {
  connectionCounter++;
  const connectionId = connectionCounter;
  console.log(`[Connection ${connectionId}] New WebSocket connection`);

  let sessionToken = null;
  let sessionConfigured = false;

  ws.on("message", (data) => {
    const raw = data.toString();
    console.log(`[Connection ${connectionId}] Received: ${raw}`);

    let message;
    try {
      message = JSON.parse(raw);
    } catch {
      console.log(`[Connection ${connectionId}] Invalid JSON received`);
      return;
    }

    switch (message.action) {
      case "configureSession":
      case "configureAuthenticatedSession":
        handleConfigureSession(ws, message, connectionId);
        sessionToken = message.token;
        sessionConfigured = true;
        activeSessions.set(connectionId, ws);
        break;

      case "onMessage":
        if (message.message?.type === "Event") {
          handlePresenceEvent(ws, message, connectionId);
        } else {
          handleChatMessage(ws, message, connectionId);
        }
        break;

      case "echo":
        handleEcho(ws, message, connectionId);
        break;

      case "closeSession":
        handleCloseSession(ws, message, connectionId);
        break;

      default:
        console.log(`[Connection ${connectionId}] Unknown action: ${message.action}`);
    }
  });

  ws.on("close", (code, reason) => {
    console.log(`[Connection ${connectionId}] Closed: code=${code}, reason=${reason?.toString()}`);
    activeSessions.delete(connectionId);
  });

  ws.on("error", (err) => {
    console.log(`[Connection ${connectionId}] Error: ${err.message}`);
    activeSessions.delete(connectionId);
  });
});

function handleConfigureSession(ws, message, connectionId) {
  const response = {
    type: "response",
    class: "SessionResponse",
    code: 200,
    body: {
      connected: true,
      newSession: true,
      readOnly: false,
      maxCustomDataBytes: 2048,
      allowedMedia: {
        inbound: {
          fileTypes: [{ type: "*/*" }],
          maxFileSizeKB: 10240
        }
      },
      blockedExtensions: [],
      clearedExistingSession: false
    }
  };

  console.log(`[Connection ${connectionId}] Sending SessionResponse`);
  ws.send(JSON.stringify(response));
}

function handlePresenceEvent(ws, message, connectionId) {
  const events = message.message?.events || [];
  const presenceEvent = events.find((e) => e.eventType === "Presence");

  if (presenceEvent?.presence?.type === "Join") {
    console.log(`[Connection ${connectionId}] Presence Join received`);
  }
}

function handleChatMessage(ws, message, connectionId) {
  const userText = message.message?.text || "";
  const tracingId = message.tracingId;
  console.log(`[Connection ${connectionId}] Chat message: "${userText}"`);

  const outboundEcho = {
    type: "message",
    class: "StructuredMessage",
    code: 200,
    body: {
      text: userText,
      direction: "Outbound",
      id: `msg-${Date.now()}-outbound`,
      channel: {
        time: new Date().toISOString(),
        messageId: `channel-msg-${Date.now()}`
      },
      type: "Text",
      originatingEntity: "Human"
    },
    tracingId: tracingId
  };

  ws.send(JSON.stringify(outboundEcho));

  setTimeout(() => {
    const agentResponse = {
      type: "message",
      class: "StructuredMessage",
      code: 200,
      body: {
        text: `Agent received: "${userText}"`,
        direction: "Inbound",
        id: `msg-${Date.now()}-inbound`,
        channel: {
          time: new Date().toISOString(),
          messageId: `channel-msg-${Date.now()}-agent`,
          type: "Private",
          from: {
            firstName: "WireMock",
            lastName: "Agent",
            nickname: "MockAgent"
          }
        },
        type: "Text",
        originatingEntity: "Bot"
      }
    };

    console.log(`[Connection ${connectionId}] Sending agent response`);
    ws.send(JSON.stringify(agentResponse));
  }, 500);
}

function handleEcho(ws, message, connectionId) {
  const response = {
    type: "response",
    class: "StructuredMessage",
    code: 200,
    body: {
      text: "ping",
      type: "Text",
      direction: "Inbound",
      id: "echo_id"
    },
    tracingId: message.tracingId || "SGVhbHRoQ2hlY2tNZXNzYWdlSWQ="
  };

  console.log(`[Connection ${connectionId}] Sending echo response`);
  ws.send(JSON.stringify(response));
}

function handleCloseSession(ws, message, connectionId) {
  const sessionResponse = {
    type: "response",
    class: "SessionResponse",
    code: 200,
    body: {
      connected: false,
      newSession: false
    }
  };

  console.log(`[Connection ${connectionId}] Sending close session response`);
  ws.send(JSON.stringify(sessionResponse));

  if (message.closeAllConnections) {
    const closedEvent = {
      type: "message",
      class: "ConnectionClosedEvent",
      code: 200,
      body: {}
    };
    ws.send(JSON.stringify(closedEvent));
  }
}

// Admin endpoint to trigger unexpected disconnects
const adminServer = http.createServer((req, res) => {
  if (req.method === "POST" && req.url === "/disconnect-all") {
    console.log("[Admin] Triggering disconnect for all active sessions");
    activeSessions.forEach((ws, id) => {
      console.log(`[Admin] Force-closing connection ${id}`);
      ws.close(1001, "Server shutting down");
    });
    activeSessions.clear();
    res.writeHead(200, { "Content-Type": "application/json" });
    res.end(JSON.stringify({ status: "All connections closed" }));
  } else if (req.method === "POST" && req.url === "/disconnect-latest") {
    const keys = Array.from(activeSessions.keys());
    if (keys.length > 0) {
      const latestKey = keys[keys.length - 1];
      const ws = activeSessions.get(latestKey);
      console.log(`[Admin] Force-closing latest connection ${latestKey}`);
      ws.close(1001, "Server initiated disconnect");
      activeSessions.delete(latestKey);
      res.writeHead(200, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ status: `Connection ${latestKey} closed` }));
    } else {
      res.writeHead(404, { "Content-Type": "application/json" });
      res.end(JSON.stringify({ error: "No active connections" }));
    }
  } else {
    res.writeHead(200, { "Content-Type": "application/json" });
    res.end(JSON.stringify({
      status: "WebSocket admin server",
      activeConnections: activeSessions.size,
      endpoints: [
        "POST /disconnect-all",
        "POST /disconnect-latest"
      ]
    }));
  }
});

const ADMIN_PORT = process.env.ADMIN_PORT || 8090;

server.listen(PORT, () => {
  console.log(`WebSocket mock server listening on port ${PORT}`);
  console.log(`WebSocket endpoint: ws://localhost:${PORT}/api/v2/webmessaging/messages`);
});

adminServer.listen(ADMIN_PORT, () => {
  console.log(`Admin server listening on port ${ADMIN_PORT}`);
  console.log(`Admin endpoints:`);
  console.log(`  POST http://localhost:${ADMIN_PORT}/disconnect-all`);
  console.log(`  POST http://localhost:${ADMIN_PORT}/disconnect-latest`);
});
