package com.tucv.ticketflow.dto.mapper;

import com.tucv.ticketflow.dto.request.TicketCreateRequest;
import com.tucv.ticketflow.dto.response.CommentResponse;
import com.tucv.ticketflow.dto.response.TicketResponse;
import com.tucv.ticketflow.entity.Ticket;
import com.tucv.ticketflow.entity.TicketComment;
import com.tucv.ticketflow.enums.TicketStatus;

import java.time.LocalDateTime;

/**
 * Conversión entidad <-> DTO. Los DTOs nunca exponen la entidad JPA.
 */
public final class TicketMapper {

    private TicketMapper() {
    }

    public static Ticket toEntity(TicketCreateRequest request) {
        return Ticket.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .priority(request.getPriority())
                .reporterEmail(request.getReporterEmail())
                .status(TicketStatus.OPEN)
                .build();
    }

    public static TicketResponse toResponse(Ticket ticket) {
        return toResponse(ticket, LocalDateTime.now());
    }

    public static TicketResponse toResponse(Ticket ticket, LocalDateTime now) {
        boolean overdue = ticket.getStatus() != null
                && ticket.getStatus().isOpen()
                && ticket.getDueAt() != null
                && ticket.getDueAt().isBefore(now);

        return TicketResponse.builder()
                .id(ticket.getId())
                .title(ticket.getTitle())
                .description(ticket.getDescription())
                .status(ticket.getStatus())
                .priority(ticket.getPriority())
                .reporterEmail(ticket.getReporterEmail())
                .assigneeEmail(ticket.getAssigneeEmail())
                .dueAt(ticket.getDueAt())
                .resolvedAt(ticket.getResolvedAt())
                .overdue(overdue)
                .createdAt(ticket.getCreatedAt())
                .updatedAt(ticket.getUpdatedAt())
                .build();
    }

    public static CommentResponse toResponse(TicketComment comment) {
        return CommentResponse.builder()
                .id(comment.getId())
                .ticketId(comment.getTicket() != null ? comment.getTicket().getId() : null)
                .authorEmail(comment.getAuthorEmail())
                .message(comment.getMessage())
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }
}
