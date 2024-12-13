package com.crm.rdvision.service;

import com.crm.rdvision.entity.TicketEntity;
import com.crm.rdvision.entity.TicketTrackHistory;
import com.crm.rdvision.entity.User;
import com.crm.rdvision.repository.TicketTrackHistoryRepo;
import com.crm.rdvision.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TicketTrackHistoryService {
    @Autowired
    private TicketTrackHistoryRepo ticketTrackHistoryRepo;

    @Autowired
    UserRepo userRepo;


    public void addTicketTrackHistory(String id,String customerName,String status,String queryDate,int userId,String action){
        TicketTrackHistory ticketTrackHistory=new TicketTrackHistory();
        User user=userRepo.findByUserId(userId).get();
        user.setImageData(null);
        ticketTrackHistory.setUser(user);
        ticketTrackHistory.setTicketId(id);
        ticketTrackHistory.setTicketStatus(status);
        ticketTrackHistory.setAction(action);
        ticketTrackHistory.setCustomerName(customerName);
        ticketTrackHistory.setQueryDate(queryDate);
        ticketTrackHistory.setActionDateTime(LocalDateTime.now());
        ticketTrackHistoryRepo.save(ticketTrackHistory);
    }

    public List<TicketTrackHistory> getAllByUser(User user){
        return ticketTrackHistoryRepo.findAllByUser(user);
    }

}
