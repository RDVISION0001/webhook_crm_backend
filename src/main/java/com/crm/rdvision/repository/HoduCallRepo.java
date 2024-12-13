package com.crm.rdvision.repository;

import com.crm.rdvision.entity.HoduCall;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HoduCallRepo extends JpaRepository<HoduCall,Integer> {

    HoduCall findByUserId(int userId);
}
