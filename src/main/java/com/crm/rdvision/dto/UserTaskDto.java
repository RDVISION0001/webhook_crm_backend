package com.crm.rdvision.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class UserTaskDto {
    private int assignedBy;
    private int assignedToRoleId;
    private int saleTask;
    private LocalDate assignedDate;
    private LocalTime assignedTime;
    private String taskDesc;
}
