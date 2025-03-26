package com.example.ckcm.repositories;

import com.example.ckcm.entities.Cycle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;


public interface CycleRepository extends JpaRepository<Cycle,Long> {
    Optional<Cycle> findByCycleId(String cycleId);
    Optional<Cycle> findByQrCode(String qrCode);
    List<Cycle> findByBorrowedBy(String email);
    List<Cycle> findByStatus(String status);
    boolean existsByCycleId(String cycleId);
    List<Cycle> findAll();

    void deleteByCycleId(String cycleId);
}