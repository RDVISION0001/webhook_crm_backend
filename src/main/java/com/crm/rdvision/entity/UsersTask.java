package com.crm.rdvision.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Table(name = "Task_status")
public class UsersTask {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long taskId;
    private int assignedBy;
    private int assignedToRoleId;
    private int saleTask;
    private LocalDate assignedDate;
    private LocalTime assignedTime;
    private String taskDesc;
}
