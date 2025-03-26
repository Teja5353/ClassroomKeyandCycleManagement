package com.example.ckcm.services;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import com.example.ckcm.repositories.KeyRepository;
import com.example.ckcm.entities.Key;

@Service
@RequiredArgsConstructor
public class KeyService {
    private final KeyRepository keyRepository;
    @Transactional
    public Key registerKey(String keyId, String Location){
        if(keyRepository.findByKeyIdAndLocation(keyId,Location).isPresent()){
            throw new IllegalArgumentException("Key already exists");
        }
        Key key = Key.builder()
                .keyId(keyId)
                .location(Location)
                .status("Available")
                .borrowedAt(null)
                .borrowedBy(null)
                .owner("Admin")
                .build();
        return keyRepository.save(key);
    }
    @Transactional
    public void deleteKey(String keyId,String location) {
        keyRepository.deleteByKeyIdAndLocation(keyId,location);
    }
}
