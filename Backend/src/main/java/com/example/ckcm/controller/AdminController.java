package com.example.ckcm.controller;

import com.example.ckcm.auth.AuthenticationRequest;
import com.example.ckcm.auth.AuthenticationResponse;
import com.example.ckcm.entities.Cycle;
import com.example.ckcm.entities.CycleBorrow;
import com.example.ckcm.entities.Key;
import com.example.ckcm.entities.KeyBorrow;
import com.example.ckcm.repositories.CycleBorrowRepository;
import com.example.ckcm.repositories.CycleRepository;
import com.example.ckcm.repositories.KeyBorrowRepository;
import com.example.ckcm.repositories.KeyRepository;
import com.example.ckcm.services.AuthenticationService;
import com.example.ckcm.services.KeyBorrowService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/admin")
@CrossOrigin(origins = "http://localhost:4200")
public class AdminController {

    @Autowired
    private CycleBorrowRepository cycleBorrowRepository;

    @Autowired
    private CycleRepository cycleRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private KeyRepository keyRepository;
    @Autowired
    private KeyBorrowRepository keyBorrowRepository;
    @Autowired
    private KeyBorrowService keyBorrowService;
    @Autowired
    private AuthenticationService authenticationService;
    //  Fetch all borrow requests (for admin)
    @GetMapping("/borrow-requests")
    public ResponseEntity<List<CycleBorrow>> getBorrowRequests() {
        return ResponseEntity.ok(cycleBorrowRepository.findAll());
    }

