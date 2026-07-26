package com.hospital.bloodapp.controller;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.hospital.bloodapp.model.Donor;
import com.hospital.bloodapp.model.Hospital;
import com.hospital.bloodapp.repository.DonorRepository;
import com.hospital.bloodapp.repository.HospitalRepository;

@RestController
@CrossOrigin
@RequestMapping("/blood")
public class DonorController {
 
    private final DonorRepository donorRepository;
    private final com.hospital.bloodapp.service.BadgeService badgeService;
    private final com.hospital.bloodapp.service.InventoryService inventoryService;
    private final com.hospital.bloodapp.service.GeocodingService geocodingService;
    private final HospitalRepository hospitalRepository;
 
    public DonorController(DonorRepository donorRepository,
                           com.hospital.bloodapp.service.BadgeService badgeService,
                           com.hospital.bloodapp.service.InventoryService inventoryService,
                           com.hospital.bloodapp.service.GeocodingService geocodingService,
                           HospitalRepository hospitalRepository) {
        this.donorRepository = donorRepository;
        this.badgeService = badgeService;
        this.inventoryService = inventoryService;
        this.geocodingService = geocodingService;
        this.hospitalRepository = hospitalRepository;
    }
 
    // GET all donors
    @GetMapping
    public List<Donor> getDonors(
            @org.springframework.web.bind.annotation.RequestParam(required = false) Boolean visibleOnly,
            @org.springframework.web.bind.annotation.RequestParam(required = false) Long hospitalId) {
        List<Donor> donors;
        if (hospitalId != null) {
            donors = donorRepository.findByHospitalId(hospitalId);
        } else if (visibleOnly != null && visibleOnly) {
            donors = donorRepository.findByVisibleToHospital(true);
        } else {
            donors = donorRepository.findAll();
        }
        for (Donor d : donors) {
            d.setBadgeLevel(badgeService.calculateBadge(d.getLastDonationDate()));
        }
        return donors;
    }
 
    // POST add donor — also increments blood inventory stock immediately on registration
    @PostMapping
    public Donor addDonor(@RequestBody Donor donor) {
        System.out.println("NAME RECEIVED = " + donor.getName());

        // Geocode the free-text city/address via Nominatim
        String locationText = donor.getAddress() != null && !donor.getAddress().trim().isEmpty()
                ? donor.getAddress() + ", " + (donor.getCity() != null ? donor.getCity() : "")
                : donor.getCity();
        if (locationText != null && !locationText.trim().isEmpty()) {
            double[] coords = geocodingService.geocodeCity(locationText);
            donor.setLatitude(coords[0]);
            donor.setLongitude(coords[1]);
        }

        Donor saved = donorRepository.save(donor);

        // Override X: Increment inventory stock immediately on registration
        if (saved.getHospitalId() != null && saved.getBloodGroup() != null) {
            try {
                Hospital hospital = hospitalRepository.findById(saved.getHospitalId()).orElse(null);
                if (hospital != null) {
                    inventoryService.incrementStock(hospital.getName(), saved.getBloodGroup(), 1);
                    System.out.println("[Stock] Incremented " + saved.getBloodGroup()
                            + " at " + hospital.getName() + " on registration of " + saved.getName());
                }
            } catch (Exception e) {
                System.err.println("[Stock] Failed to increment on registration: " + e.getMessage());
            }
        }

        return saved;
    }
 
    // DELETE donor by id
    @DeleteMapping("/{id}")
    public String deleteDonor(@PathVariable Long id) {
        donorRepository.deleteById(id);
        return "Deleted";
    }

    // POST record donation (repeat/follow-up donation)
    @PostMapping("/{id}/donation")
    public com.hospital.bloodapp.model.DonationHistory recordDonation(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        String location = body.getOrDefault("location", "City Hospital");
        String dateStr = body.get("donationDate");
        java.time.LocalDate date = dateStr != null ? java.time.LocalDate.parse(dateStr) : java.time.LocalDate.now();
        
        com.hospital.bloodapp.model.DonationHistory history = badgeService.recordDonation(id, date, location);
        
        // Mark as Donated: also increments stock for repeat/follow-up donation
        Donor donor = donorRepository.findById(id).orElse(null);
        if (donor != null) {
            inventoryService.incrementStock(location, donor.getBloodGroup(), 1);
        }
        
        return history;
    }

    // GET donor badge details
    @GetMapping("/{id}/badge")
    public Map<String, Object> getBadge(@PathVariable Long id) {
        return badgeService.getBadgeInfo(id);
    }
}
