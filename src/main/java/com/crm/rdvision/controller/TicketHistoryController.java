package com.crm.rdvision.controller;

import com.crm.rdvision.dto.TicketStatusHistoryDto;
import com.crm.rdvision.entity.*;
import com.crm.rdvision.repository.*;
import com.crm.rdvision.service.TicketTrackHistoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/history")
public class TicketHistoryController {
    @Autowired
    TicketUpdateHistoryRepo ticketUpdateHistoryRepo;

    @Autowired
    UserTaskRepo userTaskRepo;

    @Autowired
    UserRepo userRepo;

    @Autowired
    TicketRepo ticketRepo;

    @Autowired
    TicketTrackHistoryService ticketTrackHistoryService;

    @Autowired
    UploadTicketRepo uploadTicketRepo;


    @PostMapping("/copyhistory")
    public ResponseEntity<TicketStatusUpdateHistory> addcopyhistory(@RequestBody TicketStatusUpdateHistory history){
        history.setUpdateDate(LocalDate.now());
        history.setUpdateTime(LocalTime.now());
        TicketEntity ticketEntity =ticketRepo.findByUniqueQueryId(history.getTicketIdWhichUpdating());
        if (ticketEntity != null) {
            TicketTrackHistory ticketTrackHistory=new TicketTrackHistory();
            ticketTrackHistoryService.addTicketTrackHistory(ticketEntity.getUniqueQueryId(),ticketEntity.getSenderName(),ticketEntity.getTicketstatus(),ticketEntity.getQueryTime(),history.getUpdatedBy(),history.getComment());

        }else{
            UploadTicket uploadTicket=uploadTicketRepo.findByUniqueQueryId(history.getTicketIdWhichUpdating());
            TicketTrackHistory ticketTrackHistory=new TicketTrackHistory();
            ticketTrackHistoryService.addTicketTrackHistory(uploadTicket.getUniqueQueryId(),uploadTicket.getFirstName(),uploadTicket.getTicketstatus(),uploadTicket.getUploadDate()+uploadTicket.getQueryTime(),history.getUpdatedBy(),history.getComment());

        }
        return new ResponseEntity<>(ticketUpdateHistoryRepo.save(history), HttpStatus.CREATED);
    }

    @GetMapping("/getByTicketId/{ticketId}")
    public List<TicketStatusHistoryDto> getUpdateHistoryByTicketId(@PathVariable String ticketId){
        return ticketUpdateHistoryRepo.findByTicketIdWhichUpdating(ticketId);
    }

    @GetMapping("/getTotalTodayUpdateByUser/{userId}")
    public List<TicketStatusUpdateHistory> getUpdatedHistoryByUserToday(@PathVariable int userId){
        return ticketUpdateHistoryRepo.findByUpdatedByAndUpdateDate(userId,LocalDate.now());
    }

    @PostMapping("/getProgressOfUser")
    public float getUsersProgress(@RequestBody Map<String,Integer> object){
        UsersTask usersTask =userTaskRepo.findByAssignedToRoleIdAndAssignedDate(object.get("roleId"),LocalDate.now());
        List<TicketStatusUpdateHistory> history = ticketUpdateHistoryRepo.findByUpdatedByAndUpdateDate(object.get("userId"),LocalDate.now());
        int numberOfSales =0;
        for(int i=0;i<history.size();i++){
            if(Objects.equals(history.get(i).getStatus(), "Sale")){
                numberOfSales++;
            }
        }
        return (float) ( numberOfSales/usersTask.getSaleTask())*100;
    }

    @PostMapping("/getEmailSals")
    public Map<String, Map<String, Long>> getNnumberOfSalesEmails(@RequestBody(required = false) Map<String,LocalDate> dates){
        System.out.println(
                "User numbers called"
        );
        if(dates==null){
            LocalDate startDate=LocalDate.now().minusDays(30);
            LocalDate endDate=LocalDate.now();
            List<Object[]> results=ticketUpdateHistoryRepo.countByStatusTypesForAllUsers("email","Sale","quotation","paylink",startDate,endDate);
            Map<String, Map<String, Long>> userStatusCounts = new HashMap<>();
            for (Object[] result : results) {
                String userName = (String) result[0];
                String statusType = (String) result[1];
                Long count = (Long) result[2];

                userStatusCounts
                        .computeIfAbsent(userName!=null?userName.split(" ")[0]:"NA", k -> new HashMap<>())
                        .put(statusType, count);
            }
            return userStatusCounts;
        }else{
            LocalDate startDate =dates.get("startDate");
            LocalDate endDate =dates.get("endDate");
            List<Object[]> results=ticketUpdateHistoryRepo.countByStatusTypesForAllUsers("email","Sale","quotation","paylink",startDate,endDate);
            Map<String, Map<String, Long>> userStatusCounts = new HashMap<>();
            for (Object[] result : results) {
                String userName = (String) result[0];
                String statusType = (String) result[1];
                Long count = (Long) result[2];

                userStatusCounts
                        .computeIfAbsent(userName!=null?userName.split(" ")[0]:"NA", k -> new HashMap<>())
                        .put(statusType, count);
            }
            return userStatusCounts;
        }

    }


    @GetMapping("/getTickettrackhistory/{userId}")
    public List<TicketTrackHistory> getAllTicketTrackHistoryOfUser(@PathVariable  int userId){
        User user =userRepo.findByUserId(userId).get();
        return ticketTrackHistoryService.getAllByUser(user);
    }
}
