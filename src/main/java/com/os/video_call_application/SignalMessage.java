package com.os.video_call_application;

public class SignalMessage {
    private String type;
    private String roomId;

    public String getType() { return type; }
    public String getRoomId() { return roomId; }
    public void setType(String type) { this.type = type; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
}