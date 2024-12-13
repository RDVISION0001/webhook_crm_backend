package com.crm.rdvision.dto;

import lombok.*;

import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Data
public class JobRequestDto {
    private String token;
    private String tenant_id;
    private String job_id;
    private List<NumberDetails> numbers;
    private  Object agent_id;


    @NoArgsConstructor
    @AllArgsConstructor
    @Getter
    @Setter
    @Data
    public static class NumberDetails{
          private String job_fname;
          private String job_lname;
          private String job_address;
          private String job_email_id;
          private String job_max_try;
          private String job_next_dial_time;
          private String number;
          private String contact_number_1;
          private String contact_number_2;
          private String customfield1;
    }

}
