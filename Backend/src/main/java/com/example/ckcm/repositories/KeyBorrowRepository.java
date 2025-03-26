package com.example.ckcm.repositories;

import com.example.ckcm.entities.KeyBorrow;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KeyBorrowRepository extends JpaRepository<KeyBorrow,Long> {
    Optional<KeyBorrow> findByKeyId(String keyId);
    List<KeyBorrow> findByStatus(String status);
    List<KeyBorrow> findByKeyIdAndStatus(String keyId,String status);
    List<KeyBorrow> findByBorrowerEmail(String borrowerEmail);
    List<KeyBorrow> findByBorrowerEmailAndStatus(String borrowerEmail,String status);
    Optional<KeyBorrow> findByKeyIdAndBorrowerEmailAndStatus(String keyId,String borrowerEmail,String status);

    Optional<KeyBorrow> findByIdAndStatus(Long id, String returnRequested);

    List<KeyBorrow> findByBorrowFrom(String email);

    Optional<KeyBorrow> findByKeyIdAndBorrowFromAndStatus(String keyId, String borrowerEmail, String status);
}
