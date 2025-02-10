package com.os.video_call_application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
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
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            SignalMessage signalMessage = gson.fromJson(message.getPayload(), SignalMessage.class);
            String roomId = signalMessage.getRoomId();

            if ("join".equals(signalMessage.getType())) {
                handleJoinRoom(session, roomId);
            } else {
                handleSignaling(session, roomId, message);
            }
        } catch (Exception e) {
            logger.error("Error handling message: ", e);
        }
    }

    private void handleJoinRoom(WebSocketSession session, String roomId) throws IOException {
        rooms.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>())
                .put(session, session.getId());

        // Notify other participants in the room
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

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        rooms.values().forEach(roomSessions -> {
            if (roomSessions.containsKey(session)) {
                String roomId = roomSessions.values().iterator().next();
                roomSessions.remove(session);
                try {
                    notifyRoomParticipants(roomId, session, "user-left");
                } catch (Exception e) {
                    logger.error("Error handling connection close: ", e);
                }
            }
        });

        // Clean up empty rooms
        rooms.entrySet().removeIf(entry -> entry.getValue().isEmpty());
    }
}
