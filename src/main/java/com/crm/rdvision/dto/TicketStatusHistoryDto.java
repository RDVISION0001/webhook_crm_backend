package com.crm.rdvision.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
public class TicketStatusHistoryDto {
    private int updatedBy;
    private String status;
    private LocalDate updateDate;
    private String ticketIdWhichUpdating;
    private String comment;
    private String userName;
    private String recordingFile;
}
