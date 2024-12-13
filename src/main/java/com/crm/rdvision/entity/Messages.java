package com.crm.rdvision.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class Messages {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(columnDefinition = "MEDIUMTEXT")
    private String message;        // The message content
    private String role;
    private int sentByUserId;
    private int sentToUserId;
    private String recipientName;
    private String sentByUserName;
    private LocalDate sentDate;
    private LocalTime sentTime;

}
