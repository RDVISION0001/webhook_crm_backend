package com.crm.rdvision.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "employee_attendance")
public class EmployeeAttendance {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private long attendanceId;
    private int employeeId;
    @ManyToOne
    private User user;
    private LocalDate loginDate;
    private LocalTime loginTime;
    private LocalTime logoutTime;
    private String logoutReason;
    private double workedInHour;
    private int actualWorkingSeconds;
    private int totalBreakInSec;
    private boolean onBreak;

}
