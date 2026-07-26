package com.hospital.bloodapp.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.hospital.bloodapp.model.ChatLog;

@Repository
public interface ChatLogRepository extends JpaRepository<ChatLog, Long> {
}
