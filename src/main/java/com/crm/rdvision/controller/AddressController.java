package com.crm.rdvision.controller;

import com.crm.rdvision.Exception.BussinessException;
import com.crm.rdvision.common.EndPointReference;
import com.crm.rdvision.dto.AddressDto;
import com.crm.rdvision.dto.SuccessResponse;
import com.crm.rdvision.entity.Address;
import com.crm.rdvision.repository.AddressRepo;
import com.crm.rdvision.utility.Constants;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@CrossOrigin
@RequestMapping("/address/")
public class AddressController {

    @Autowired
    AddressRepo addressRepo;

    @Autowired
    ModelMapper modelMapper;

    private static final Logger logger = LoggerFactory.getLogger(AddressController.class);

    @PostMapping(EndPointReference.CREATE_ADDRESS)
    public ResponseEntity<Map<String, Object>> createAddress(@RequestBody AddressDto addressDto) throws com.avanse.core.exception.TechnicalException, BussinessException {
        logger.info("Creating Address with {}", addressDto);  // Logger added
        Map<String, Object> responseMap = new HashMap<>();
        Address address = null;

        try {
            Optional<Address> existingAddressOpt = addressRepo.findByTicketId(addressDto.getTicketId());

            if (existingAddressOpt.isPresent()) {
                logger.info("Address exists for the given ticket id: {}", addressDto.getTicketId());  // Logger added
                address = existingAddressOpt.get();
                address.setCity(addressDto.getCity());
                address.setHouseNumber(addressDto.getHouseNumber());
                address.setCountry(addressDto.getCountry());
                address.setLandmark(addressDto.getLandmark());
                address.setState(addressDto.getState());
                address.setZipCode(addressDto.getZipCode());

                // Preserve the addressId and map other fields
                Integer addressId = address.getAddressId();
                modelMapper.map(addressDto, address);
                address.setAddressId(addressId); // Ensure the addressId is not altered
            } else {
                // Create a new address
                logger.info("No address found with the given ticket id, creating new address");  // Logger added
                address = modelMapper.map(addressDto, Address.class);
            }

            address = addressRepo.save(address);

            logger.info("Address saved with id: {}", address.getAddressId());  // Logger added

            responseMap.put(Constants.ID, address.getAddressId());
            responseMap.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
            responseMap.put(Constants.ERROR, null);
            return ResponseEntity.ok(responseMap);

        } catch (Exception e) {
            logger.error("Error occurred while creating address: {}", e.getMessage());  // Logger added
            throw new com.avanse.core.exception.TechnicalException(Constants.TECHNICAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, Constants.SPACE);
        }
    }

    @GetMapping(EndPointReference.GET_ADDRESS)
    public Map<String, Object> getAddress(@PathVariable String ticketId) throws com.avanse.core.exception.TechnicalException, BussinessException {
        logger.info("Fetching Address by Ticket ID: {}", ticketId);  // Logger added
        Map<String, Object> map = new HashMap<>();
        try {
            Optional<Address> address = addressRepo.findByTicketId(ticketId);
            if (address.isPresent()) {
                logger.info("Address found for Ticket ID: {}", ticketId);  // Logger added
            } else {
                logger.warn("No address found for Ticket ID: {}", ticketId);  // Logger added
            }
            map.put(Constants.DTO, address);
            map.put(Constants.ERROR, null);
            map.put(Constants.SUCCESS, new SuccessResponse(Constants.SUCCESS));
            return map;
        } catch (Exception e) {
            logger.error("Error occurred while fetching address for Ticket ID {}: {}", ticketId, e.getMessage());  // Logger added
            throw new com.avanse.core.exception.TechnicalException(Constants.TECHNICAL_ERROR, HttpStatus.INTERNAL_SERVER_ERROR, Constants.SPACE);
        }
    }
}
