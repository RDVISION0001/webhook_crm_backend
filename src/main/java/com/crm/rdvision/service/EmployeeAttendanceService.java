package com.crm.rdvision.service;

//import com.crm.rdvision.dto.EmployeeReportDto;
import com.crm.rdvision.dto.EmployeeAttendanceDto;
import com.crm.rdvision.entity.EmployeeAttendance;
import com.crm.rdvision.entity.User;
import com.crm.rdvision.repository.EmployeeAttendanceRepo;
import com.crm.rdvision.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class EmployeeAttendanceService {
    @Autowired
    private EmployeeAttendanceRepo employeeAttendanceRepo;
    @Autowired
    private UserRepo userRepo;


    //Login Method
    public ResponseEntity<EmployeeAttendance> employeeLogin(int  employeeId){
        User user =new User();
        user.setUserId(employeeId);
        EmployeeAttendance employeeAttendance1=new EmployeeAttendance();
        employeeAttendance1.setUser(user);
        employeeAttendance1.setLoginDate(LocalDate.now());
        employeeAttendance1.setLoginTime(LocalTime.now());
        employeeAttendance1.setActualWorkingSeconds(0);
        employeeAttendance1.setOnBreak(false);
        employeeAttendance1.setTotalBreakInSec(0);
        return new ResponseEntity<>(employeeAttendanceRepo.save(employeeAttendance1), HttpStatus.CREATED);
    }
    //Login Method
    public ResponseEntity<EmployeeAttendance> employeeLoginByUser(User  user){
        EmployeeAttendance employeeAttendance1=new EmployeeAttendance();
       employeeAttendance1.setUser(user);
        employeeAttendance1.setLoginDate(LocalDate.now());
        employeeAttendance1.setLoginTime(LocalTime.now());
        employeeAttendance1.setActualWorkingSeconds(0);
        employeeAttendance1.setOnBreak(false);
        employeeAttendance1.setTotalBreakInSec(0);
        return new ResponseEntity<>(employeeAttendanceRepo.save(employeeAttendance1), HttpStatus.CREATED);
    }

    //Logout method
    public ResponseEntity<?> employeeLogout(EmployeeAttendanceDto employeeAttendanceDto){
        EmployeeAttendance employeeAttendance1 =employeeAttendanceRepo.findByAttendanceId(employeeAttendanceDto.getAttendanceId());
        if(employeeAttendance1!=null){
            LocalTime currentTime = LocalTime.now();
            Duration workedBeforeLogout =Duration.between(employeeAttendance1.getLoginTime(),currentTime);
            employeeAttendance1.setWorkedInHour((double) workedBeforeLogout.toMinutes() /60);
            employeeAttendance1.setLogoutTime(currentTime);
            employeeAttendance1.setLogoutReason(employeeAttendanceDto.getLogoutReason());
            employeeAttendance1.setOnBreak(true);
            employeeAttendance1.setActualWorkingSeconds(employeeAttendanceDto.getActualWorkingSeconds());
            employeeAttendance1.setTotalBreakInSec(employeeAttendanceDto.getTotalBreakInSec());
            employeeAttendanceRepo.save(employeeAttendance1);
            Optional<User> user=userRepo.findByUserId(employeeAttendance1.getUser().getUserId());
            user.get().setOnBreak(true);
            userRepo.save(user.get());
            return ResponseEntity.ok("Successfully Logout ");
        }else{
            return ResponseEntity.badRequest().body("Some Error occurs");
        }
    }

    //Employee Report
public List<EmployeeAttendance> employeeWorkReport(LocalDate fromDate,LocalDate toDate,int userId){
        List<EmployeeAttendance> employeeAttendances =employeeAttendanceRepo.findByLoginDateBetweenAndEmployeeId(fromDate,toDate,userId);
        return employeeAttendances;
    }
}
