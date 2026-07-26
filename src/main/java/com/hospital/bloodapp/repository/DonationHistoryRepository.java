package com.hospital.bloodapp.repository;

import com.hospital.bloodapp.model.DonationHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DonationHistoryRepository extends JpaRepository<DonationHistory, Long> {
    List<DonationHistory> findByDonorId(Long donorId);
    List<DonationHistory> findByDonationDate(java.time.LocalDate donationDate);
}
