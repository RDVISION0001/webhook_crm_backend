package com.crm.rdvision.controller;

import com.crm.rdvision.dto.EmployeeAttendanceDto;
//import com.crm.rdvision.dto.EmployeeReportDto;
import com.crm.rdvision.entity.EmployeeAttendance;
import com.crm.rdvision.entity.User;
import com.crm.rdvision.repository.EmployeeAttendanceRepo;
import com.crm.rdvision.repository.UserRepo;
import com.crm.rdvision.service.EmployeeAttendanceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/attendance")
public class EmployeeAttendanceController {
    @Autowired
    private EmployeeAttendanceService employeeAttendanceService;
    @Autowired
    private EmployeeAttendanceRepo employeeAttendanceRepo;
    @Autowired
    private UserRepo userRepo;
    private static final Logger logger = LoggerFactory.getLogger(EmployeeAttendanceController.class);
    @PostMapping("logout")
    public ResponseEntity<?> logout(@RequestBody EmployeeAttendanceDto employeeAttendanceDto){
        logger.info("Logout Api Called it will record logout time and working hour");

        return employeeAttendanceService.employeeLogout(employeeAttendanceDto);
    }

     @GetMapping("/workinhourbyattendanceid/{attendanceId}")
     public int getWorkingMinutesByAttendanceId(@PathVariable long attendanceId){
         return employeeAttendanceRepo.findByAttendanceId(attendanceId).getActualWorkingSeconds();
     }

    @GetMapping("/BreakSecondbyattendanceid/{attendanceId}")
    public int getBreaktime(@PathVariable long attendanceId){
        return employeeAttendanceRepo.findByAttendanceId(attendanceId).getTotalBreakInSec();
    }
    @GetMapping("/toggleBreak/{attendanceId}")
    public  Boolean toggleBreak(@PathVariable long attendanceId){
        EmployeeAttendance employeeAttendance=employeeAttendanceRepo.findByAttendanceId(attendanceId);
        System.out.println("Previous state"+employeeAttendance.isOnBreak());
        employeeAttendanceRepo.save(employeeAttendance);
        return employeeAttendance.isOnBreak();
}

//    @GetMapping("/todayemployeereport")
//    public Map<String,String> todayEmployeeReport(){
//        List<EmployeeAttendance> employeeAttendances =employeeAttendanceRepo.findByLoginDate(LocalDate.now());
//
//    }
    @GetMapping("/totalWorkTimeBreakOfCloser")
    public List<Map<String, String>> totalWorkTimeOfTeam(){
        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = fromDate.minusDays(30);
        return employeeAttendanceRepo.findWorkAndBreakTimeByLoginDateMonthBetweenWithUserAndRole(toDate,fromDate);
    }

    @PostMapping("/addAttendance")
    public void addAttendance(@RequestBody User user){
        employeeAttendanceService.employeeLoginByUser(user);
    }

}
