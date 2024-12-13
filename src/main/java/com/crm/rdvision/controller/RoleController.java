package com.crm.rdvision.controller;

import com.crm.rdvision.Exception.BussinessException;
import com.crm.rdvision.common.EndPointReference;
import com.crm.rdvision.dto.DepartmentDto;
import com.crm.rdvision.dto.RoleDto;
import com.crm.rdvision.dto.SuccessResponse;
import com.crm.rdvision.entity.Department;
import com.crm.rdvision.entity.Role;
import com.crm.rdvision.repository.DepartmentRepo;
import com.crm.rdvision.repository.RoleRepo;
import com.crm.rdvision.utility.Constants;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@CrossOrigin
@RequestMapping("/role/")
public class RoleController {
    @Autowired
    RoleRepo roleRepo;
    @Autowired
    ModelMapper modelMapper;
    private static final Logger logger =LoggerFactory.getLogger(RoleController.class);
    @PostMapping(EndPointReference.CREATE_ROLE)
    public Map<String, Object> createRole(@RequestBody RoleDto roleDto) throws com.avanse.core.exception.TechnicalException, BussinessException {
        logger.info("Create Role Called");
        Map<String, Object> map = new HashMap<>();
        Role role=null;
        try {

            role=roleRepo.save(modelMapper.map(roleDto, Role.class));

        } catch (Exception e) {
            throw new com.avanse.core.exception.TechnicalException(Constants.TECHNICAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, Constants.SPACE);
        }
        map.put(Constants.ID, role.getRoleId());
        map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
        map.put(Constants.ERROR, null);

        return map;
    }
    @GetMapping(EndPointReference.GET_ROLE)
    public Map<String, Object> getRoleById(@PathVariable Integer roleId) throws com.avanse.core.exception.TechnicalException, BussinessException {
        logger.info("Get Role By Role Id Called");
        Map<String, Object> map = new HashMap<>();
        try {
            map.put(Constants.DTO, roleRepo.findById(roleId));
            map.put(Constants.ERROR, null);
            map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
            return map;
        } catch (Exception e) {
            throw new com.avanse.core.exception.TechnicalException(Constants.TECHNICAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, Constants.SPACE);
        }
    }
    @GetMapping(EndPointReference.GET_ALL_ROLES)
    public Map<String, Object> roles() throws com.avanse.core.exception.TechnicalException, BussinessException {
        logger.info("Get All Roles Called");
        Map<String, Object> map = new HashMap<>();
        try {
            map.put(Constants.DTO_LIST, roleRepo.findAll());
            map.put(Constants.ERROR, null);
            map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
            return map;
        } catch (Exception e) {
            throw new com.avanse.core.exception.TechnicalException(Constants.TECHNICAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, Constants.SPACE);
        }
    }
}
