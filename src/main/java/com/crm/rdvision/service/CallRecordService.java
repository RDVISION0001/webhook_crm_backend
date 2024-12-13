package com.crm.rdvision.service;

import com.crm.rdvision.entity.CallRecords;
import com.crm.rdvision.entity.User;
import com.crm.rdvision.repository.CallRecordRepo;
import com.crm.rdvision.repository.UserRepo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

@Service
public class CallRecordService {


    @Autowired
    CallRecordRepo callRecordRepo;
    @Autowired
    UserRepo userRepo;


    public void addCallRecord(Map<String, String> numberDetails){
        int userId =Integer.parseInt(numberDetails.get("userId"));
        User user =userRepo.findByUserId(userId).get();
        CallRecords callRecords=new CallRecords();
        callRecords.setUser(user);
        callRecords.setCallingNumber(numberDetails.get("number"));
        callRecords.setCallDate(LocalDate.now());
        callRecords.setCallTime(LocalTime.now());
        callRecordRepo.save(callRecords);
    }
}
