package com.crm.rdvision.service;

import com.crm.rdvision.Exception.BussinessException;
import com.crm.rdvision.Exception.ContractException;
import com.crm.rdvision.dto.SuccessResponse;
import com.crm.rdvision.dto.UserDto;
import com.crm.rdvision.entity.User;
import com.crm.rdvision.repository.UserRepo;
import com.crm.rdvision.utility.Constants;
import com.crm.rdvision.utility.EnglishConstants;
import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class UserImpl implements UserService {
    @Autowired
    UserRepo userRepo;
    @Autowired
    ModelMapper modelMapper;
    @Autowired
    PasswordEncoder passwordEncoder;

    @Override
    public Map<String, Object> createUser(UserDto userDto) throws com.avanse.core.exception.TechnicalException, BussinessException, ContractException {
        Map<String, Object> map = new HashMap<>();
        User user = null;

        try {
            // Validate the user DTO
            dtoValidationUtilForUser(userDto);

            // Check if the email already exists in the database
            Optional<User> existingUser = userRepo.findByEmail(userDto.getEmail());
            if (existingUser.isPresent()) {
                throw new BussinessException(HttpStatus.CONFLICT, "Email already exists: " + userDto.getEmail());
            }

            // Map DTO to User entity
            user = mapUserDtoToUser(userDto, 124);

            // Encode the password
            user.setPassword(passwordEncoder.encode(user.getPassword()));

            // Save the user to the repository
            user = userRepo.save(user);

            // Prepare the response map
            map.put(Constants.ID, user.getUserId());
            map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
            map.put(Constants.ERROR, null);

        } catch (BussinessException | ContractException e) {
            // Handle business and contract exceptions
            throw e;
        } catch (Exception e) {
            // Log the exception
            throw new com.avanse.core.exception.TechnicalException(Constants.TECHNICAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, Constants.SPACE);
        }

        return map;
    }


    User mapUserDtoToUser(UserDto userDto, Integer logedUserId) throws UnknownHostException {
       User user = modelMapper.map(userDto, User.class);
        user.setCreatedDate(String.valueOf(new Date()));
        user.setSystemIp(InetAddress.getLocalHost().getHostAddress());
        user.setCreatedBy(logedUserId);
        return user;
    }
    public static void dtoValidationUtilForUser(UserDto dto) throws ContractException {

        try {
            if (dto == null) {
//                log.error(DocumentServiceConstants.DTO_NULL_ERROR);
                throw new ContractException(HttpStatus.EXPECTATION_FAILED, "Getting user all fields error",
                        "allFields");//                        ConstantsUtil.getConstant("DTO_NULL_ERROR", language), "DmsDocumentDTO");
            }

            if (dto.getTeamId() == null) {
//                log.error(DocumentServiceConstants.LEAD_ID_ERROR);
                throw new ContractException(HttpStatus.EXPECTATION_FAILED, "Getting team id error",
                        "teamId");
            }
            if (dto.getDepartmentId() == null) {
//                log.error(DocumentServiceConstants.LEAD_ID_ERROR);
                throw new ContractException(HttpStatus.EXPECTATION_FAILED, "Getting department id error",
                        "deparmentId");
            }
            if (dto.getRoleId() == null) {
//                log.error(DocumentServiceConstants.LEAD_ID_ERROR);
                throw new ContractException(HttpStatus.EXPECTATION_FAILED, "Getting role id error",
                        "roleId");
            }


        }  catch (Exception e) {
//        log.error(LogUtil.errorLog(e));
        }
    }

}
