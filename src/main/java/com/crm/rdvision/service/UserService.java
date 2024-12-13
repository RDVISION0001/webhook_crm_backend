package com.crm.rdvision.service;

import com.crm.rdvision.Exception.BussinessException;
import com.crm.rdvision.Exception.ContractException;
import com.crm.rdvision.dto.UserDto;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.util.Map;
@Service
public interface UserService {
    public Map<String, Object> createUser(UserDto userDto) throws com.avanse.core.exception.TechnicalException, BussinessException, ContractException;

}
