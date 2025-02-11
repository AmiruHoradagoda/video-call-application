package com.os.video_call_application;

import lombok.Data;

@Data
public class SignalMessage {
    private String type;
    private String roomId;
    private Object offer;
    private Object answer;
    private Object candidate;
}
