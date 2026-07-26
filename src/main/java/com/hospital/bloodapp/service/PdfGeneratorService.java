package com.hospital.bloodapp.service;

import com.hospital.bloodapp.model.BloodRequest;
import com.hospital.bloodapp.model.Donor;
import com.hospital.bloodapp.repository.DonorRepository;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class PdfGeneratorService {

    private final DonorRepository donorRepository;
    private final GeocodingService geocodingService;

    public PdfGeneratorService(DonorRepository donorRepository, GeocodingService geocodingService) {
        this.donorRepository = donorRepository;
        this.geocodingService = geocodingService;
    }

    public byte[] generateConfirmationPdf(BloodRequest request, List<Donor> matchedDonorsFallback) {
        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Fonts
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Font.BOLD, java.awt.Color.RED);
            Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Font.BOLD, java.awt.Color.DARK_GRAY);
            Font boldFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Font.BOLD, java.awt.Color.BLACK);
            Font regularFont = FontFactory.getFont(FontFactory.HELVETICA, 10, Font.NORMAL, java.awt.Color.BLACK);
            Font italicFont = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 9, Font.NORMAL, java.awt.Color.GRAY);

            // Title
            Paragraph title = new Paragraph("BLOOD REQUEST CONFIRMATION", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            // Separator Line
            Paragraph line = new Paragraph("--------------------------------------------------------------------------------", regularFont);
            line.setAlignment(Element.ALIGN_CENTER);
            document.add(line);

            // Request Details Table
            PdfPTable detailsTable = new PdfPTable(2);
            detailsTable.setWidthPercentage(100);
            detailsTable.setSpacingBefore(10);
            detailsTable.setSpacingAfter(15);
            detailsTable.setWidths(new float[]{30f, 70f});

            addCell(detailsTable, "Request ID", boldFont);
            addCell(detailsTable, "REQ-" + (request.getId() != null ? request.getId() : "PENDING"), regularFont);

            addCell(detailsTable, "Patient Name", boldFont);
            addCell(detailsTable, request.getPatientName() != null ? request.getPatientName() : "N/A", regularFont);

            addCell(detailsTable, "Blood Group", boldFont);
            addCell(detailsTable, request.getBloodGroup() != null ? request.getBloodGroup() : "N/A", regularFont);

            addCell(detailsTable, "Units Needed", boldFont);
            addCell(detailsTable, String.valueOf(request.getUnits() != null ? request.getUnits() : 1), regularFont);

            addCell(detailsTable, "Hospital / Location", boldFont);
            addCell(detailsTable, request.getLocation() != null ? request.getLocation() : "N/A", regularFont);

            addCell(detailsTable, "Urgency", boldFont);
            addCell(detailsTable, request.getUrgency() != null ? request.getUrgency() : "Medium", regularFont);

            String formattedTime = request.getCreatedAt() != null 
                    ? request.getCreatedAt().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm"))
                    : java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm"));
            addCell(detailsTable, "Request Time", boldFont);
            addCell(detailsTable, formattedTime, regularFont);

            document.add(detailsTable);
            document.add(line);

            // Find all matching blood group and city donors
            String bloodGroup = request.getBloodGroup();
            String city = request.getCity();
            if (city == null || city.trim().isEmpty()) {
                if (request.getLocation() != null && !request.getLocation().trim().isEmpty()) {
                    city = extractCity(request.getLocation());
                }
            }

            List<Donor> allMatched;
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

            LocalDate limitDate = LocalDate.now().minusDays(90);
            LocalDate fiftyDaysAgo = LocalDate.now().minusDays(50);

            // Geocode patient city for distance computation (Override Z)
            String patientCity = request.getCity();
            if (patientCity == null || patientCity.trim().isEmpty()) {
                patientCity = request.getLocation() != null ? request.getLocation() : "Chennai";
            }
            double[] patientCoords = geocodingService.geocodeCity(patientCity);

            // List 1: Matched Eligible Donors
            List<Donor> eligibleDonors = allMatched.stream()
                .filter(d -> d.getLastDonationDate() == null || d.getLastDonationDate().isBefore(limitDate) || d.getLastDonationDate().isEqual(limitDate))
                .collect(Collectors.toList());

            // List 2: Recently Donated (within 50 days)
            List<Donor> recentlyDonated = allMatched.stream()
                .filter(d -> d.getLastDonationDate() != null && !d.getLastDonationDate().isBefore(fiftyDaysAgo))
                .collect(Collectors.toList());

            // 1. Matched Eligible Donors Section
            Paragraph eligibleHeader = new Paragraph("SECTION 1: MATCHED ELIGIBLE DONORS", headerFont);
            eligibleHeader.setSpacingBefore(10);
            eligibleHeader.setSpacingAfter(5);
            document.add(eligibleHeader);

            Paragraph eligibleSub = new Paragraph("The following compatible donors are eligible to donate right now (last donation was 90+ days ago or never):", italicFont);
            eligibleSub.setSpacingAfter(10);
            document.add(eligibleSub);

            if (!eligibleDonors.isEmpty()) {
                PdfPTable eligibleTable = new PdfPTable(6);
                eligibleTable.setWidthPercentage(100);
                eligibleTable.setSpacingAfter(15);
                eligibleTable.setWidths(new float[]{8f, 26f, 18f, 18f, 18f, 12f});

                addTableHeaderCell(eligibleTable, "S.No", boldFont);
                addTableHeaderCell(eligibleTable, "Donor Name", boldFont);
                addTableHeaderCell(eligibleTable, "City", boldFont);
                addTableHeaderCell(eligibleTable, "Phone", boldFont);
                addTableHeaderCell(eligibleTable, "Status", boldFont);
                addTableHeaderCell(eligibleTable, "Dist (km)", boldFont);

                int index = 1;
                for (Donor donor : eligibleDonors) {
                    double dLat = donor.getLatitude() != null ? donor.getLatitude() : 0;
                    double dLng = donor.getLongitude() != null ? donor.getLongitude() : 0;
                    if (dLat == 0 && dLng == 0 && donor.getCity() != null) {
                        double[] coords = geocodingService.geocodeCity(donor.getCity());
                        dLat = coords[0]; dLng = coords[1];
                    }
                    double distKm = geocodingService.calculateDistance(patientCoords[0], patientCoords[1], dLat, dLng);
                    String distStr = String.format("%.1f", distKm);

                    addCell(eligibleTable, String.valueOf(index++), regularFont);
                    addCell(eligibleTable, donor.getName() != null ? donor.getName() : "", regularFont);
                    addCell(eligibleTable, donor.getCity() != null ? donor.getCity() : "", regularFont);
                    addCell(eligibleTable, donor.getPhone() != null ? donor.getPhone() : "", regularFont);
                    addCell(eligibleTable, "Eligible", regularFont);
                    addCell(eligibleTable, distStr, regularFont);
                }
                document.add(eligibleTable);
            } else {
                Paragraph noEligible = new Paragraph("No eligible donors are currently available in this group/location.", italicFont);
                noEligible.setSpacingAfter(15);
                document.add(noEligible);
            }

            document.add(line);

            // 2. Recently Donated Section
            Paragraph recentHeader = new Paragraph("SECTION 2: RECENTLY DONATED (WITHIN 50 DAYS)", headerFont);
            recentHeader.setSpacingBefore(10);
            recentHeader.setSpacingAfter(5);
            document.add(recentHeader);

            Paragraph recentSub = new Paragraph("The following compatible donors have donated within the last 50 days. They are not eligible to donate yet but will be available soon:", italicFont);
            recentSub.setSpacingAfter(10);
            document.add(recentSub);

            if (!recentlyDonated.isEmpty()) {
                PdfPTable recentTable = new PdfPTable(6);
                recentTable.setWidthPercentage(100);
                recentTable.setSpacingAfter(15);
                recentTable.setWidths(new float[]{8f, 24f, 16f, 18f, 20f, 14f});

                addTableHeaderCell(recentTable, "S.No", boldFont);
                addTableHeaderCell(recentTable, "Donor Name", boldFont);
                addTableHeaderCell(recentTable, "City", boldFont);
                addTableHeaderCell(recentTable, "Phone", boldFont);
                addTableHeaderCell(recentTable, "Last Donation", boldFont);
                addTableHeaderCell(recentTable, "Dist (km)", boldFont);

                int index = 1;
                for (Donor donor : recentlyDonated) {
                    double dLat = donor.getLatitude() != null ? donor.getLatitude() : 0;
                    double dLng = donor.getLongitude() != null ? donor.getLongitude() : 0;
                    if (dLat == 0 && dLng == 0 && donor.getCity() != null) {
                        double[] coords = geocodingService.geocodeCity(donor.getCity());
                        dLat = coords[0]; dLng = coords[1];
                    }
                    double distKm = geocodingService.calculateDistance(patientCoords[0], patientCoords[1], dLat, dLng);
                    String distStr = String.format("%.1f", distKm);

                    addCell(recentTable, String.valueOf(index++), regularFont);
                    addCell(recentTable, donor.getName() != null ? donor.getName() : "", regularFont);
                    addCell(recentTable, donor.getCity() != null ? donor.getCity() : "", regularFont);
                    addCell(recentTable, donor.getPhone() != null ? donor.getPhone() : "", regularFont);
                    String dDate = donor.getLastDonationDate() != null
                            ? donor.getLastDonationDate().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy"))
                            : "N/A";
                    addCell(recentTable, dDate, regularFont);
                    addCell(recentTable, distStr, regularFont);
                }
                document.add(recentTable);
            } else {
                Paragraph noRecent = new Paragraph("No donors have donated recently in this group/location.", italicFont);
                noRecent.setSpacingAfter(15);
                document.add(noRecent);
            }

            document.add(line);

            // Footer
            Paragraph footer = new Paragraph("Confidential - Hospital Staff Use Only", italicFont);
            footer.setAlignment(Element.ALIGN_CENTER);
            footer.setSpacingBefore(20);
            document.add(footer);

            document.close();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return out.toByteArray();
    }

    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(4);
        table.addCell(cell);
    }

    private void addTableHeaderCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBackgroundColor(java.awt.Color.LIGHT_GRAY);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
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
