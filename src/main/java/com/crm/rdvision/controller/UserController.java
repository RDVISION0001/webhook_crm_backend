package com.crm.rdvision.controller;

import com.crm.rdvision.Exception.BussinessException;
import com.crm.rdvision.Exception.ContractException;
import com.crm.rdvision.common.EndPointReference;
import com.crm.rdvision.dto.*;
import com.crm.rdvision.entity.*;
import com.crm.rdvision.repository.*;
import com.crm.rdvision.service.AutoAssignService;
import com.crm.rdvision.service.EmployeeAttendanceService;
import com.crm.rdvision.service.UserService;
import com.crm.rdvision.utility.Constants;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.crm.rdvision.utility.EnglishConstants;
import jakarta.transaction.Transactional;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@RestController
@CrossOrigin
@RequestMapping("/user/")
@Tag(name = "User Controller",description = "perform user operation")
public class UserController {
    @Autowired
    UserService userService;
    @Autowired
    UserRepo userRepo;
    @Autowired
    DepartmentRepo departmentRepo;
    @Autowired
    RoleRepo roleRepo;
    @Autowired
    TeamRepo teamRepo;
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    AutoAssignService autoAssignService;
    @Autowired
    EmployeeAttendanceService employeeAttendanceService;
    @Autowired
    EmployeeAttendanceRepo employeeAttendanceRepo;
    @Autowired
    TicketUpdateHistoryRepo ticketUpdateHistoryRepo;
    @Autowired
    TicketRepo ticketRepo;
    @Autowired
    UploadTicketRepo uploadTicketRepo;
    @Autowired
    HoduCallRepo hoduCallRepo;
    @Autowired
    EnoteBookRepo enoteBookRepo;

    @Autowired
    ChatRepo chatRepo;

    @Autowired
    CallRecordRepo callRecordRepo;
private static final Logger logger= LoggerFactory.getLogger(UserController.class);
    @Operation(summary = "post operation in user",description = "save user in db")

    @PostMapping(EndPointReference.CREATE_USER)
    public Map<String, Object> createUser(@RequestBody UserDto userDto) throws com.avanse.core.exception.TechnicalException, BussinessException, ContractException {
        logger.info("Create User Called");
        return userService.createUser(userDto);
    }
    @PostMapping(EndPointReference.DELETE_USER)
    public Map<String, Object> deleteUser(@PathVariable Integer userId) throws com.avanse.core.exception.TechnicalException, BussinessException {
        logger.info("Delete user Called");
        Map<String, Object> map = new HashMap<>();
        Optional<User> user=userRepo.findById(userId);
        if(user.isPresent()){
            if(user.get().getUserStatus()=='F'){
                user.get().setUserStatus('A');
            }else{
                user.get().setUserStatus('F');
            }
            userRepo.save(user.get());
            map.put(Constants.ID, user.get().getUserId());
            map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
            map.put(Constants.ERROR, null);
        }else{
            map.put(Constants.ID,null);
            map.put(Constants.SUCCESS,"Failed !");
            map.put(Constants.ERROR,"User Not Found");
        }

        return map;
    }
    @PostMapping(EndPointReference.UPDATE_USER)
    public Map<String, Object> updateUser(@RequestBody UserDto userDto) throws com.avanse.core.exception.TechnicalException, BussinessException {
        logger.info("UPDATE USER CALLED");
        Map<String, Object> map = new HashMap<>();
        Optional<User> existingUser=userRepo.findByUserId(userDto.getUserId());
        userRepo.save(modelMapper.map(userDto,User.class));
        map.put(Constants.ID, userDto.getUserId());
        map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
        map.put(Constants.ERROR, null);
        return map;
    }
    @GetMapping(EndPointReference.GET_USER)
    public Map<String, Object> getUserByUserId(@PathVariable Integer userId) throws com.avanse.core.exception.TechnicalException, BussinessException {
        logger.info("Get User By User ID Called");
        Map<String, Object> map = new HashMap<>();
        User userDetail = this.userRepo.findById(userId).orElseThrow(() -> new BussinessException(HttpStatus.NOT_FOUND, EnglishConstants.USER_NOT_FOUND_WITH_USER_ID + userId));
        UserDto user=modelMapper.map(userDetail,UserDto.class);
        Optional<Department> department=departmentRepo.findById(user.getDepartmentId());
        DepartmentDto departmentDto=modelMapper.map(department,DepartmentDto.class);
        Optional<Role> role=roleRepo.findById(user.getRoleId());
        RoleDto roleDto=modelMapper.map(role,RoleDto.class);
        Optional<Team> team=teamRepo.findById(user.getTeamId());
        TeamDto teamDto=modelMapper.map(team,TeamDto.class);
        user.setRoleDto(roleDto);
        user.setDepartmentDto(departmentDto);
        user.setTeamDto(teamDto);
        map.put(Constants.DTO, user);
        map.put(Constants.ERROR, null);
        map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
        return map;
    }
    @GetMapping(EndPointReference.GET_ALL_USERS)
    public Map<String, Object> getUserByStatus(@RequestParam(required = false) Integer roleId) throws com.avanse.core.exception.TechnicalException, BussinessException {
      logger.info("Get User By Status called"+roleId);
        Map<String, Object> map = new HashMap<>();
        List<User> userdb=userRepo.findAll();
        UserDto userDto =modelMapper.map(userdb,UserDto.class);
//        if (null!=roleId) {
//            userdb = userRepo.findByRoleId(roleId);
//            userDto = new ArrayList<>();
//            for (User userItr : userdb) {
//                UserDto user = modelMapper.map(userItr, UserDto.class);
//                Optional<Department> department = departmentRepo.findById(user.getDepartmentId());
//                DepartmentDto departmentDto = modelMapper.map(department, DepartmentDto.class);
//                Optional<Role> role = roleRepo.findById(user.getRoleId());
//                RoleDto roleDto = modelMapper.map(role, RoleDto.class);
//                Optional<Team> team = teamRepo.findById(user.getTeamId());
//                TeamDto teamDto = modelMapper.map(team, TeamDto.class);
//                user.setRoleDto(roleDto);
//                user.setDepartmentDto(departmentDto);
//                user.setTeamDto(teamDto);
//                userDto.add(user);
//
//            }
//        }else {
//        userdb= userRepo.findAll();
//        for (User userItr:userdb) {
//            UserDto user = modelMapper.map(userItr, UserDto.class);
//            Optional<Department> department = departmentRepo.findById(user.getDepartmentId());
//            DepartmentDto departmentDto = modelMapper.map(department, DepartmentDto.class);
//            Optional<Role> role = roleRepo.findById(user.getRoleId());
//            RoleDto roleDto = modelMapper.map(role, RoleDto.class);
//            Optional<Team> team = teamRepo.findById(user.getTeamId());
//            TeamDto teamDto = modelMapper.map(team, TeamDto.class);
//            user.setRoleDto(roleDto);
//            user.setDepartmentDto(departmentDto);
//            user.setTeamDto(teamDto);
//            userDto.add(user);
//        }
//        }
        map.put(Constants.DTO_LIST, userdb);
        map.put(Constants.ERROR, null);
        map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
        return map;
    }

