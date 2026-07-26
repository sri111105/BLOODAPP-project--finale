package com.hospital.bloodapp.service;

import com.hospital.bloodapp.model.DonationHistory;
import com.hospital.bloodapp.model.Donor;
import com.hospital.bloodapp.repository.DonationHistoryRepository;
import com.hospital.bloodapp.repository.DonorRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Service
public class BadgeService {

    private final DonorRepository donorRepository;
    private final DonationHistoryRepository donationHistoryRepository;

    public BadgeService(DonorRepository donorRepository, DonationHistoryRepository donationHistoryRepository) {
        this.donorRepository = donorRepository;
        this.donationHistoryRepository = donationHistoryRepository;
    }

    @Transactional
    public DonationHistory recordDonation(Long donorId, LocalDate date, String location) {
        Donor donor = donorRepository.findById(donorId).orElse(null);
        if (donor == null) {
            throw new IllegalArgumentException("Donor not found with ID: " + donorId);
        }

        // Add donation history entry
        DonationHistory history = new DonationHistory();
        history.setDonor(donor);
        history.setDonationDate(date);
        history.setLocation(location);
        donationHistoryRepository.save(history);

        // Update donor stats
        int newCount = (donor.getDonationCount() != null ? donor.getDonationCount() : 0) + 1;
        donor.setDonationCount(newCount);
        donor.setLastDonationDate(date);
        
        // Compute and store badge level (though we will also compute it live on read for accuracy)
        donor.setBadgeLevel(calculateBadge(date));
        donorRepository.save(donor);

        return history;
    }

    public Map<String, Object> getBadgeInfo(Long donorId) {
        Donor donor = donorRepository.findById(donorId).orElse(null);
        if (donor == null) {
            return Map.of("donationCount", 0, "badgeLevel", "None", "livesSaved", 0);
        }

        int count = donor.getDonationCount() != null ? donor.getDonationCount() : 0;
        String badge = calculateBadge(donor.getLastDonationDate());
        int livesSaved = count * 3;

        return Map.of(
            "donationCount", count,
            "badgeLevel", badge,
            "livesSaved", livesSaved
        );
    }

    public String calculateBadge(LocalDate lastDonationDate) {
        if (lastDonationDate == null) {
            return "None";
        }
        long days = ChronoUnit.DAYS.between(lastDonationDate, LocalDate.now());
        if (days >= 90) {
            return "Gold";
        } else if (days >= 60) {
            return "Silver";
        } else if (days >= 0) {
            return "Bronze";
        }
        return "None";
    }
}
