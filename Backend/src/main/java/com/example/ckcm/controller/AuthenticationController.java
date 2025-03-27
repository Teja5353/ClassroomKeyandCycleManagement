package com.example.ckcm.controller;

import com.example.ckcm.auth.AuthenticationRequest;
import com.example.ckcm.auth.AuthenticationResponse;
import com.example.ckcm.auth.RegisterRequest;
import com.example.ckcm.entities.User;
import com.example.ckcm.services.AuthenticationService;
import com.example.ckcm.services.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    @Autowired
    private AuthenticationService authenticationService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/authenticate")
    public ResponseEntity<?> authenticate(@RequestBody AuthenticationRequest request) {
        try {
            AuthenticationResponse authResponse = authenticationService.authenticate(request);
            String token = authResponse.getToken();
            String role = authResponse.getRole().name();

            // ✅ Return JSON instead of redirecting
            Map<String, String> response = new HashMap<>();
            response.put("token", token);
            response.put("role", role);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(401).body("{\"error\": \"Invalid credentials\"}");
        }
    }



    // OAuth2 Login Success Handler
    @GetMapping("/oauth2/success")
    public void oauth2LoginSuccess(@RequestParam("token") String token,
                                   @RequestParam("role") String role,
                                   HttpServletResponse response,@RequestParam("email") String email) throws IOException {
        // ✅ Redirect to Angular frontend with token and role
        response.sendRedirect("http://localhost:4200?token=" + token + "&role=" + role+"&email="+email);
    }

    // @PostMapping("/register")
    // public String register(@ModelAttribute User user) {
    //     RegisterRequest request = new RegisterRequest(user.getFirstName(), user.getLastName(), user.getEmail(), user.getPassword());
    //     authenticationService.register(request);
    //     return "redirect:/auth/login";
    // }

    // @GetMapping("/register")
    // public String showRegistrationForm(Model model) {
    //     model.addAttribute("user", new User());
    //     return "register";
    // }
}
