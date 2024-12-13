package com.crm.rdvision.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class TicketTrackHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long trackId;
    private String ticketId;
    private String customerName;
    private String queryDate;
    private String ticketStatus;
    private LocalDateTime actionDateTime;
    private String action;
    @ManyToOne
    private User user;

}
