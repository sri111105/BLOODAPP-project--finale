package com.hospital.bloodapp.service;

import com.hospital.bloodapp.model.BloodInventory;
import com.hospital.bloodapp.repository.BloodInventoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class InventoryService {

    private final BloodInventoryRepository inventoryRepository;

    public InventoryService(BloodInventoryRepository inventoryRepository) {
        this.inventoryRepository = inventoryRepository;
    }

    public List<BloodInventory> getInventory(String hospitalName) {
        if (hospitalName == null || hospitalName.trim().isEmpty()) {
            return inventoryRepository.findAll();
        }
        return inventoryRepository.findByHospitalName(hospitalName);
    }

    @Transactional
    public BloodInventory updateInventory(String hospitalName, String bloodGroup, int unitsAvailable) {
        Optional<BloodInventory> existing = inventoryRepository.findByHospitalNameAndBloodGroup(hospitalName, bloodGroup);
        BloodInventory item;
        if (existing.isPresent()) {
            item = existing.get();
        } else {
            item = new BloodInventory();
            item.setHospitalName(hospitalName);
            item.setBloodGroup(bloodGroup);
        }
        item.setUnitsAvailable(unitsAvailable);
        item.setStatus(computeStatus(unitsAvailable));
        item.setLastUpdated(LocalDateTime.now());
        return inventoryRepository.save(item);
    }

    @Transactional
    public void incrementStock(String hospitalName, String bloodGroup, int units) {
        Optional<BloodInventory> existing = inventoryRepository.findByHospitalNameAndBloodGroup(hospitalName, bloodGroup);
        BloodInventory item;
        if (existing.isPresent()) {
            item = existing.get();
        } else {
            item = new BloodInventory();
            item.setHospitalName(hospitalName);
            item.setBloodGroup(bloodGroup);
            item.setUnitsAvailable(0);
        }
        int newQty = item.getUnitsAvailable() + units;
        item.setUnitsAvailable(newQty);
        item.setStatus(computeStatus(newQty));
        item.setLastUpdated(LocalDateTime.now());
        inventoryRepository.save(item);
    }

    @Transactional
    public void decrementStock(String hospitalName, String bloodGroup, int units) {
        Optional<BloodInventory> existing = inventoryRepository.findByHospitalNameAndBloodGroup(hospitalName, bloodGroup);
        if (existing.isPresent()) {
            BloodInventory item = existing.get();
            int newQty = Math.max(0, item.getUnitsAvailable() - units);
            item.setUnitsAvailable(newQty);
            item.setStatus(computeStatus(newQty));
            item.setLastUpdated(LocalDateTime.now());
            inventoryRepository.save(item);
        }
    }

    public String computeStatus(int units) {
        if (units > 10) {
            return "Good";
        } else if (units >= 5) {
            return "Medium";
        } else {
            return "Low";
        }
    }
}