    @GetMapping("dropdown")
    public Map<String, Object> getUsersByRoleId(@RequestParam int roleId){
        logger.info("Dropdown user called");
        Map<String, Object> map = new HashMap<>();
        map.put(Constants.DTO_LIST, userRepo.findByRoleId(roleId));
        map.put(Constants.ERROR, null);
        map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
        return map;
    }

    //Auto assign toggle controller
    @PutMapping("autoAssign")
    public String toggleAutoAssign(){
         autoAssignService.toggleFeature(1L);
         return "Operation Successful";
    }

    //auto assign feature status
    @GetMapping("getAutoStatus")
    public Boolean getStatusOfAutoAssignFeature(){
        return autoAssignService.getAutoAssignFeatureStatus();
    }

    @PostMapping("updateImage")
    public String updateImageByUserId(@RequestBody  User user){
        logger.info("Image update is calling");
       Optional<User> user1 =userRepo.findByEmail(user.getEmail());
        user1.get().setImageData(user.getImageData());
        userRepo.save(user1.get());
        logger.info("Image updated");
        return "Image Updated";
    }

    @PostMapping("/userreport")
    public List<Map<String, String>> userReport(@RequestBody Map<String, String> object) {
        int noOfMonthOrWeek =Integer.parseInt(object.get("weeks"));
        LocalDate fromDate = LocalDate.parse(object.get("startDate"));
        LocalDate toDate = LocalDate.parse(object.get("endDate"));;  // Subtract 7 days from the fromDate
        int userID = Integer.parseInt(object.get("userId"));
        Optional<User> user =userRepo.findByUserId(userID);
        return  employeeAttendanceRepo.findWorkAndBreakTimeByLoginDateBetweenAndUser(toDate,fromDate,user.get()) ;

    }
    @PostMapping("/userreportbymonth")
    public List<Map<String, String>> userReportByMonth(@RequestBody Map<String, String> object) {
        int noOfMonthOrWeek =Integer.parseInt(object.get("weeks"));
        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = fromDate.minusDays(noOfMonthOrWeek*30);  // Subtract 7 days from the fromDate
        int userID = Integer.parseInt(object.get("userId"));
        Optional<User> user =userRepo.findByUserId(userID);
        return  employeeAttendanceRepo.findWorkAndBreakTimeByLoginDateMonthBetweenAndUser(toDate,fromDate,user.get()) ;
    }

    @GetMapping("/getNoOfTickets/{userId}")
    public Map<String,Object> getNumberOfTickets(@PathVariable int userId){
        Map<String,Long> uploadCount =uploadTicketRepo.countTicketStatisticsByUserId(userId);
        Map<String,Long> liveCount =ticketRepo.countTicketStatisticsByUserId(userId);
        Map<String,Object> totalLiveAndUploaded =new HashMap<>();
        totalLiveAndUploaded.put("Live",liveCount);
        totalLiveAndUploaded.put("uploded",uploadCount);
        return totalLiveAndUploaded;
    }

