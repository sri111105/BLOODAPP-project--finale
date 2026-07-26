package com.hospital.bloodapp.repository;

import com.hospital.bloodapp.model.BloodInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.List;

public interface BloodInventoryRepository extends JpaRepository<BloodInventory, Long> {
    Optional<BloodInventory> findByHospitalNameAndBloodGroup(String hospitalName, String bloodGroup);
    List<BloodInventory> findByHospitalName(String hospitalName);
}