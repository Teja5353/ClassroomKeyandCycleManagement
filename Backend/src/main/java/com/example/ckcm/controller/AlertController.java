package com.example.ckcm.controller;

import com.example.ckcm.services.AlertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@CrossOrigin(origins = "http://localhost:4200")
@RequestMapping("/alerts_for_notifications")
public class AlertController {

    @Autowired
    private AlertService alertService;

    // Endpoint to set an alert
    @PostMapping("/set")
    public String setAlert(@RequestBody Map<String, String> payload) {
        String email = payload.get("email");
        String message = payload.get("message");
        int duration = Integer.parseInt(payload.get("duration")); // Duration in minutes

        alertService.setAlert(email, message, duration);
        return "Alert set successfully.";
    }

    // Endpoint to cancel an alert
    @DeleteMapping("/cancel/{email}")
    public String cancelAlert(@PathVariable String email) {
        boolean canceled = alertService.cancelAlert(email);
        return canceled ? "Alert canceled successfully." : "No active alert found for this user.";
    }

    // Endpoint to check remaining time for an alert
    @GetMapping("/remaining-time/{email}")
    public String getRemainingTime(@PathVariable String email) {
        return alertService.getRemainingTime(email);
    }

    // Endpoint to get all active alerts
    @GetMapping("/all-active")
    public Map<String, String> getAllActiveAlerts() {
        return alertService.getAllActiveAlerts();
    }
}
