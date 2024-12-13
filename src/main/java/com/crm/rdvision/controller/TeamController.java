package com.crm.rdvision.controller;

import com.crm.rdvision.Exception.BussinessException;
import com.crm.rdvision.common.EndPointReference;
import com.crm.rdvision.dto.DepartmentDto;
import com.crm.rdvision.dto.SuccessResponse;
import com.crm.rdvision.dto.TeamDto;
import com.crm.rdvision.entity.Department;
import com.crm.rdvision.entity.Team;
import com.crm.rdvision.entity.User;
import com.crm.rdvision.repository.DepartmentRepo;
import com.crm.rdvision.repository.TeamRepo;
import com.crm.rdvision.repository.TicketUpdateHistoryRepo;
import com.crm.rdvision.repository.UserRepo;
import com.crm.rdvision.utility.Constants;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping("/team/")
public class TeamController {
    @Autowired
    TeamRepo teamRepo;
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    UserRepo userRepo;
    @Autowired
    TicketUpdateHistoryRepo ticketUpdateHistoryRepo;
    private static  final Logger logger = LoggerFactory.getLogger(TeamController.class);
    @PostMapping(EndPointReference.CREATE_TEAM)
    public Map<String, Object> createTeam(@RequestBody TeamDto teamDto) throws com.avanse.core.exception.TechnicalException, BussinessException {
        logger.info("Create team called");
        Map<String, Object> map = new HashMap<>();
        Team team=null;
        try {

            team=teamRepo.save(modelMapper.map(teamDto, Team.class));

        } catch (Exception e) {
            throw new com.avanse.core.exception.TechnicalException(Constants.TECHNICAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, Constants.SPACE);
        }
        map.put(Constants.ID, team.getTeamId());
        map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
        map.put(Constants.ERROR, null);

        return map;
    }
    @GetMapping(EndPointReference.GET_TEAM)
    public Map<String, Object> getTeamById(@PathVariable Integer teamId) throws com.avanse.core.exception.TechnicalException, BussinessException {
        logger.info("Get Team by Id Called");
        Map<String, Object> map = new HashMap<>();
        try {
            map.put(Constants.DTO_LIST, teamRepo.findById(teamId));
            map.put(Constants.ERROR, null);
            map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
            return map;
        } catch (Exception e) {
            throw new com.avanse.core.exception.TechnicalException(Constants.TECHNICAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, Constants.SPACE);
        }
    }
    @GetMapping(EndPointReference.GET_ALL_TEAM)
    public Map<String, Object> teams() throws com.avanse.core.exception.TechnicalException, BussinessException {
        logger.info("Get All Team Called");
        Map<String, Object> map = new HashMap<>();
        try {
            map.put(Constants.DTO_LIST, teamRepo.findAll());
            map.put(Constants.ERROR, null);
            map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
            return map;
        } catch (Exception e) {
            throw new com.avanse.core.exception.TechnicalException(Constants.TECHNICAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, Constants.SPACE);
        }
    }

    @GetMapping("/bestsellingTeammates/{userId}")
    public List<Map<String, String>> bestSellingTeamMates(@PathVariable int userId){
//        Optional<User> user =userRepo.findByUserId(userId);
//        List<User> closersOfTeam =userRepo.findByTeamIdAndRoleId(user.get().getTeamId(),4);
        LocalDate fromDate = LocalDate.now();
        LocalDate toDate = fromDate.minusDays(30);
        return ticketUpdateHistoryRepo.findUserSaleCount(toDate,fromDate);
    }
}