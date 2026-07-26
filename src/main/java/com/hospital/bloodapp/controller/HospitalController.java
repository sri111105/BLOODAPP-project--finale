package com.hospital.bloodapp.controller;

import com.hospital.bloodapp.model.BloodRequest;
import com.hospital.bloodapp.model.Donor;
import com.hospital.bloodapp.repository.BloodRequestRepository;
import com.hospital.bloodapp.service.DonorMatchingService;
import com.hospital.bloodapp.service.RequestParserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/api/hospital")
public class HospitalController {
 
    private final RequestParserService requestParserService;
    private final DonorMatchingService donorMatchingService;
    private final BloodRequestRepository bloodRequestRepository;
    private final com.hospital.bloodapp.service.PdfGeneratorService pdfGeneratorService;
    private final com.hospital.bloodapp.service.InventoryService inventoryService;
    private final com.hospital.bloodapp.service.GeocodingService geocodingService;
    private final com.hospital.bloodapp.repository.DonorRepository donorRepository;
    private final com.hospital.bloodapp.service.LlmService llmService;
    private final com.hospital.bloodapp.service.ReminderService reminderService;
    private final com.hospital.bloodapp.repository.HospitalRepository hospitalRepository;
    private final com.hospital.bloodapp.service.NotificationService notificationService;
    private final com.hospital.bloodapp.service.BadgeService badgeService;
 
 
    public HospitalController(RequestParserService requestParserService, DonorMatchingService donorMatchingService, BloodRequestRepository bloodRequestRepository, com.hospital.bloodapp.service.PdfGeneratorService pdfGeneratorService, com.hospital.bloodapp.service.InventoryService inventoryService, com.hospital.bloodapp.service.GeocodingService geocodingService, com.hospital.bloodapp.repository.DonorRepository donorRepository, com.hospital.bloodapp.service.LlmService llmService, com.hospital.bloodapp.service.ReminderService reminderService, com.hospital.bloodapp.repository.HospitalRepository hospitalRepository, com.hospital.bloodapp.service.NotificationService notificationService, com.hospital.bloodapp.service.BadgeService badgeService) {
        this.requestParserService = requestParserService;
        this.donorMatchingService = donorMatchingService;
        this.bloodRequestRepository = bloodRequestRepository;
        this.pdfGeneratorService = pdfGeneratorService;
        this.inventoryService = inventoryService;
        this.geocodingService = geocodingService;
        this.donorRepository = donorRepository;
        this.llmService = llmService;
        this.reminderService = reminderService;
        this.hospitalRepository = hospitalRepository;
        this.notificationService = notificationService;
        this.badgeService = badgeService;
    }
 
    @PostMapping("/request")
    public ResponseEntity<BloodRequest> parseRequest(@RequestBody Map<String, String> request) {
        if (request.containsKey("rawText") && request.get("rawText") != null && !request.get("rawText").trim().isEmpty()) {
            String rawText = request.get("rawText");
            BloodRequest parsedRequest = requestParserService.parseRequest(rawText);
            if (parsedRequest != null) {
                parsedRequest.setLevel("Urgent");
                parsedRequest.setCity(extractCity(parsedRequest.getLocation()));
                bloodRequestRepository.save(parsedRequest);
                return ResponseEntity.ok(parsedRequest);
            }
            return ResponseEntity.badRequest().build();
        } else {
            BloodRequest manualRequest = new BloodRequest();
            manualRequest.setPatientName(request.get("patientName"));
            manualRequest.setBloodGroup(request.get("bloodGroup"));
            manualRequest.setCity(request.get("city"));
            manualRequest.setLocation(request.get("city"));
            manualRequest.setLevel(request.get("level"));
            manualRequest.setUnits(request.containsKey("units") && request.get("units") != null ? Integer.parseInt(request.get("units")) : 1);
            manualRequest.setUrgency("high");
            manualRequest.setRawText("Manual Request for " + request.get("patientName"));
            
            bloodRequestRepository.save(manualRequest);
            return ResponseEntity.ok(manualRequest);
        }
    }

    private String extractCity(String location) {
        if (location == null) return "Chennai";
        String[] cities = {"chennai", "madurai", "salem", "coimbatore", "trichy", "vellore", "tirunelveli", "thanjavur"};
        String lowerLoc = location.toLowerCase();
        for (String city : cities) {
            if (lowerLoc.contains(city)) {
                return city.substring(0, 1).toUpperCase() + city.substring(1);
            }
        }
        return "Chennai";
    }
 
