package com.example.ckcm.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AlertService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Store notifications with their expiration time
    private final Map<String, LocalDateTime> notificationExpiry = new ConcurrentHashMap<>();

    public void setAlert(String email, String message, int duration) {
        LocalDateTime expiryTime = LocalDateTime.now().plusMinutes(duration);
        notificationExpiry.put(email, expiryTime);

        // Inform user about the scheduled expiration
        messagingTemplate.convertAndSend("/topic/user/" + email, Map.of(
            "message", "Notification set. You will receive an alert after " + duration + " minutes."
        ));
    }

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
}
