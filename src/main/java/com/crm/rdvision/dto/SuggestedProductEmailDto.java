package com.crm.rdvision.dto;

import com.crm.rdvision.entity.Product;
import com.crm.rdvision.entity.TicketEntity;
import com.crm.rdvision.entity.UploadTicket;
import lombok.*;

import java.util.List;

@Data
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class SuggestedProductEmailDto {

    private String text;
    private TicketEntity ticket;
    private UploadTicket uploadTicket;
    private List<Integer> productsIds;
    private int temp;
    private int userId;
}
