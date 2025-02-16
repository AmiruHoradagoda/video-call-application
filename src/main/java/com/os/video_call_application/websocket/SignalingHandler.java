package com.os.video_call_application.websocket;

import com.os.video_call_application.model.SignalMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import com.google.gson.Gson;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class SignalingHandler extends TextWebSocketHandler {
    private static final Logger logger = LoggerFactory.getLogger(SignalingHandler.class);
    private static final int MAX_USERS_PER_ROOM = 5;
    private final Map<String, Map<WebSocketSession, String>> rooms = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            SignalMessage signalMessage = gson.fromJson(message.getPayload(), SignalMessage.class);
            String roomId = signalMessage.getRoomId();
            logger.debug("Received message type: {} for room: {}", signalMessage.getType(), roomId);

            if ("join".equals(signalMessage.getType())) {
                handleJoinRoom(session, roomId);
            } else {
                handleSignaling(session, roomId, signalMessage);
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

    private void handleJoinRoom(WebSocketSession session, String roomId) throws IOException {
        Map<WebSocketSession, String> roomSessions = rooms.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>());

        if (roomSessions.size() >= MAX_USERS_PER_ROOM) {
            session.sendMessage(new TextMessage(gson.toJson(Map.of(
                    "type", "error",
                    "message", "Room is full"
            ))));
            return;
        }

        // Add user to room
        roomSessions.put(session, session.getId());

        // Send current participants list to new user
        List<String> participants = new ArrayList<>(roomSessions.values());
        session.sendMessage(new TextMessage(gson.toJson(Map.of(
                "type", "room-info",
                "participants", participants
        ))));

        // Notify others about new user
        notifyRoomParticipants(roomId, session, "user-joined", session.getId());

        logger.info("User {} joined room: {}. Current participants: {}",
                session.getId(), roomId, roomSessions.size());
    }

    private void handleSignaling(WebSocketSession session, String roomId, SignalMessage message) {
        Map<WebSocketSession, String> roomSessions = rooms.get(roomId);
        if (roomSessions != null) {
            String targetUserId = message.getTargetUserId();

            roomSessions.forEach((targetSession, targetSessionId) -> {
                if (targetSession.isOpen() &&
                        !targetSession.equals(session) &&
                        (targetUserId == null || targetUserId.equals(targetSessionId))) {
                    try {
                        // Add sender's ID to the message
                        message.setFromUserId(session.getId());
                        targetSession.sendMessage(new TextMessage(gson.toJson(message)));
                    } catch (IOException e) {
                        logger.error("Error sending message to peer: ", e);
                    }
                }
            });
        }
    }

    private void notifyRoomParticipants(String roomId, WebSocketSession excludeSession,
                                        String eventType, String userId) {
        Map<WebSocketSession, String> roomSessions = rooms.get(roomId);
        if (roomSessions != null) {
            SignalMessage notification = new SignalMessage();
            notification.setType(eventType);
            notification.setRoomId(roomId);
            notification.setFromUserId(userId);

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

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        handleUserDisconnection(session);
    }

    private void handleUserDisconnection(WebSocketSession session) {
        rooms.forEach((roomId, roomSessions) -> {
            if (roomSessions.containsKey(session)) {
                String userId = roomSessions.remove(session);
                notifyRoomParticipants(roomId, session, "user-left", userId);
                if (roomSessions.isEmpty()) {
                    rooms.remove(roomId);
                }
            }
        });
    }
}