package com.crm.rdvision.controller;

import com.crm.rdvision.Exception.BussinessException;
import com.crm.rdvision.common.EndPointReference;
import com.crm.rdvision.dto.DepartmentDto;
import com.crm.rdvision.dto.SuccessResponse;
import com.crm.rdvision.entity.Department;
import com.crm.rdvision.repository.DepartmentRepo;
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
@RequestMapping("/department/")
public class DepartmentController {
    @Autowired
    DepartmentRepo departmentRepo;
    @Autowired
    ModelMapper modelMapper;
    private static final Logger logger = LoggerFactory.getLogger(DepartmentController.class);
    @PostMapping(EndPointReference.CREATE_DEPARTMENT)
    public Map<String, Object> createDepartment(@RequestBody DepartmentDto departmentDto) throws com.avanse.core.exception.TechnicalException, BussinessException {
       logger.info("Create Department Called to create new Department ");
        Map<String, Object> map = new HashMap<>();
        Department department=null;
        try {

            department=departmentRepo.save(modelMapper.map(departmentDto,Department.class));

        } catch (Exception e) {
            throw new com.avanse.core.exception.TechnicalException(Constants.TECHNICAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, Constants.SPACE);
        }
        map.put(Constants.ID, department.getDeptId());
        map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
        map.put(Constants.ERROR, null);

        return map;
    }
    @GetMapping(EndPointReference.GET_DEPARTMENT)
    public Map<String, Object> getDepartmentById(@PathVariable Integer deptId) throws com.avanse.core.exception.TechnicalException, BussinessException {
       logger.info("Get Department By Id Called ");
        Map<String, Object> map = new HashMap<>();
        try {
            map.put(Constants.DTO_LIST, departmentRepo.findById(deptId));
            map.put(Constants.ERROR, null);
            map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
            return map;
        } catch (Exception e) {
            throw new com.avanse.core.exception.TechnicalException(Constants.TECHNICAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, Constants.SPACE);
        }
    }
    @GetMapping(EndPointReference.GET_ALL_DEPARTMENT)
    public Map<String, Object> departments() throws com.avanse.core.exception.TechnicalException, BussinessException {
        logger.info("Get All Department called");
        Map<String, Object> map = new HashMap<>();
        try {
            map.put(Constants.DTO_LIST, departmentRepo.findAll());
            map.put(Constants.ERROR, null);
            map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
            return map;
        } catch (Exception e) {
            throw new com.avanse.core.exception.TechnicalException(Constants.TECHNICAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, Constants.SPACE);
        }
    }
}