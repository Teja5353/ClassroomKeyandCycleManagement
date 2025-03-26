package com.example.ckcm.services;

import com.example.ckcm.entities.Cycle;
import com.example.ckcm.entities.User;
import com.example.ckcm.repositories.CycleRepository;
import com.example.ckcm.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CycleBorrowService {
    private final CycleRepository cycleRepository;
    private final UserRepository userRepository;
//    public String borrowCycleById(String emailId, String cycleId){
//        Optional<User> userOpt = userRepository.findByEmail(emailId);
//        Optional<Cycle> cycleOpt = cycleRepository.findByCycleId(cycleId);
//        if(userOpt.isPresent() && cycleOpt.isPresent()){
//            if(cycleOpt.get().getBorrowedBy() == null){
//                User user  = userOpt.get();
//                Cycle cycle = cycleOpt.get();
//                if(cycleRepository.findByBorrowedBy(emailId).isPresent()){
//                    return "You have already borrowed a cycle, Please return the cycle to borrow another one";
//                }
//                if(!cycle.getStatus().equals("Available")){
//                    return "Cycle is not available";
//                }
//                cycle.setStatus("Borrowed");
//                cycle.setBorrowedBy(user.getEmail());
//                cycleRepository.save(cycle);
//                return "Cycle borrowed successfully";
//            }
//        }
//        return "Cycle or User not found";
//    }
//    public String borrowCycleByQr(String emailId, String qrCode){
//        Optional<User> userOpt = userRepository.findByEmail(emailId);
//        Optional<Cycle> cycleOpt = cycleRepository.findByQrCode(qrCode);
//        if(userOpt.isPresent() && cycleOpt.isPresent()){
//            if(cycleOpt.get().getBorrowedBy() == null){
//                User user  = userOpt.get();
//                Cycle cycle = cycleOpt.get();
//                if(cycleRepository.findByBorrowedBy(emailId).isPresent()){
//                    return "You have already borrowed a cycle, Please return the cycle to borrow another one";
//                }
//                if(!cycle.getStatus().equals("Available")){
//                    return "Cycle is not available";
//                }
//                cycle.setStatus("Borrowed");
//                cycle.setBorrowedBy(user.getEmail());
//                cycleRepository.save(cycle);
//                return "Cycle borrowed successfully";
//            }
//        }
//        return "Invalid QR code or User not found";
//    }
    public String returnCycleByQr(String qrCode, String emailId){
        Optional<Cycle> cycleOpt = cycleRepository.findByQrCode(qrCode);
        if(cycleOpt.isPresent()){
            Cycle cycle = cycleOpt.get();
            if(cycle.getBorrowedBy().equals(emailId)){
                cycle.setBorrowedBy("Available");
                cycle.setBorrowedBy(null);
                cycleRepository.save(cycle);
                return "Successfully returned cycle";
            }
        }
        return "Invalid QR code or It is not Borrowed by you";
    }
    public String returnCycleById(String cycleId, String emailId){
        Optional<Cycle> cycleOpt = cycleRepository.findByCycleId(cycleId);
        if(cycleOpt.isPresent()){
            Cycle cycle = cycleOpt.get();
            if(cycle.getBorrowedBy().equals(emailId)){
                cycle.setBorrowedBy("Available");
                cycle.setBorrowedBy(null);
                cycleRepository.save(cycle);
                return "Successfully returned cycle";
            }
        }
        return "Invalid QR code or It is not Borrowed by you";
    }
}
