package com.crm.rdvision.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Map;

@Controller
@RequestMapping("/hodu")
public class HoduWebhookController {





    @PostMapping("/call_events")
    public void callEvents(@RequestBody Map<String,String> payload){
        System.out.println(payload.toString());
    }
}
