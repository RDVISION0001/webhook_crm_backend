package com.crm.rdvision.controller;

import com.crm.rdvision.dto.UserDto;
import com.crm.rdvision.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class JwtResponse {
    UserDto user;
    String jwtToken;
    private String refreshToken;
    private long attendanceId;


}
