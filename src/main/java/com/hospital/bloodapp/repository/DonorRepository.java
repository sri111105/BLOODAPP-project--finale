package com.hospital.bloodapp.repository;

import com.hospital.bloodapp.model.Donor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DonorRepository extends JpaRepository<Donor, Long> {
    List<Donor> findByBloodGroupAndCity(String bloodGroup, String city);
    List<Donor> findByBloodGroup(String bloodGroup);
    List<Donor> findByVisibleToHospital(Boolean visibleToHospital);
    List<Donor> findByHospitalId(Long hospitalId);
}
