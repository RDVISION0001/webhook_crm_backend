package com.crm.rdvision.dto;

import jakarta.persistence.Column;
import jakarta.persistence.Lob;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Transient;

import java.util.List;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class UserDto {
    private Integer userId;
    private String firstName;
    private String lastName;
    private String password;
    private String email;
    private String phoneNumber;
    private Integer roleId;
    private Integer departmentId;
    private Integer teamId;
    private Boolean onBreak;
    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] imageData;
    private String profilepic;
    private String createdDate;
    private Integer createdBy;
    private Integer updatedBy;
    private String systemIp;
    private Character userStatus;
    @Transient
    private DepartmentDto departmentDto;
    @Transient
    private RoleDto roleDto;
    @Transient
    private TeamDto teamDto;




}
