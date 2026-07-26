package com.hospital.bloodapp.runner;

import com.hospital.bloodapp.model.Hospital;
import com.hospital.bloodapp.model.BloodInventory;
import com.hospital.bloodapp.repository.HospitalRepository;
import com.hospital.bloodapp.repository.BloodInventoryRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final HospitalRepository hospitalRepository;
    private final BloodInventoryRepository inventoryRepository;
    private final JdbcTemplate jdbcTemplate;

    public DatabaseSeeder(HospitalRepository hospitalRepository, BloodInventoryRepository inventoryRepository, JdbcTemplate jdbcTemplate) {
        this.hospitalRepository = hospitalRepository;
        this.inventoryRepository = inventoryRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 0;");
            jdbcTemplate.execute("TRUNCATE TABLE donation_history;");
            jdbcTemplate.execute("TRUNCATE TABLE donor;");
            jdbcTemplate.execute("SET FOREIGN_KEY_CHECKS = 1;");
            jdbcTemplate.execute("ALTER TABLE donor AUTO_INCREMENT = 1;");
            jdbcTemplate.execute("ALTER TABLE donation_history AUTO_INCREMENT = 1;");
            System.out.println("[DB RESET] Cleaned donor and donation_history tables, reset AUTO_INCREMENT to 1.");
        } catch (Exception e) {
            System.err.println("[DB RESET] Failed to truncate tables: " + e.getMessage());
        }

        if (hospitalRepository.count() == 0) {
            seedHospitals();
        }
    }

    private void seedHospitals() {
        List<Hospital> hospitals = List.of(
            createHospital("City Hospital Chennai", "Chennai", 13.0827, 80.2707),
            createHospital("Madurai General Hospital", "Madurai", 9.9252, 78.1198),
            createHospital("Salem Medical Center", "Salem", 11.6643, 78.1460),
            createHospital("Coimbatore Life Care", "Coimbatore", 11.0168, 76.9558),
            createHospital("Trichy Multi Speciality", "Trichy", 10.7905, 78.7047)
        );

        for (Hospital h : hospitals) {
            hospitalRepository.save(h);
            seedInventoryForHospital(h.getName());
        }
    }

    private Hospital createHospital(String name, String city, double lat, double lng) {
        Hospital h = new Hospital();
        h.setName(name);
        h.setCity(city);
        h.setLatitude(lat);
        h.setLongitude(lng);
        return h;
    }

    private void seedInventoryForHospital(String hospitalName) {
        String[] groups = {"A+", "A-", "B+", "B-", "O+", "O-", "AB+", "AB-"};
        for (String bg : groups) {
            Optional<BloodInventory> existing = inventoryRepository.findByHospitalNameAndBloodGroup(hospitalName, bg);
            if (existing.isEmpty()) {
                BloodInventory item = new BloodInventory();
                item.setHospitalName(hospitalName);
                item.setBloodGroup(bg);
                item.setUnitsAvailable(0);
                item.setStatus("Low");
                item.setLastUpdated(java.time.LocalDateTime.now());
                inventoryRepository.save(item);
            }
        }
    }
}
