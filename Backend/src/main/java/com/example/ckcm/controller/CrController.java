package com.example.ckcm.controller;

import com.example.ckcm.entities.Key;
import com.example.ckcm.entities.KeyBorrow;
import com.example.ckcm.repositories.KeyBorrowRepository;
import com.example.ckcm.repositories.KeyRepository;
import com.example.ckcm.services.KeyBorrowService;
import com.example.ckcm.services.KeyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/cr")
@CrossOrigin(origins = "http://localhost:4200")
public class CrController {
    @Autowired
    private KeyRepository keyRepository;
    @Autowired
    private KeyService keyService;
    @Autowired
    private KeyBorrowService keyBorrowService;
    @Autowired
    private KeyBorrowRepository keyBorrowRepository;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @PostMapping("/request-key-borrow")
    public ResponseEntity<?> requestKeyFromCr(@RequestBody KeyBorrow borrow){
        if(!keyBorrowService.isKeyAvailable(borrow.getKeyId(),borrow.getStartTime(),borrow.getEndTime())){
            return ResponseEntity.badRequest().body("Key is not available for the requested time");
        }
        if(!keyRepository.findByBorrowedBy(borrow.getBorrowerEmail()).isEmpty()){
            return ResponseEntity.badRequest().body("You have already borrowed a key, Please return the key to borrow another one");
        }
        Key key = keyRepository.findByKeyIdAndLocation(borrow.getKeyId(),borrow.getLocation()).get();
        KeyBorrow borrowReq = KeyBorrow.builder()
                .keyId(borrow.getKeyId())
                .location(borrow.getLocation())
                .borrowerEmail(borrow.getBorrowerEmail())
                .requestTime(new Date())
                .startTime(borrow.getStartTime())
                .endTime(borrow.getEndTime())
                .status("Pending")
                .borrowFrom(key.getBorrowedBy())
                .build();
        keyBorrowRepository.save(borrowReq);
        messagingTemplate.convertAndSend("/topic/user/"+key.getBorrowedBy(),
                Map.of(
                "message","Borrow request for key: "+borrow.getLocation()+"-"+borrow.getKeyId() + " has been submitted"));
        messagingTemplate.convertAndSend("/topic/user/" + borrowReq.getBorrowerEmail(),
                Map.of("message", "Your Borrow Request for key: "+borrowReq.getLocation()+"-"+borrowReq.getKeyId() + " has been submitted","status","Pending"));
        return ResponseEntity.ok(Map.of("message", "Key Borrow Requested Submitted Successfully!! Waiting For CR`s Approval"));
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
            key.setBorrowedAt(new Date());
            keyRepository.save(key);
            borrow.setStatus("Approved");
            borrow.setApprovalTime(new Date());
            keyBorrowRepository.save(borrow);
            messagingTemplate.convertAndSend("/topic/user/"+key.getOwner(),
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
            Key key = keyOpt.get();
//            if(keyOpt.isPresent()){
//                Key key = keyOpt.get();
//                key.setStatus("Available");
//                key.setBorrowedBy(null);
//                key.setBorrowedAt(null);
//                keyRepository.save(key);
//            }
            borrow.setStatus("Denied");
            keyBorrowRepository.save(borrow);
            messagingTemplate.convertAndSend("/topic/user/"+key.getOwner(),
                    "Borrow request denied for " + borrow.getBorrowerEmail());
            messagingTemplate.convertAndSend("/topic/user/" + borrow.getBorrowerEmail(),
                    Map.of("message", "Your borrow request has been denied.", "status", "Denied"));

            return ResponseEntity.ok(Map.of("message", "Borrow request denied."));
        }
        return ResponseEntity.badRequest().body(Map.of("message","Request Not Found"));
    }
    @GetMapping("/crBorrow/{email}")
    public ResponseEntity<List<KeyBorrow>> fetchCrBorrowRequests(@PathVariable String email){
        return ResponseEntity.ok(keyBorrowRepository.findByBorrowFrom(email));
    }
    @PostMapping("/request-key-return")
    public ResponseEntity<?> requestReturnKey(@RequestBody KeyBorrow borrow){
        Optional<KeyBorrow> borrowOpt = keyBorrowRepository.findByKeyIdAndBorrowerEmailAndStatus(borrow.getKeyId(),borrow.getBorrowerEmail(),"Approved");
        Optional<KeyBorrow> borrowOpt1 = keyBorrowRepository.findByKeyIdAndBorrowFromAndStatus(borrow.getKeyId(),borrow.getBorrowerEmail(),"Approved");
        if(borrowOpt.isPresent()){
            KeyBorrow borrowReq = borrowOpt.get();
            borrowReq.setStatus("Return Requested");
            borrowReq.setBorrowFrom(borrowOpt1.get().getBorrowerEmail());
            keyBorrowRepository.save(borrowReq);
            messagingTemplate.convertAndSend("/topic/user/"+borrowOpt1.get().getBorrowerEmail(),
                    " Key Handover Request: "+borrow.getLocation()+"-"+borrow.getKeyId() + " has been submitted");
            messagingTemplate.convertAndSend("/topic/user/" + borrow.getBorrowerEmail(),
                    Map.of("message", "Your Return Request for key: "+borrow.getLocation()+"-"+borrow.getKeyId() + " has been submitted","status","Return Requested"));
            return ResponseEntity.ok(Map.of("message","Return Request Submitted Successfully!! Waiting For CR`s Approval"));
        }
        return ResponseEntity.badRequest().body(Map.of("message","Request Not Found"));
    }
    @PostMapping("/approve-key-return/{id}")
    public ResponseEntity<?> approveKeyReturn(@PathVariable Long id,@RequestBody KeyBorrow borrow) {
        Optional<KeyBorrow> borrowRequestOpt = keyBorrowRepository.findById(id);
        Optional<KeyBorrow> borrow1 = keyBorrowRepository.findByKeyIdAndBorrowFromAndStatus(borrow.getKeyId(), borrowRequestOpt.get().getBorrowerEmail(),"Approved");
        if (borrowRequestOpt.isPresent() && "Return Requested".equals(borrowRequestOpt.get().getStatus())) {
            KeyBorrow borrowRequest = borrowRequestOpt.get();
            borrowRequest.setStatus("Returned");
            keyBorrowRepository.save(borrowRequest);

            Optional<Key> keyOpt = keyRepository.findByKeyId(borrowRequest.getKeyId());
            if (keyOpt.isPresent()) {
                Key key = keyOpt.get();
                key.setBorrowedBy(borrow.getBorrowerEmail());
                key.setBorrowedAt(new Date());
                key.setStatus("Borrowed");
                key.setStartTime(borrow1.get().getStartTime());
                key.setEndTime(borrow1.get().getEndTime());
                key.setOwner(borrow.getBorrowerEmail());
                keyRepository.save(key);
            }

            // ✅ Notify User via WebSockets
            messagingTemplate.convertAndSend("/topic/user/" + borrowRequest.getBorrowerEmail(),
                    Map.of("message", "Your key return request has been approved.", "status", "Returned"));
            messagingTemplate.convertAndSend("/topic/user/" + borrow.getBorrowerEmail(),
                    Map.of("message", "Your key has been successfully recieved from: "+borrowRequest.getBorrowerEmail(), "status", "Returned"));

            return ResponseEntity.ok(Map.of("message", "Key return approved."));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Return request not found."));
    }

    // ✅ Admin Denies Key Return
    @PostMapping("/deny-key-return/{id}")
    public ResponseEntity<?> denyKeyReturn(@PathVariable Long id, @RequestBody KeyBorrow borrow) {
        Optional<KeyBorrow> borrowRequestOpt = keyBorrowRepository.findByIdAndStatus(id,"Return Requested");
        Optional<KeyBorrow> borrow1  = keyBorrowRepository.findByKeyIdAndBorrowFromAndStatus(borrow.getKeyId(),borrow.getBorrowerEmail(),"Approved");
        if (borrowRequestOpt.isPresent() && "Return Requested".equals(borrowRequestOpt.get().getStatus())) {
            KeyBorrow borrowRequest = borrowRequestOpt.get();
            borrowRequest.setStatus("Approved");
            KeyBorrow borrow2 = borrow1.get();
            borrow2.setStatus("Denied");
            keyBorrowRepository.save(borrow2);
            keyBorrowRepository.save(borrowRequest);
            // ✅ Notify User via WebSockets
            messagingTemplate.convertAndSend("/topic/user/" + borrowRequest.getBorrowerEmail(),
                    Map.of("message", "Your key return request has been denied.", "status", "Return Denied"));

            return ResponseEntity.ok(Map.of("message", "Key return request denied."));
        }
        return ResponseEntity.badRequest().body(Map.of("message", "Return request not found."));
    }
}
