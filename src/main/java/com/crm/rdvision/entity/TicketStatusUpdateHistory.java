package com.crm.rdvision.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.sql.Time;
import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Table(name = "Ticket_status_history")
public class TicketStatusUpdateHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long updateId;
    private int updatedBy;
    private String status;
    private LocalDate updateDate;
    private LocalTime updateTime;
    private String ticketIdWhichUpdating;
    private String comment;
    private String userName;
    private String recordingFile;

}
