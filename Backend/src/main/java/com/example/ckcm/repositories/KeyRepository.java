package com.example.ckcm.repositories;

import com.example.ckcm.entities.Key;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface KeyRepository extends JpaRepository<Key,Long> {
    Optional<Key> findByKeyId(String keyId);
    List<Key> findByBorrowedBy(String email);
    List<Key> findByStatus(String status);
    Optional<Key> findByKeyIdAndLocation(String keyId, String location);
    void deleteByKeyIdAndLocation(String keyId,String location);
}
