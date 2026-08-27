package com.tucv.ticketflow.service.impl;

import com.tucv.ticketflow.dto.mapper.TicketMapper;
import com.tucv.ticketflow.dto.request.AssignTicketRequest;
import com.tucv.ticketflow.dto.request.StatusUpdateRequest;
import com.tucv.ticketflow.dto.request.TicketCreateRequest;
import com.tucv.ticketflow.dto.request.TicketUpdateRequest;
import com.tucv.ticketflow.dto.response.TicketResponse;
import com.tucv.ticketflow.entity.Ticket;
import com.tucv.ticketflow.enums.TicketPriority;
import com.tucv.ticketflow.enums.TicketStatus;
import com.tucv.ticketflow.exception.InvalidStateTransitionException;
import com.tucv.ticketflow.exception.ResourceNotFoundException;
import com.tucv.ticketflow.repository.TicketRepository;
import com.tucv.ticketflow.service.TicketService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;

    @Override
    public TicketResponse create(TicketCreateRequest request) {
        Ticket ticket = TicketMapper.toEntity(request);
        // Regla 1: status=OPEN y dueAt calculado según la prioridad (SLA).
        ticket.setStatus(TicketStatus.OPEN);
        ticket.setDueAt(LocalDateTime.now().plusHours(request.getPriority().getSlaHours()));
        return TicketMapper.toResponse(ticketRepository.save(ticket));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketResponse> findAll(TicketStatus status, TicketPriority priority, String assigneeEmail) {
        LocalDateTime now = LocalDateTime.now();
        return ticketRepository.findByFilters(status, priority, assigneeEmail).stream()
                .map(ticket -> TicketMapper.toResponse(ticket, now))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse findById(Long id) {
        return TicketMapper.toResponse(getTicketOrThrow(id));
    }

    @Override
    public TicketResponse update(Long id, TicketUpdateRequest request) {
        Ticket ticket = getTicketOrThrow(id);
        ticket.setTitle(request.getTitle());
        ticket.setDescription(request.getDescription());
        ticket.setPriority(request.getPriority());
        // Nota: el dueAt NO se recalcula al editar; el SLA se fija al crear el ticket.
        return TicketMapper.toResponse(ticketRepository.save(ticket));
    }

    @Override
    public TicketResponse updateStatus(Long id, StatusUpdateRequest request) {
        Ticket ticket = getTicketOrThrow(id);
        TicketStatus current = ticket.getStatus();
        TicketStatus target = request.getStatus();

        // Regla 2: máquina de estados. Cualquier transición fuera del flujo -> 409.
        if (!current.canTransitionTo(target)) {
            throw new InvalidStateTransitionException(current, target);
        }

        if (target == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(LocalDateTime.now());
        } else if (current == TicketStatus.RESOLVED && target == TicketStatus.IN_PROGRESS) {
            // Reapertura: se limpia la fecha de resolución.
            ticket.setResolvedAt(null);
        }

        ticket.setStatus(target);
        return TicketMapper.toResponse(ticketRepository.save(ticket));
    }

    @Override
    public TicketResponse assign(Long id, AssignTicketRequest request) {
        Ticket ticket = getTicketOrThrow(id);
        ticket.setAssigneeEmail(request.getAssigneeEmail());
        return TicketMapper.toResponse(ticketRepository.save(ticket));
    }

    private Ticket getTicketOrThrow(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado con id: " + id));
    }
}
