package com.hospital.bloodapp.service;

import com.hospital.bloodapp.model.DonationHistory;
import com.hospital.bloodapp.repository.DonationHistoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
public class BloodExpiryScheduler {

    private final DonationHistoryRepository donationHistoryRepository;
    private final InventoryService inventoryService;

    @Value("${blood.shelf.life.days:90}")
    private int shelfLifeDays;

    public BloodExpiryScheduler(DonationHistoryRepository donationHistoryRepository, InventoryService inventoryService) {
        this.donationHistoryRepository = donationHistoryRepository;
        this.inventoryService = inventoryService;
    }

    // Runs daily at midnight to decrement expired blood units from stock
    @Scheduled(cron = "0 0 0 * * *")
    public void checkExpiredBlood() {
        LocalDate expiryDate = LocalDate.now().minusDays(shelfLifeDays);
        List<DonationHistory> expiredDonations = donationHistoryRepository.findByDonationDate(expiryDate);

        System.out.println("[SHELF LIFE SCHEDULER] Checking expired blood for date: " + expiryDate + ". Found count: " + expiredDonations.size());

        for (DonationHistory donation : expiredDonations) {
            if (donation.getDonor() != null) {
                String bloodGroup = donation.getDonor().getBloodGroup();
                String location = donation.getLocation(); // Hospital Name
                if (location != null && bloodGroup != null) {
                    System.out.println("[EXPIRED BLOOD] Decrementing 1 unit of " + bloodGroup + " at " + location + " due to shelf-life expiry");
                    inventoryService.decrementStock(location, bloodGroup, 1);
                }
            }
        }
    }
}
