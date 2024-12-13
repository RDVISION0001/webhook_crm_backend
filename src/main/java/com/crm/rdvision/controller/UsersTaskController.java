package com.crm.rdvision.controller;

import com.crm.rdvision.dto.UserTaskDto;
import com.crm.rdvision.entity.User;
import com.crm.rdvision.entity.UsersTask;
import com.crm.rdvision.repository.UserRepo;
import com.crm.rdvision.repository.UserTaskRepo;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/users_task")
public class UsersTaskController {

    @Autowired
    private UserTaskRepo userTaskRepo;
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    private UserRepo userRepo;


    @PostMapping("/addTask")
    public ResponseEntity<?> assignTaskToRoles(@RequestBody UserTaskDto taskDto){
        taskDto.setAssignedDate(LocalDate.now());
        taskDto.setAssignedTime(LocalTime.now());
        return new ResponseEntity<>(userTaskRepo.save(modelMapper.map(taskDto,UsersTask.class)), HttpStatus.CREATED);
    }



    @GetMapping("/getYourTodayTask/{userId}")
    public UserTaskDto getYourTodayTask(@PathVariable int userId){
        Optional<User> user =userRepo.findByUserId(userId);
        LocalDate localDate = LocalDate.now();
        UsersTask usersTask = userTaskRepo.findByAssignedToRoleIdAndAssignedDate(user.get().getRoleId(),localDate);
       if(usersTask==null){
           return null;
       }else{
           return modelMapper.map(usersTask,UserTaskDto.class);
       }

    }

}
