package com.os.video_call_application.model;

import lombok.Data;

@Data
public class SignalMessage {
    private String type;
    private String roomId;
    private String fromUserId;
    private String targetUserId;
    private Object offer;
    private Object answer;
    private Object candidate;
}