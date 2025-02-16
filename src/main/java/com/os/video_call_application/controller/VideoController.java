package com.os.video_call_application.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class VideoController {
    private static final Logger logger = LoggerFactory.getLogger(VideoController.class);

    @GetMapping("/room/{roomId}")
    public String room(@PathVariable String roomId, Model model) {
        try {
            model.addAttribute("roomId", roomId);
            return "room";
        } catch (Exception e) {
            logger.error("Error in room controller: ", e);
            throw e;
        }
    }
}