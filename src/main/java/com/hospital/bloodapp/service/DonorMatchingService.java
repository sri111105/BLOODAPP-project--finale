package com.hospital.bloodapp.service;

import com.hospital.bloodapp.model.BloodRequest;
import com.hospital.bloodapp.model.Donor;
import com.hospital.bloodapp.repository.DonorRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DonorMatchingService {

    private final DonorRepository donorRepository;
    private final NotificationService notificationService;

    public DonorMatchingService(DonorRepository donorRepository, NotificationService notificationService) {
        this.donorRepository = donorRepository;
        this.notificationService = notificationService;
    }

    public List<Donor> matchDonors(BloodRequest request) {
        List<Donor> allMatched;
        String bloodGroup = request.getBloodGroup();
        String city = request.getCity();

        if (city == null || city.trim().isEmpty()) {
            if (request.getLocation() != null && !request.getLocation().trim().isEmpty()) {
                city = extractCity(request.getLocation());
            }
        }

        if (city != null && !city.trim().isEmpty()) {
            List<Donor> cityMatches = donorRepository.findByBloodGroupAndCity(bloodGroup, city);
            if (cityMatches.isEmpty()) {
                final String finalCity = city;
                allMatched = donorRepository.findByBloodGroup(bloodGroup).stream()
                    .filter(d -> d.getCity() != null && d.getCity().toLowerCase().contains(finalCity.toLowerCase()))
                    .collect(Collectors.toList());
                if (allMatched.isEmpty()) {
                    allMatched = donorRepository.findByBloodGroup(bloodGroup);
                }
            } else {
                allMatched = cityMatches;
            }
        } else {
            allMatched = donorRepository.findByBloodGroup(bloodGroup);
        }

        // Apply Level filter
        String level = request.getLevel() != null ? request.getLevel().trim() : "Urgent";
        LocalDate limitDate = LocalDate.now().minusDays(90);

        List<Donor> filteredDonors;
        if ("Urgent".equalsIgnoreCase(level)) {
            filteredDonors = allMatched.stream()
                .filter(d -> d.getLastDonationDate() == null || d.getLastDonationDate().isBefore(limitDate) || d.getLastDonationDate().isEqual(limitDate))
                .collect(Collectors.toList());
        } else if ("New Register".equalsIgnoreCase(level)) {
            filteredDonors = allMatched.stream()
                .filter(d -> d.getLastDonationDate() == null)
                .collect(Collectors.toList());
        } else {
            // "Donor" level: match all existing donors
            filteredDonors = allMatched;
        }

        // Send SMS/Email notifications only to those matched in the filtered list who are eligible (not in cooldown)
        for (Donor donor : filteredDonors) {
            boolean isEligible = donor.getLastDonationDate() == null || donor.getLastDonationDate().isBefore(limitDate) || donor.getLastDonationDate().isEqual(limitDate);
            if (isEligible) {
                notificationService.notifyDonor(donor, request.getUrgency(), request.getLocation());
            }
        }

        return filteredDonors;
    }

    private String extractCity(String location) {
        String[] cities = {"chennai", "madurai", "salem", "coimbatore", "trichy", "vellore", "tirunelveli", "thanjavur"};
        String lowerLoc = location.toLowerCase();
        for (String city : cities) {
            if (lowerLoc.contains(city)) {
                return city.substring(0, 1).toUpperCase() + city.substring(1);
            }
        }
        return location;
    }
}
