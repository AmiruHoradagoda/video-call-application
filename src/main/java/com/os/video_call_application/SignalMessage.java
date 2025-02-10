package com.os.video_call_application;

public class SignalMessage {
    private String type;
    private String roomId;
    private Object offer;
    private Object answer;
    private Object candidate;

    // Getters and Setters
    public String getType() { return type; }
    public String getRoomId() { return roomId; }
    public Object getOffer() { return offer; }
    public Object getAnswer() { return answer; }
    public Object getCandidate() { return candidate; }

    public void setType(String type) { this.type = type; }
    public void setRoomId(String roomId) { this.roomId = roomId; }
    public void setOffer(Object offer) { this.offer = offer; }
    public void setAnswer(Object answer) { this.answer = answer; }
    public void setCandidate(Object candidate) { this.candidate = candidate; }
}