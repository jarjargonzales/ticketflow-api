package com.tucv.ticketflow.dto.request;

import com.tucv.ticketflow.enums.TicketStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusUpdateRequest {

    @NotNull(message = "El nuevo estado es obligatorio (OPEN, IN_PROGRESS, RESOLVED, CLOSED)")
    private TicketStatus status;
}
