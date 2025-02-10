package com.os.video_call_application;

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
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private final Gson gson = new Gson();

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        SignalMessage signalMessage = gson.fromJson(message.getPayload(), SignalMessage.class);
        String roomId = signalMessage.getRoomId();

        if ("join".equals(signalMessage.getType())) {
            sessions.put(roomId, session);
        } else {
            WebSocketSession peerSession = sessions.get(roomId);
            if (peerSession != null && peerSession.isOpen() && !peerSession.equals(session)) {
                peerSession.sendMessage(message);
            }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, org.springframework.web.socket.CloseStatus status) {
        sessions.values().remove(session);
    }
}