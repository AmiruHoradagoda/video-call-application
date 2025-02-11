package com.os.video_call_application;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class CustomErrorController implements ErrorController {

    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        Object message = request.getAttribute(RequestDispatcher.ERROR_MESSAGE);

        if (status != null) {
            model.addAttribute("errorCode", status.toString());
        } else {
            model.addAttribute("errorCode", "Unknown");
        }

        if (message != null) {
            model.addAttribute("errorMessage", message.toString());
        } else {
            model.addAttribute("errorMessage", "An unexpected error occurred");
        }

        return "error";
    }
}
