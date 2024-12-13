package com.crm.rdvision.controller;

import com.crm.rdvision.dto.EmailDto;
import com.crm.rdvision.dto.SuggestedProductEmailDto;
import com.crm.rdvision.entity.*;
import com.crm.rdvision.repository.*;
import com.crm.rdvision.service.EmailService;
import com.crm.rdvision.service.TicketTrackHistoryService;
import jakarta.mail.MessagingException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/email")
public class EmailController {
    @Autowired
    private EmailService emailService;

    @Autowired
    TicketUpdateHistoryRepo ticketUpdateHistoryRepo;

    @Autowired
    TicketRepo ticketRepo;
    @Autowired
    ProductRepo productRepo;
    @Autowired
    UploadTicketRepo uploadTicketRepo;
    @Autowired
    UserRepo userRepo;

    @Autowired
    TicketTrackHistoryService ticketTrackHistoryService;


    @PostMapping("/sendsugetionmail")
    public void sendSuggetionMail(@RequestBody SuggestedProductEmailDto suggestedProductEmailDto) throws MessagingException {
        TicketEntity ticketEntity = ticketRepo.findByUniqueQueryId(suggestedProductEmailDto.getTicket().getUniqueQueryId());
      if(ticketEntity!=null){
          List<Integer> ids =suggestedProductEmailDto.getProductsIds();
          List<Product> productList =new ArrayList<>();
          for(int i=0;i<ids.size();i++){
              Product product=productRepo.findByProductId(ids.get(i));
              productList.add(product);
          }
          TicketStatusUpdateHistory ticketStatusUpdateHistory=new TicketStatusUpdateHistory();
          ticketStatusUpdateHistory.setUpdateTime(LocalTime.now());
          ticketStatusUpdateHistory.setUpdateDate(LocalDate.now());
          ticketStatusUpdateHistory.setStatus("Email send to "+ticketEntity.getSenderEmail());
          ticketStatusUpdateHistory.setTicketIdWhichUpdating(ticketEntity.getUniqueQueryId());
          ticketStatusUpdateHistory.setUpdatedBy(suggestedProductEmailDto.getUserId());
          Optional<User> user=userRepo.findByUserId(suggestedProductEmailDto.getUserId());
          ticketStatusUpdateHistory.setComment("Email To "+ticketEntity.getSenderName()+","+ "by "+user.get().getFirstName());
          ticketStatusUpdateHistory.setUserName(user.get().getFirstName()+" "+user.get().getLastName());
          ticketUpdateHistoryRepo.save(ticketStatusUpdateHistory);
          emailService.sendEnquiryEmailTest(ticketEntity.getSenderEmail(),ticketEntity.getSenderName(),ticketEntity.getSenderAddress(),ticketEntity.getSenderMobile(),ticketEntity.getSenderEmail(),ticketEntity.getQueryProductName(),productList,suggestedProductEmailDto.getText(), suggestedProductEmailDto.getTemp());
          ticketTrackHistoryService.addTicketTrackHistory(ticketEntity.getUniqueQueryId(),ticketEntity.getSenderName(),ticketEntity.getTicketstatus(),ticketEntity.getQueryTime(),user.get().getUserId(),"Suggestion Email");
      }else{
          sendUploadeEmail(suggestedProductEmailDto);
      }
    }


    @PostMapping("/ulpoadsendsugetionmail")
    public void sendUploadeEmail(SuggestedProductEmailDto suggestedProductEmailDto) throws MessagingException {

        UploadTicket ticketEntity = uploadTicketRepo.findByUniqueQueryId(suggestedProductEmailDto.getUploadTicket().getUniqueQueryId());
        List<Integer> ids =suggestedProductEmailDto.getProductsIds();
        List<Product> productList =new ArrayList<>();
        for(int i=0;i<ids.size();i++){
            Product product=productRepo.findByProductId(ids.get(i));
            productList.add(product);
        }
        emailService.sendEnquiryEmailTest(ticketEntity.getEmail(),ticketEntity.getFirstName(),ticketEntity.getSenderAddress(),ticketEntity.getMobileNumber(),ticketEntity.getEmail(),ticketEntity.getQueryProductName(),productList,suggestedProductEmailDto.getText(), suggestedProductEmailDto.getTemp());
        TicketStatusUpdateHistory ticketStatusUpdateHistory=new TicketStatusUpdateHistory();
        ticketStatusUpdateHistory.setUpdateTime(LocalTime.now());
        ticketStatusUpdateHistory.setUpdateDate(LocalDate.now());
        ticketStatusUpdateHistory.setStatus("Email send to "+ticketEntity.getEmail());
        ticketStatusUpdateHistory.setTicketIdWhichUpdating(ticketEntity.getUniqueQueryId());
        ticketStatusUpdateHistory.setUpdatedBy(suggestedProductEmailDto.getUserId());
        Optional<User> user=userRepo.findByUserId(suggestedProductEmailDto.getUserId());
        ticketStatusUpdateHistory.setComment("Email To "+ticketEntity.getFirstName()+","+ "by "+user.get().getFirstName());
        ticketStatusUpdateHistory.setUserName(user.get().getFirstName()+" "+user.get().getLastName());
        ticketUpdateHistoryRepo.save(ticketStatusUpdateHistory);
        ticketTrackHistoryService.addTicketTrackHistory(ticketEntity.getUniqueQueryId(),ticketEntity.getFirstName(),ticketEntity.getTicketstatus(),ticketEntity.getQueryTime(),user.get().getUserId(),"Suggestion Email");

    }

}
