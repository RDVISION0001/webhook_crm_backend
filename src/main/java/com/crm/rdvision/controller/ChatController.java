package com.crm.rdvision.controller;

import com.crm.rdvision.entity.Messages;

import com.crm.rdvision.repository.ChatRepo;
import com.crm.rdvision.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@Controller
public class ChatController {

    @Autowired
    ChatRepo chatRepo;

    @Autowired
    UserRepo userRepo;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Listen to messages sent to "/app/send"
    @MessageMapping("/send")
    public void sendMessage(Messages message) {
        message.setSentByUserName(userRepo.findByUserId(message.getSentByUserId()).get().getFirstName());
        chatRepo.save(message);
        System.out.println(message.getSentToUserId());
//        if (message.getSentToUserId() == 0) {
            System.out.println("sent to all");
            messagingTemplate.convertAndSend("/topic/messages", message);
            System.out.println("sent");

//        } else {
//            String username =userRepo.findByUserId(message.getSentToUserId()).get().getFirstName().toLowerCase();
//
//            messagingTemplate.convertAndSend("/topic/messages", message);
//            messagingTemplate.convertAndSendToUser(username, "/queue/messages",message);
//        }
    }

    // Optionally, retrieve chat history with a specific user
    @GetMapping("/chats/{userId}")
    public List<Messages> getMessagesWithUser(@PathVariable long userId) {
        return chatRepo.findBySentByUserIdOrSentToUserId(userId,userId); // Add a method in ChatRepo to fetch messages by recipientId
    }
}

