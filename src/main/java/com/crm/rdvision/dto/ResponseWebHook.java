package com.crm.rdvision.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@Data
public class ResponseWebHook {
    private int code;
    private String status;

    public ResponseWebHook(int code, String status) {
        this.code = code;
        this.status = status;
    }
}
