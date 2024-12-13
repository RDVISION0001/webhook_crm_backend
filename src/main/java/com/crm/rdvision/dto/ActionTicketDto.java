package com.crm.rdvision.dto;

import com.crm.rdvision.entity.TicketEntity;
import lombok.*;

@Data
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ActionTicketDto {
    private TicketEntity ticket;
    private int TotalTickets;
    private int currentTicketNo;
    private boolean isFirst;
    private boolean isLast;
}
