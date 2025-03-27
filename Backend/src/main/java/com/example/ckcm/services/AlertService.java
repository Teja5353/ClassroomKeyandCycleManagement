package com.example.ckcm.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class AlertService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Store notifications with their expiration time
    private final Map<String, LocalDateTime> notificationExpiry = new ConcurrentHashMap<>();

    // Set an alert
    public void setAlert(String email, String message, int duration) {
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(duration);
        notificationExpiry.put(email, expiryTime);

        // Inform user about the scheduled expiration
        messagingTemplate.convertAndSend("/topic/user/" + email, Map.of(
            "message", "Notification set. You will receive an alert after " + duration + " minutes."
        ));
    }

    // Check alerts every minute
    @Scheduled(fixedRate = 60000) // Runs every 1 minute
    public void checkAlerts() {
        LocalDateTime now = LocalDateTime.now();

        notificationExpiry.forEach((email, expiryTime) -> {
            if (now.isAfter(expiryTime)) {
                // Send alert when time is over
                messagingTemplate.convertAndSend("/topic/user/" + email, Map.of(
                    "alert", "Your notification time is over!"
                ));
                notificationExpiry.remove(email);
            }
        });
    }

    // Cancel an alert before it expires
    public boolean cancelAlert(String email) {
        if (notificationExpiry.containsKey(email)) {
            notificationExpiry.remove(email);
            messagingTemplate.convertAndSend("/topic/user/" + email, Map.of(
                "alert", "Your scheduled notification has been canceled."
            ));
            return true;
        }
        return false;
    }

    // Get the remaining time for an alert
    public String getRemainingTime(String email) {
        LocalDateTime expiryTime = notificationExpiry.get(email);
        if (expiryTime != null) {
            long minutesLeft = Duration.between(LocalDateTime.now(), expiryTime).toMinutes();
            return minutesLeft > 0 ? minutesLeft + " minutes left" : "Expired";
        }
        return "No active alert found.";
    }

    // Get all active alerts
    public Map<String, String> getAllActiveAlerts() {
        return notificationExpiry.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> Duration.between(LocalDateTime.now(), entry.getValue()).toMinutes() + " minutes left"
            ));
    }
}
