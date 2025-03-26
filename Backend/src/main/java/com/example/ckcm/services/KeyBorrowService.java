package com.example.ckcm.services;

import com.example.ckcm.entities.Key;
import com.example.ckcm.entities.KeyBorrow;
import com.example.ckcm.entities.Role;
import com.example.ckcm.entities.User;
import com.example.ckcm.repositories.KeyBorrowRepository;
import com.example.ckcm.repositories.KeyRepository;
import com.example.ckcm.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KeyBorrowService {
    private final KeyRepository keyRepository;
    private final UserRepository userRepository;
    private final KeyBorrowRepository keyBorrowRepository;

//    public String requestForKey(String keyId, String emailId,int duration){
//        Optional<User> userOpt = userRepository.findByEmail(emailId);
//        Optional<Key> keyOpt = keyRepository.findByKeyId(keyId);
//        if(userOpt.isPresent() && keyOpt.isPresent()){
//            if(keyOpt.get().getBorrowedBy() == null){
//                User user = userOpt.get();
//                Key key = keyOpt.get();
//                if(user.getRole()!= Role.CR){
//                    return "You are not authorized to borrow keys";
//                }
//                if(keyRepository.findByBorrowedBy(emailId).isPresent()){
//                    return "You have already borrowed a key, Please return the key to borrow another one";
//                }
//                if(!key.getStatus().equals("Available")){
//                    return "Key is not available";
//                }
//                KeyBorrow keyBorrow = new KeyBorrow();
//                keyBorrow.setKeyId(keyId);
//                keyBorrow.setBorrowerEmail(emailId);
//                keyBorrow.setDuration(duration);
//                keyBorrow.setRequestTime(new Date());
//                keyBorrow.setStatus("Pending");
//                keyRepository.save(key);
//                return "Key Borrow Requested Submitted Successfully!! Waiting For Admin`s Approval";
//            }
//        }
//        return "Key or User not found";
//    }
    public String approveKeyRequest(String keyId){
        Optional<KeyBorrow> requestOpt = keyBorrowRepository.findByKeyId(keyId);
        if(requestOpt.isPresent()){
            KeyBorrow keyBorrow = requestOpt.get();
            Optional<Key> keyOpt = keyRepository.findByKeyId(keyId);
            if(keyOpt.isPresent()){
                Key key = keyOpt.get();
                key.setBorrowedBy(keyBorrow.getBorrowerEmail());
                key.setStatus("Borrowed");
                keyRepository.save(key);
                keyBorrow.setStatus("Approved");
                keyBorrow.setApprovalTime(new Date());
                keyBorrowRepository.save(keyBorrow);
                return "Key Borrow Request Approved Successfully!!";
            }
        }
        return "Key Borrow Request not found";
    }
    public String rejectKeyRequest(String keyId){
        Optional<KeyBorrow> requestOpt = keyBorrowRepository.findByKeyId(keyId);
        if(requestOpt.isPresent()){
            KeyBorrow keyBorrow = requestOpt.get();
            keyBorrow.setStatus("Rejected");
            keyBorrow.setApprovalTime(new Date());
            keyBorrowRepository.save(keyBorrow);
            return "Key Borrow Request Rejected Successfully!!";
        }
        return "Key Borrow Request not found";
    }
    @Scheduled(fixedRate = 6000*5)
    public void durationexpiredHandler(){
        List<KeyBorrow> approvedRequest = keyBorrowRepository.findByStatus("Approved");
        Date now = new Date();
        for(KeyBorrow request:approvedRequest){
            long elapsedTime = (now.getTime() - request.getApprovalTime().getTime())/(60 * 1000);
            if(elapsedTime > request.getDuration()){
                Optional<Key> keyOpt = keyRepository.findByKeyId(request.getKeyId());
                if(keyOpt.isPresent()){
                    Key key = keyOpt.get();
                    key.setBorrowedBy(null);
                    key.setStatus("Available");
                    keyRepository.save(key);
                }
                request.setStatus("Completed");
                keyBorrowRepository.save(request);
            }
        }
    }
    public boolean isKeyAvailable(String keyId, LocalTime startTime, LocalTime endTime) {
        List<KeyBorrow> existingRequests = keyBorrowRepository.findByKeyIdAndStatus(keyId, "Approved");

        for (KeyBorrow request : existingRequests) {
            if (request.getStartTime().isBefore(endTime) && request.getEndTime().isAfter(startTime)) {
                return false; // Overlapping booking exists
            }
        }
        return true;
    }

}
