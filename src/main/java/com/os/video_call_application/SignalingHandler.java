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
    private Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();
    private Gson gson = new Gson();

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws IOException {
        SignalMessage signalMessage = gson.fromJson(message.getPayload(), SignalMessage.class);

        if ("join".equals(signalMessage.getType())) {
            sessions.put(signalMessage.getRoomId(), session);
        } else {
            WebSocketSession peerSession = sessions.get(signalMessage.getRoomId());
            if (peerSession != null && peerSession.isOpen()) {
                peerSession.sendMessage(message);
            }
        }
    }
}

class SignalMessage {
    private String type;
    private String roomId;

    public String getType() { return type; }
    public String getRoomId() { return roomId; }
    public void setType(String type) { this.type = type; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
}