    //  Approve Borrow Request
    @PostMapping("/approve/{id}")
    public ResponseEntity<?> approveBorrow(@PathVariable Long id) {
        Optional<CycleBorrow> request = cycleBorrowRepository.findById(id);
        if (request.isPresent() && "Pending".equals(request.get().getStatus())) {
            CycleBorrow borrowRequest = request.get();
            Optional<Cycle> cycleOpt = cycleRepository.findByCycleId(borrowRequest.getCycleId());
            if (cycleOpt.isPresent()) {
                Cycle cycle = cycleOpt.get();
                cycle.setStatus("Borrowed");
                cycle.setBorrowedBy(borrowRequest.getBorrowerEmail());
                cycle.setBorrowedAt(new Date());
                cycleRepository.save(cycle);
            }
            borrowRequest.setStatus("Approved");
            cycleBorrowRepository.save(borrowRequest);

            // Notify Admin & User via WebSockets
            messagingTemplate.convertAndSend("/topic/admin/notifications",
                    "Borrow request approved for " + borrowRequest.getBorrowerEmail());
            messagingTemplate.convertAndSend("/topic/user/" + borrowRequest.getBorrowerEmail(),
                    Map.of("message", "Your borrow request has been approved.", "status", "Approved"));

            return ResponseEntity.ok(Map.of("message", "Borrow request approved."));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Request not found."));
    }

    //  Deny Borrow Request
    @PostMapping("/deny/{id}")
    public ResponseEntity<?> denyBorrow(@PathVariable Long id) {
        Optional<CycleBorrow> request = cycleBorrowRepository.findById(id);
        if (request.isPresent() && "Pending".equals(request.get().getStatus())) {
            CycleBorrow borrowRequest = request.get();
            Optional<Cycle> cycleOpt = cycleRepository.findByCycleId(borrowRequest.getCycleId());
            if (cycleOpt.isPresent()) {
                Cycle cycle = cycleOpt.get();
                cycle.setStatus("Available");
                cycle.setBorrowedBy(null);
                cycleRepository.save(cycle);
            }
            borrowRequest.setStatus("Denied");
            cycleBorrowRepository.save(borrowRequest);

            //  Notify Admin & User via WebSockets
            messagingTemplate.convertAndSend("/topic/admin/notifications",
                    "Borrow request denied for " + borrowRequest.getBorrowerEmail());
            messagingTemplate.convertAndSend("/topic/user/" + borrowRequest.getBorrowerEmail(),
                    Map.of("message", "Your borrow request has been denied.", "status", "Denied"));

            return ResponseEntity.ok(Map.of("message", "Borrow request denied."));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Request not found."));
    }
    @PostMapping("/request-borrow")
    public ResponseEntity<?> createBorrowRequest(@RequestBody CycleBorrow borrowRequest) {
        Optional<Cycle> cycleOpt = cycleRepository.findByCycleId(borrowRequest.getCycleId());
        Optional<CycleBorrow> borrowOpt = cycleBorrowRepository.findByCycleIdAndBorrowerEmailAndStatus(borrowRequest.getCycleId(), borrowRequest.getBorrowerEmail(),"Approved");
        if(borrowOpt.isPresent() && borrowOpt.get().getStatus().equals("Approved")){
             return ResponseEntity.badRequest().body(Map.of("message", "Request not found."));
        }
        if (cycleOpt.isPresent() && "Pending".equals(cycleOpt.get().getStatus())) {
            return ResponseEntity.badRequest().body(Map.of("message", "Cycle is not available"));
        }

        borrowRequest.setStatus("Pending");
        if (cycleOpt.isPresent()) {
            Cycle cycle = cycleOpt.get();
            cycle.setStatus("Pending");
            cycleRepository.save(cycle);
        }
        borrowRequest.setRequestTime(new Date());
        borrowRequest.setBorrowerEmail(borrowRequest.getBorrowerEmail());
        cycleBorrowRepository.save(borrowRequest);

        // ✅ Notify Admin via WebSockets
        messagingTemplate.convertAndSend("/topic/admin/notifications",
                "New borrow request from " + borrowRequest.getBorrowerEmail());

        return ResponseEntity.ok(Map.of("message", "Borrow request submitted successfully."));
    }

    // ==========================
    // ✅ REQUEST RETURN (SIMILAR TO BORROWING)
    // ==========================

    // ✅ User Requests to Return Cycle
    @PostMapping("/request-return")
    public ResponseEntity<?> requestCycleReturn(@RequestBody CycleBorrow returnRequest) {
        Optional<CycleBorrow> borrowRequestOpt = cycleBorrowRepository
                .findByCycleIdAndBorrowerEmailAndStatus(returnRequest.getCycleId(), returnRequest.getBorrowerEmail(),"Approved");

        if (borrowRequestOpt.isPresent()) {
            CycleBorrow borrowRequest = borrowRequestOpt.get();
            if (!"Approved".equals(borrowRequest.getStatus())) {
                return ResponseEntity.badRequest().body(Map.of("message", "Cycle cannot be returned as it was not borrowed."));
            }

            borrowRequest.setStatus("Return Requested");
            cycleBorrowRepository.save(borrowRequest);

            // ✅ Notify Admin via WebSockets
            messagingTemplate.convertAndSend("/topic/admin/notifications",
                    "Return request submitted for cycle: " + returnRequest.getCycleId());

            return ResponseEntity.ok(Map.of("message", "Return request submitted successfully."));
        }

        return ResponseEntity.badRequest().body(Map.of("message", "No active borrow request found for this cycle."));
    }

    // ✅ Approve or Deny Return Request (Admin)
    @PostMapping("/approve-return/{id}")
    public ResponseEntity<?> approveCycleReturn(@PathVariable Long id) {
        Optional<CycleBorrow> borrowRequestOpt = cycleBorrowRepository.findById(id);
        if (borrowRequestOpt.isPresent() && "Return Requested".equals(borrowRequestOpt.get().getStatus())) {
            CycleBorrow borrowRequest = borrowRequestOpt.get();
            borrowRequest.setStatus("Returned");
            cycleBorrowRepository.save(borrowRequest);

            Optional<Cycle> cycleOpt = cycleRepository.findByCycleId(borrowRequest.getCycleId());
            if (cycleOpt.isPresent()) {
                Cycle cycle = cycleOpt.get();
                cycle.setBorrowedBy(null);
                cycle.setStatus("Available");
                cycleRepository.save(cycle);
            }

            // ✅ Notify User via WebSockets
            messagingTemplate.convertAndSend("/topic/user/" + borrowRequest.getBorrowerEmail(),
                    Map.of("message", "Your return request has been approved.", "status", "Returned"));

            return ResponseEntity.ok(Map.of("message", "Cycle return approved."));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Return request not found."));
    }

    @PostMapping("/deny-return/{id}")
    public ResponseEntity<?> denyCycleReturn(@PathVariable Long id) {
        Optional<CycleBorrow> borrowRequestOpt = cycleBorrowRepository.findById(id);
        if (borrowRequestOpt.isPresent() && "Return Requested".equals(borrowRequestOpt.get().getStatus())) {
            CycleBorrow borrowRequest = borrowRequestOpt.get();
            borrowRequest.setStatus("Return Denied");
            cycleBorrowRepository.save(borrowRequest);

            //Notify User via WebSockets
            messagingTemplate.convertAndSend("/topic/user/" + borrowRequest.getBorrowerEmail(),
                    Map.of("message", "Your return request has been denied.", "status", "Return Denied"));

            return ResponseEntity.ok(Map.of("message", "Cycle return request denied."));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Return request not found."));
    }
    @PostMapping("/request-key-borrow")
    public ResponseEntity<?> createBorrowRequest(@RequestBody KeyBorrow request){
        boolean isAvailable = keyBorrowService.isKeyAvailable(request.getKeyId(), request.getStartTime(),request.getEndTime());
        if (!isAvailable) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Key is not available during the requested time.");
        }
        Optional<Key> keyOpt = keyRepository.findByKeyIdAndLocation(request.getKeyId(), request.getLocation());
        Optional<KeyBorrow> borrowOpt = keyBorrowRepository.findByKeyIdAndBorrowerEmailAndStatus(request.getKeyId(), request.getBorrowerEmail(), "Pending");
        if(keyOpt.isEmpty() || !keyOpt.get().getStatus().equals("Available")){
            return ResponseEntity.badRequest().body("Key is Not Available get from CR");
        }
        if(borrowOpt.isPresent()){
            return ResponseEntity.badRequest().body("Key is Not Available get from CR");
        }
        Key key = keyOpt.get();
        key.setStatus("Pending");
        keyRepository.save(key);
        KeyBorrow borrowReq = KeyBorrow.builder()
                .keyId(request.getKeyId())
                .borrowerEmail(request.getBorrowerEmail())
                .requestTime(new Date())
                .location(request.getLocation())
                .status("Pending")
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .build();
        keyBorrowRepository.save(borrowReq);
        messagingTemplate.convertAndSend("/topic/admin/notifications",
                "New borrow request" + "for"+ request.getLocation()+" "+request.getKeyId()+"from " + request.getBorrowerEmail());
        return ResponseEntity.ok(Map.of("message","Borrow Request Submitted Successfully"));
    }
    @GetMapping("/key-borrow-requests")
    public ResponseEntity<?> getAllRequests(){
        System.out.println(keyBorrowRepository.findAll());
        return ResponseEntity.ok(keyBorrowRepository.findAll());
    }
    @PostMapping("/approve-key/{id}")
    public ResponseEntity<?> approveKeyRequest(@PathVariable Long id){
        Optional<KeyBorrow> borrowOpt = keyBorrowRepository.findById(id);
        System.out.println(borrowOpt);
        if(borrowOpt.isPresent() && borrowOpt.get().getStatus().equals("Pending")){
            KeyBorrow borrow = borrowOpt.get();
            Optional<Key> keyOpt = keyRepository.findByKeyIdAndLocation(borrow.getKeyId(),borrow.getLocation());
            if(keyOpt.isEmpty()){
                return ResponseEntity.badRequest().body("key not found");
            }
            Key key = keyOpt.get();
            key.setBorrowedBy(borrow.getBorrowerEmail());
            key.setBorrowedAt(new Date());
            key.setStatus("Borrowed");
            key.setStartTime(borrow.getStartTime());
            key.setEndTime(borrow.getEndTime());
            keyRepository.save(key);
            borrow.setStatus("Approved");
            borrow.setApprovalTime(new Date());
            keyBorrowRepository.save(borrow);
            messagingTemplate.convertAndSend("/topic/admin/notifications",
                    "Borrow request approved for " + borrow.getBorrowerEmail());
            messagingTemplate.convertAndSend("/topic/user/" + borrow.getBorrowerEmail(),
                    Map.of("message", "Your borrow request has been approved.", "status", "Approved"));
            return ResponseEntity.ok(Map.of("message","Borrow Request Has been Approved Successfully"));

        }
        return ResponseEntity.badRequest().body(Map.of("message","Request Not Found!!"));
    }
    @PostMapping("/deny-key/{id}")
    public ResponseEntity<?> denyKeyRequest(@PathVariable Long id){
        Optional<KeyBorrow> borrowOpt = keyBorrowRepository.findById(id);
        if(borrowOpt.isPresent()){
            KeyBorrow borrow = borrowOpt.get();
            Optional<Key> keyOpt = keyRepository.findByKeyIdAndLocation(borrow.getKeyId(),borrow.getLocation());
            if(keyOpt.isPresent()){
                Key key = keyOpt.get();
                key.setStatus("Available");
                key.setBorrowedBy(null);
                key.setBorrowedAt(null);
                keyRepository.save(key);
            }
            borrow.setStatus("Denied");
            keyBorrowRepository.save(borrow);
            messagingTemplate.convertAndSend("/topic/admin/notifications",
                    "Borrow request denied for " + borrow.getBorrowerEmail());
            messagingTemplate.convertAndSend("/topic/user/" + borrow.getBorrowerEmail(),
                    Map.of("message", "Your borrow request has been denied.", "status", "Denied"));

            return ResponseEntity.ok(Map.of("message", "Borrow request denied."));
        }
        return ResponseEntity.badRequest().body(Map.of("message","Request Not Found"));
    }
    @PostMapping("/changePassword")
    public ResponseEntity<?> changePassword(@RequestBody Map<String,Object> payload){
        String email = (String) payload.get("email");
        String oldPassword = (String) payload.get("password");
        String newPassword = (String) payload.get("newPassword");
        AuthenticationRequest request = new AuthenticationRequest(email,oldPassword);
        try{
            AuthenticationResponse response = authenticationService.authenticate(request);
            if(response != null){
                authenticationService.changePassword(email,newPassword);
                return ResponseEntity.ok(Map.of("message","Password Changed Successfully"));
            }
            ResponseEntity.badRequest().body(Map.of("message","Invalid Credentials"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("{\"error\": \"Invalid credentials\"}");
        }
        return ResponseEntity.badRequest().body(Map.of("message","Invalid Credentials"));
    }
    // ✅ User Requests to Return Key
    @PostMapping("/request-key-return")
    public ResponseEntity<?> requestKeyReturn(@RequestBody KeyBorrow returnRequest) {
        Optional<KeyBorrow> borrowRequestOpt = keyBorrowRepository
                .findByKeyIdAndBorrowerEmailAndStatus(returnRequest.getKeyId(), returnRequest.getBorrowerEmail(), "Approved");

        if (borrowRequestOpt.isPresent()) {
            KeyBorrow borrowRequest = borrowRequestOpt.get();
            borrowRequest.setStatus("Return Requested");
            keyBorrowRepository.save(borrowRequest);

            // ✅ Notify Admin via WebSockets
            messagingTemplate.convertAndSend("/topic/admin/notifications",
                    "Return request submitted for key: " + returnRequest.getKeyId());

            return ResponseEntity.ok(Map.of("message", "Return request submitted successfully."));
        }

        return ResponseEntity.badRequest().body(Map.of("message", "No active borrow request found for this key."));
    }

    // ✅ Admin Approves Key Return
    @PostMapping("/approve-key-return/{id}")
    public ResponseEntity<?> approveKeyReturn(@PathVariable Long id) {
        Optional<KeyBorrow> borrowRequestOpt = keyBorrowRepository.findById(id);
        if (borrowRequestOpt.isPresent() && "Return Requested".equals(borrowRequestOpt.get().getStatus())) {
            KeyBorrow borrowRequest = borrowRequestOpt.get();
            borrowRequest.setStatus("Returned");
            keyBorrowRepository.save(borrowRequest);

            Optional<Key> keyOpt = keyRepository.findByKeyId(borrowRequest.getKeyId());
            if (keyOpt.isPresent()) {
                Key key = keyOpt.get();
                key.setBorrowedBy(null);
                key.setBorrowedAt(null);
                key.setStatus("Available");
                key.setStartTime(null);
                key.setEndTime(null);
                keyRepository.save(key);
            }

            // ✅ Notify User via WebSockets
            messagingTemplate.convertAndSend("/topic/user/" + borrowRequest.getBorrowerEmail(),
                    Map.of("message", "Your key return request has been approved.", "status", "Returned"));

            return ResponseEntity.ok(Map.of("message", "Key return approved."));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Return request not found."));
    }

    // ✅ Admin Denies Key Return
    @PostMapping("/deny-key-return/{id}")
    public ResponseEntity<?> denyKeyReturn(@PathVariable Long id) {
        Optional<KeyBorrow> borrowRequestOpt = keyBorrowRepository.findByIdAndStatus(id,"Return Requested");
        if (borrowRequestOpt.isPresent() && "Return Requested".equals(borrowRequestOpt.get().getStatus())) {
            KeyBorrow borrowRequest = borrowRequestOpt.get();
            borrowRequest.setStatus("Return Denied");
            keyBorrowRepository.save(borrowRequest);

            // ✅ Notify User via WebSockets
            messagingTemplate.convertAndSend("/topic/user/" + borrowRequest.getBorrowerEmail(),
                    Map.of("message", "Your key return request has been denied.", "status", "Return Denied"));

            return ResponseEntity.ok(Map.of("message", "Key return request denied."));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Return request not found."));
    }
    @GetMapping("/borrowed-keys/{email}")
    public ResponseEntity<?> getBorrowedKeysByEmail(@PathVariable String email){
        return ResponseEntity.ok(keyBorrowRepository.findByBorrowerEmailAndStatus(email,"Approved"));
    }
}
