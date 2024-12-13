package com.crm.rdvision.repository;

import com.crm.rdvision.entity.EmailTracking;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailTrackingRepo extends JpaRepository<EmailTracking,Long> {
}
