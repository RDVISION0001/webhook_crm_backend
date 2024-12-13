
package com.crm.rdvision.feignClient;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@FeignClient(name = "data", url = "https://mapi.indiamart.com/wservce/crm/crmListing/v2/?glusr_crm_key=mRyyF7xk4nzJT/ep5neJ7liLo1rMlDZkXA==")
@Component
public interface TichetFiegnClient {

    @GetMapping
    String getTickets();
}

