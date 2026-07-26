package com.hospital.bloodapp.service;

import com.hospital.bloodapp.model.Donor;
import com.hospital.bloodapp.repository.DonorRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ReminderService {

    private final DonorRepository donorRepository;
    private final NotificationService notificationService;

    public ReminderService(DonorRepository donorRepository, NotificationService notificationService) {
        this.donorRepository = donorRepository;
        this.notificationService = notificationService;
    }

    // Runs daily at 8:00 AM
    @Scheduled(cron = "0 0 8 * * *")
    public List<String> sendEligibilityReminders() {
        LocalDate today = LocalDate.now();
        // Donors who donated between 90 and 85 days ago will become eligible in the next 5 days
        LocalDate startDate = today.minusDays(90);
        LocalDate endDate = today.minusDays(85);

        List<Donor> donors = donorRepository.findAll();
        List<String> logs = new ArrayList<>();

        for (Donor donor : donors) {
            LocalDate lastDonation = donor.getLastDonationDate();
            if (lastDonation != null && !lastDonation.isBefore(startDate) && !lastDonation.isAfter(endDate)) {
                // Determine preferred language
                String lang = donor.getPreferredLanguage() != null ? donor.getPreferredLanguage() : "en";
                String message;
                
                if ("ta".equalsIgnoreCase(lang)) {
                    message = "அன்புள்ள " + donor.getName() + ", நீங்கள் இன்னும் சில நாட்களில் மீண்டும் இரத்தம் வழங்க தகுதி பெறுவீர்கள். உயிர்களைக் காப்பாற்ற நீங்கள் தயாரா?";
                } else {
                    message = "Dear " + donor.getName() + ", you will become eligible to donate blood again in the next few days. Are you ready to save lives?";
                }
                
                // Print simulation
                System.out.println("Reminder Service Triggered for: " + donor.getName() + " (" + donor.getPhone() + ")");
                System.out.println("Simulating SMS notification [" + lang.toUpperCase() + "]: " + message);
                
                logs.add("Reminded " + donor.getName() + " (" + donor.getPhone() + ") in " + lang.toUpperCase());
            }
        }
        return logs;
    }
}
