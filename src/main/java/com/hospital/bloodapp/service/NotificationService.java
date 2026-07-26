package com.hospital.bloodapp.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.BodyInserters;
import com.hospital.bloodapp.model.Donor;

@Service
public class NotificationService {

    @Value("${twilio.account.sid}")
    private String accountSid;

    @Value("${twilio.auth.token}")
    private String authToken;

    @Value("${twilio.from.number}")
    private String fromNumber;

    public void notifyDonor(Donor donor, String urgency, String location) {
        String msg = "URGENT - Blood Group " + donor.getBloodGroup() + " needed at " + location + ". Urgency: " + urgency;
        sendSms(donor.getPhone(), msg);
    }

    public boolean sendDirectSms(Donor donor, String hospitalName) {
        String message = "Urgent: " + donor.getBloodGroup() + " blood needed at " + hospitalName + ". Please respond if available.";
        return sendSms(donor.getPhone(), message);
    }

    private boolean sendSms(String toPhone, String messageText) {
        String formattedPhone = toPhone.trim().replaceAll("[^0-9+]", "");
        if (!formattedPhone.startsWith("+")) {
            if (formattedPhone.length() == 10) {
                formattedPhone = "+91" + formattedPhone;
            } else {
                formattedPhone = "+" + formattedPhone;
            }
        }

        System.out.println("---------------- SMS SEND SIMULATION ----------------");
        System.out.println("To: " + formattedPhone + " (original: " + toPhone + ")");
        System.out.println("Message: " + messageText);
        System.out.println("-----------------------------------------------------");

        // If credentials are placeholder values, don't execute real call
        if (accountSid == null || accountSid.startsWith("ACxxxx") || authToken == null || authToken.equals("xxxx")) {
            System.out.println("[SMS STUB] Twilio credentials are not set or default. Skipping actual request.");
            return true;
        }

        try {
            String twilioUrl = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";
            
            WebClient webClient = WebClient.builder().build();
            String response = webClient.post()
                    .uri(twilioUrl)
                    .headers(headers -> headers.setBasicAuth(accountSid, authToken))
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .body(BodyInserters.fromFormData("To", formattedPhone)
                            .with("From", fromNumber)
                            .with("Body", messageText))
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            System.out.println("[TWILIO SUCCESS] Response: " + response);
            return true;
        } catch (Exception e) {
            System.err.println("[TWILIO ERROR] Failed to send SMS to " + formattedPhone + ": " + e.getMessage());
            return false;
        }
    }
}
