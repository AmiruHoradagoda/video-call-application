package com.os.video_call_application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SignalingHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(SignalingHandler.class);
    private final Map<String, Map<WebSocketSession, String>> rooms = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        logger.info("Connection established: {}", session.getId());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            SignalMessage signalMessage = gson.fromJson(message.getPayload(), SignalMessage.class);
            String roomId = signalMessage.getRoomId();
            logger.debug("Received message type: {} for room: {}", signalMessage.getType(), roomId);

            if ("join".equals(signalMessage.getType())) {
                handleJoinRoom(session, roomId);
            } else {
                handleSignaling(session, roomId, message);
            }
        } catch (Exception e) {
            logger.error("Error handling message: ", e);
            try {
                session.sendMessage(new TextMessage(gson.toJson(Map.of(
                        "type", "error",
                        "message", "Failed to process message"
                ))));
            } catch (IOException ex) {
                logger.error("Error sending error message: ", ex);
            }
        }
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        logger.error("Transport error: ", exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        logger.info("Connection closed: {} with status: {}", session.getId(), status);
        handleUserDisconnection(session);
    }

    private void handleUserDisconnection(WebSocketSession session) {
        rooms.forEach((roomId, roomSessions) -> {
            if (roomSessions.containsKey(session)) {
                roomSessions.remove(session);
                notifyRoomParticipants(roomId, session, "user-left");
                if (roomSessions.isEmpty()) {
                    rooms.remove(roomId);
                }
            }
        });
    }

    private void handleJoinRoom(WebSocketSession session, String roomId) throws IOException {
        Map<WebSocketSession, String> roomSessions = rooms.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());

        if (roomSessions.size() >= 2) {
            session.sendMessage(new TextMessage(gson.toJson(Map.of(
                    "type", "error",
                    "message", "Room is full"
            ))));
            return;
        }

        roomSessions.put(session, session.getId());
        logger.info("User {} joined room: {}. Current participants: {}", session.getId(), roomId, roomSessions.size());
        notifyRoomParticipants(roomId, session, "user-joined");
    }

    private void handleSignaling(WebSocketSession session, String roomId, TextMessage message) {
        Map<WebSocketSession, String> roomSessions = rooms.get(roomId);
        if (roomSessions != null) {
            roomSessions.keySet().stream()
                    .filter(s -> s.isOpen() && !s.equals(session))
                    .forEach(s -> {
                        try {
                            s.sendMessage(message);
                        } catch (IOException e) {
                            logger.error("Error sending message to peer: ", e);
                        }
                    });
        }
    }

    private void notifyRoomParticipants(String roomId, WebSocketSession excludeSession, String eventType) {
        Map<WebSocketSession, String> roomSessions = rooms.get(roomId);
        if (roomSessions != null) {
            SignalMessage notification = new SignalMessage();
            notification.setType(eventType);
            notification.setRoomId(roomId);

            String notificationJson = gson.toJson(notification);
            roomSessions.keySet().stream()
                    .filter(s -> s.isOpen() && !s.equals(excludeSession))
                    .forEach(s -> {
                        try {
                            s.sendMessage(new TextMessage(notificationJson));
                        } catch (IOException e) {
                            logger.error("Error sending notification: ", e);
                        }
                    });
        }
    }
}