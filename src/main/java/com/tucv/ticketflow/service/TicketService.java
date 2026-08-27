package com.tucv.ticketflow.service;

import com.tucv.ticketflow.dto.request.AssignTicketRequest;
import com.tucv.ticketflow.dto.request.StatusUpdateRequest;
import com.tucv.ticketflow.dto.request.TicketCreateRequest;
import com.tucv.ticketflow.dto.request.TicketUpdateRequest;
import com.tucv.ticketflow.dto.response.TicketResponse;
import com.tucv.ticketflow.enums.TicketPriority;
import com.tucv.ticketflow.enums.TicketStatus;

import java.util.List;

public interface TicketService {

    TicketResponse create(TicketCreateRequest request);

    List<TicketResponse> findAll(TicketStatus status, TicketPriority priority, String assigneeEmail);

    TicketResponse findById(Long id);

    TicketResponse update(Long id, TicketUpdateRequest request);

    TicketResponse updateStatus(Long id, StatusUpdateRequest request);

    TicketResponse assign(Long id, AssignTicketRequest request);
}