    @PostMapping("/match")
    public ResponseEntity<Map<String, List<java.util.Map<String, Object>>>> matchDonors(@RequestBody Map<String, Object> request) {
        Long requestId = Long.valueOf(request.get("requestId").toString());
        String patientName = request.containsKey("patientName") ? request.get("patientName").toString() : null;

        BloodRequest bloodRequest = bloodRequestRepository.findById(requestId).orElse(null);
        if (bloodRequest != null) {
            if (patientName != null && !patientName.trim().isEmpty()) {
                bloodRequest.setPatientName(patientName);
                bloodRequestRepository.save(bloodRequest);
            }
            List<Donor> matchedDonors = donorMatchingService.matchDonors(bloodRequest);

            // Override Z: Geocode patient city and compute distance per donor
            String patientCity = bloodRequest.getCity() != null ? bloodRequest.getCity() : bloodRequest.getLocation();
            double[] patientCoords = geocodingService.geocodeCity(patientCity);
            java.time.LocalDate limitDate = java.time.LocalDate.now().minusDays(90);

            List<java.util.Map<String, Object>> enrichedDonors = new java.util.ArrayList<>();
            for (Donor d : matchedDonors) {
                double dLat = d.getLatitude() != null ? d.getLatitude() : 0;
                double dLng = d.getLongitude() != null ? d.getLongitude() : 0;
                if (dLat == 0 && dLng == 0 && d.getCity() != null) {
                    double[] coords = geocodingService.geocodeCity(d.getCity());
                    dLat = coords[0]; dLng = coords[1];
                    d.setLatitude(dLat); d.setLongitude(dLng);
                    donorRepository.save(d);
                }
                double distKm = geocodingService.calculateDistance(patientCoords[0], patientCoords[1], dLat, dLng);
                boolean isEligible = d.getLastDonationDate() == null
                        || d.getLastDonationDate().isBefore(limitDate)
                        || d.getLastDonationDate().isEqual(limitDate);

                java.util.Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", d.getId());
                map.put("name", d.getName());
                map.put("bloodGroup", d.getBloodGroup());
                map.put("city", d.getCity());
                map.put("phone", d.getPhone());
                map.put("latitude", dLat);
                map.put("longitude", dLng);
                map.put("distanceKm", Math.round(distKm * 10.0) / 10.0);
                map.put("eligibilityStatus", isEligible ? "Eligible" : "Not Eligible");
                map.put("lastDonationDate", d.getLastDonationDate() != null ? d.getLastDonationDate().toString() : null);
                map.put("badgeLevel", badgeService.calculateBadge(d.getLastDonationDate()));
                enrichedDonors.add(map);
            }

            // Sort by distance ascending
            enrichedDonors.sort((a, b) -> Double.compare((Double) a.get("distanceKm"), (Double) b.get("distanceKm")));

            // Auto-decrement stock based on matched units request
            String hospitalLoc = bloodRequest.getLocation() != null ? bloodRequest.getLocation() : "City Hospital";
            inventoryService.decrementStock(hospitalLoc, bloodRequest.getBloodGroup(), bloodRequest.getUnits());

            return ResponseEntity.ok(Map.of("matchedDonors", enrichedDonors));
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/request/{requestId}/pdf")
    public ResponseEntity<byte[]> getRequestPdf(@PathVariable Long requestId) {
        BloodRequest bloodRequest = bloodRequestRepository.findById(requestId).orElse(null);
        if (bloodRequest != null) {
            List<Donor> matchedDonors = donorMatchingService.matchDonors(bloodRequest);
            byte[] pdfBytes = pdfGeneratorService.generateConfirmationPdf(bloodRequest, matchedDonors);
            
            String filename = "request_" + (bloodRequest.getPatientName() != null ? bloodRequest.getPatientName().replaceAll("\\s+", "_") : "patient") + "_" + requestId + ".pdf";
            
            return ResponseEntity.ok()
                    .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(org.springframework.http.MediaType.APPLICATION_PDF)
                    .body(pdfBytes);
        }
        return ResponseEntity.notFound().build();
    }

    @GetMapping("/inventory")
    public ResponseEntity<List<com.hospital.bloodapp.model.BloodInventory>> getInventory(@RequestParam(required = false) String hospitalName) {
        List<com.hospital.bloodapp.model.BloodInventory> inventory = inventoryService.getInventory(hospitalName);
        return ResponseEntity.ok(inventory);
    }

    @PutMapping("/inventory")
    public ResponseEntity<com.hospital.bloodapp.model.BloodInventory> updateInventory(@RequestBody Map<String, Object> request) {
        String hospitalName = request.containsKey("hospitalName") ? request.get("hospitalName").toString() : "City Hospital";
        String bloodGroup = request.get("bloodGroup").toString();
        int unitsAvailable = Integer.parseInt(request.get("unitsAvailable").toString());

        com.hospital.bloodapp.model.BloodInventory updated = inventoryService.updateInventory(hospitalName, bloodGroup, unitsAvailable);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/donors/nearby")
    public ResponseEntity<List<Map<String, Object>>> getNearbyDonors(
            @RequestParam String bloodGroup,
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "100.0") double radiusKm) {
        
        List<Donor> allDonors = donorRepository.findByBloodGroup(bloodGroup);
        java.time.LocalDate limitDate = java.time.LocalDate.now().minusDays(90);
        List<Map<String, Object>> nearbyList = new java.util.ArrayList<>();
        
        for (Donor d : allDonors) {
            double dLat = d.getLatitude() != null ? d.getLatitude() : 0;
            double dLng = d.getLongitude() != null ? d.getLongitude() : 0;
            
            if (dLat == 0 && dLng == 0 && d.getCity() != null) {
                double[] coords = geocodingService.geocodeCity(d.getCity());
                dLat = coords[0];
                dLng = coords[1];
                d.setLatitude(dLat);
                d.setLongitude(dLng);
                donorRepository.save(d);
            }
            
            double distance = geocodingService.calculateDistance(lat, lng, dLat, dLng);
            if (distance <= radiusKm) {
                Map<String, Object> map = new java.util.HashMap<>();
                map.put("id", d.getId());
                map.put("name", d.getName());
                map.put("bloodGroup", d.getBloodGroup());
                map.put("city", d.getCity());
                map.put("phone", d.getPhone());
                map.put("latitude", dLat);
                map.put("longitude", dLng);
                map.put("distanceKm", Math.round(distance * 10.0) / 10.0);
                
                boolean isEligible = d.getLastDonationDate() == null || d.getLastDonationDate().isBefore(limitDate) || d.getLastDonationDate().isEqual(limitDate);
                map.put("eligibilityStatus", isEligible ? "Green" : "Red");
                map.put("badgeLevel", badgeService.calculateBadge(d.getLastDonationDate()));
                
                nearbyList.add(map);
            }
        }
        
        return ResponseEntity.ok(nearbyList);
    }

    @GetMapping("/analytics")
    public ResponseEntity<Map<String, String>> getAnalyticsSummary(@RequestParam(required = false) String hospitalName) {
        List<com.hospital.bloodapp.model.BloodInventory> inventory = inventoryService.getInventory(hospitalName);
        StringBuilder sb = new StringBuilder();
        for (com.hospital.bloodapp.model.BloodInventory item : inventory) {
            sb.append(item.getBloodGroup()).append(": ").append(item.getUnitsAvailable()).append(" units\n");
        }
        String jsonResponse = llmService.generateAnalyticsSummary(sb.toString());
        String summary = "O- and AB- are critically low this week; consider organizing a donation camp targeting these groups.";
        try {
            String cleanJson = jsonResponse.replaceAll("```json|```", "").trim();
            com.fasterxml.jackson.databind.JsonNode root = new com.fasterxml.jackson.databind.ObjectMapper().readTree(cleanJson);
            summary = root.path("summary").asText(summary);
        } catch (Exception e) {
            summary = jsonResponse;
        }
        return ResponseEntity.ok(Map.of("summary", summary));
    }

    @PostMapping("/reminders/trigger")
    public ResponseEntity<List<String>> triggerReminders() {
        List<String> reminderLogs = reminderService.sendEligibilityReminders();
        return ResponseEntity.ok(reminderLogs);
    }

    @GetMapping("/hospitals")
    public ResponseEntity<List<com.hospital.bloodapp.model.Hospital>> getHospitals() {
        return ResponseEntity.ok(hospitalRepository.findAll());
    }

    @PostMapping("/donor/{id}/sms")
    public ResponseEntity<Map<String, Object>> sendSmsToDonor(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String hospitalName = body.getOrDefault("hospitalName", "City Hospital");
        Donor donor = donorRepository.findById(id).orElse(null);
        if (donor != null) {
            boolean success = notificationService.sendDirectSms(donor, hospitalName);
            if (success) {
                return ResponseEntity.ok(Map.of("success", true, "message", "SMS sent successfully."));
            } else {
                return ResponseEntity.status(500).body(Map.of("success", false, "message", "SMS failed - check phone number format or Twilio configuration"));
            }
        }
        return ResponseEntity.notFound().build();
    }
}
