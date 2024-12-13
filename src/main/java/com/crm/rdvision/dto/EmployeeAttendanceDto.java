package com.crm.rdvision.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class EmployeeAttendanceDto {
    private long attendanceId;
    private int employeeId;
    private LocalDate loginDate;
    private LocalTime loginTime;
    private LocalTime logoutTime;
    private String logoutReason;
    private double workedInHour;
    private int actualWorkingSeconds;
    private int totalBreakInSec;
    private boolean onBreak;
}
