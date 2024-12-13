package com.crm.rdvision.service;


import com.crm.rdvision.entity.FeaturesToggle;
import com.crm.rdvision.entity.TicketEntity;
import com.crm.rdvision.entity.User;
import com.crm.rdvision.repository.FeaturesToggleRepo;
import com.crm.rdvision.repository.TicketRepo;
import com.crm.rdvision.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
public class AutoAssignService {
    @Autowired
     private UserRepo userRepo;
    @Autowired
    private FeaturesToggleRepo featuresToggleRepo;
    @Autowired
    private TicketRepo ticketRepo;
    private int lastAssignedUser =-1;

    public int assignTicket(TicketEntity ticket) {
        if(featuresToggleRepo.findById(1L).get().getIsEnabled()){
            List<User> users = userRepo.findByRoleIdAndActive(4);
            for(int i=0;i<users.size();i++){
                System.out.println(users.get(i).getEmail());
            }
            int numberOfUsers=users.size();
            lastAssignedUser = (lastAssignedUser+1) % numberOfUsers;
            int userId =users.get(lastAssignedUser).getUserId();
            ticket.setAssigntouser(userId);
            ticket.setAssignDate(LocalDate.now());
            ticketRepo.save(ticket);
            return ticket.getAssigntouser();
         }else{
            return 0;
        }

    }

    public void toggleFeature(long fId){
        Optional<FeaturesToggle> featuresToggle =featuresToggleRepo.findById(fId);
        System.out.println("Initial status :-"+featuresToggle.get().getIsEnabled());
        featuresToggle.get().setIsEnabled(!featuresToggle.get().getIsEnabled());
        System.out.println("Final status :-"+featuresToggle.get().getIsEnabled());
        featuresToggleRepo.save(featuresToggle.get());
    }
    public Boolean getAutoAssignFeatureStatus(){
        return featuresToggleRepo.findById(1L).get().getIsEnabled();
    }

}
