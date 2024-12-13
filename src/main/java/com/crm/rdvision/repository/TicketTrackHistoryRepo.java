package com.crm.rdvision.repository;

import com.crm.rdvision.entity.TicketTrackHistory;
import com.crm.rdvision.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TicketTrackHistoryRepo extends JpaRepository<TicketTrackHistory,Long> {
    List<TicketTrackHistory> findAllByUser(User user);
}