    @PostMapping("/addagent")
    public ResponseEntity<?> createHoduAgent(@RequestBody HoduCall hoduCall) {
        try {
            hoduCall.setHoduTanentId("1008");
            hoduCall.setHoduToken("dVdVNnCVTQJypfE1");
            hoduCallRepo.save(hoduCall);
            return ResponseEntity.ok("Agent added");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while adding the agent: " + e.getMessage());
        }
    }

    @PutMapping("/replaceToken")
    public ResponseEntity<?> replaceToken(@RequestBody HoduCall hoduCall) {
        try {
            List<HoduCall> hoduCalls = hoduCallRepo.findAll();
            if (hoduCalls.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("No agents found to update.");
            }

            for (HoduCall call : hoduCalls) {
                call.setHoduToken(hoduCall.getHoduToken());
            }
            hoduCallRepo.saveAll(hoduCalls);
            return ResponseEntity.ok("Token updated for all agents.");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while updating tokens: " + e.getMessage());
        }
    }

    @GetMapping("/getAgent/{userId}")
    public ResponseEntity<?> getAgentByUserId(@PathVariable int userId) {
        try {
            HoduCall hoduCall = hoduCallRepo.findByUserId(userId);
            if (hoduCall == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(false);
            }
            return ResponseEntity.ok(true);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("An error occurred while retrieving the agent: " + e.getMessage());
        }
    }

    @GetMapping("/toggleBreak/{userId}")
    public Boolean toggleBreak(@PathVariable int userId){
        Optional<User> user =userRepo.findByUserId(userId);
        user.get().setOnBreak(!user.get().getOnBreak());
        userRepo.save(user.get());
        return user.get().getOnBreak();
    }
    @GetMapping("/getLiveTeammates/{userId}")
    public List<User> getLiveTeammates(@PathVariable int userId){
        Optional<User> user =userRepo.findByUserId(userId);
        return userRepo.findByTeamIdAndOnBreakIsFalseAndRoleId(user.get().getTeamId(),4);
    }

    @GetMapping("/getNoOfTodayAssinedTicket/{userId}")
    public Map<Integer,Integer> getNumberOftotalassignedTicketBySS(@PathVariable int userId){
        Optional<User> user=userRepo.findByUserId(userId);
        List<User> users =userRepo.findByTeamIdAndRoleId(user.get().getTeamId(),4);
        Map<Integer,Integer> map =new HashMap<>();
        for(int i=0;i<users.size();i++){
            map.put(users.get(i).getUserId(),uploadTicketRepo.countByAssigntouserAndAssignDate(users.get(i).getUserId(),LocalDate.now())+ticketRepo.countByAssigntouserAndAssignDate(users.get(i).getUserId(),LocalDate.now()));
        }
       return map;
    }

    @GetMapping("/getnoofCallsTotal")
    public List<Object[]> getNoOfCalls(){
        return callRecordRepo.countAllCallsGroupedByUserFirstName();
    }

    @GetMapping("/getnoofCallsToday")
    public List<Object[]> getNoOfCallsToday(){
        return callRecordRepo.countAllCallsGroupedByUserFirstName(LocalDate.now());
    }

    @GetMapping("/getnoofCalls")
    public List<Object[]> getNoOfCallsbyMonths(){
        return callRecordRepo.countAllCallsGroupedByUserFirstNameAndMonth();
    }

    @DeleteMapping("/deleteUser/{userId}")
    @Transactional
    public void deleteUserById(@PathVariable int userId){
        List<TicketEntity> ticketEntities =ticketRepo.findByUser(userId);
        List<UploadTicket> uploadTickets=uploadTicketRepo.findByUser(userId);
        for (TicketEntity ticketEntity : ticketEntities) {
            ticketEntity.setAssigntouser(null);
        }
        for (UploadTicket uploadTicket : uploadTickets) {
            uploadTicket.setAssigntouser(null);
        }
        User user =new User();
        user.setUserId(userId);
        employeeAttendanceRepo.deleteAllByUser(userId);
        callRecordRepo.deleteAllByUser(userId);
        enoteBookRepo.deleteAllByUser(userId);
        ticketRepo.saveAll(ticketEntities);
        uploadTicketRepo.saveAll(uploadTickets);
        userRepo.deleteById(userId);
    }

    @GetMapping("/getAllCloser")
    public List<User> getAllClosers(){
        return userRepo.findAll();
    }
    @GetMapping("/getAdminDetail")
    public User getAdmin(){
        return userRepo.findByRoleId(1).get(0);
    }

    // Fetch all messages by SentByUserId
    @GetMapping("/messages/{userId}")
    public List<Messages> getMessagesByUserId(@PathVariable("userId") Long userId) {
        return chatRepo.findBySentByUserIdOrSentToUserId(userId,userId);
    }


}
