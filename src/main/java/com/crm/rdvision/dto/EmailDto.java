package com.crm.rdvision.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class EmailDto {
    private String email;
    private String name;
    private String mobile;
    private String ticketId;
    private ArrayList<String> productList;

}
