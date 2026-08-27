package com.tucv.ticketflow.dto.response;

import com.tucv.ticketflow.enums.TicketPriority;
import com.tucv.ticketflow.enums.TicketStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class TicketResponse {

    private Long id;
    private String title;
    private String description;
    private TicketStatus status;
    private TicketPriority priority;
    private String reporterEmail;
    private String assigneeEmail;
    private LocalDateTime dueAt;
    private LocalDateTime resolvedAt;

    /** true si el ticket sigue abierto/en progreso y su dueAt ya venció. */
    private boolean overdue;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
