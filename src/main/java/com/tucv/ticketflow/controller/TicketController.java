package com.tucv.ticketflow.controller;

import com.tucv.ticketflow.dto.request.AssignTicketRequest;
import com.tucv.ticketflow.dto.request.StatusUpdateRequest;
import com.tucv.ticketflow.dto.request.TicketCreateRequest;
import com.tucv.ticketflow.dto.request.TicketUpdateRequest;
import com.tucv.ticketflow.dto.response.TicketResponse;
import com.tucv.ticketflow.enums.TicketPriority;
import com.tucv.ticketflow.enums.TicketStatus;
import com.tucv.ticketflow.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
@Tag(name = "Tickets", description = "Gestión de tickets de la mesa de ayuda")
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    @Operation(summary = "Crear ticket (status=OPEN y dueAt según SLA de la prioridad)")
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody TicketCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ticketService.create(request));
    }

    @GetMapping
    @Operation(summary = "Listar tickets con filtros opcionales por status, priority y assigneeEmail")
    public ResponseEntity<List<TicketResponse>> findAll(
            @RequestParam(required = false) TicketStatus status,
            @RequestParam(required = false) TicketPriority priority,
            @RequestParam(required = false) String assigneeEmail) {
        return ResponseEntity.ok(ticketService.findAll(status, priority, assigneeEmail));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener un ticket por id")
    public ResponseEntity<TicketResponse> findById(@PathVariable Long id) {
        return ResponseEntity.ok(ticketService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Editar title/description/priority de un ticket")
    public ResponseEntity<TicketResponse> update(@PathVariable Long id,
                                                 @Valid @RequestBody TicketUpdateRequest request) {
        return ResponseEntity.ok(ticketService.update(id, request));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cambiar el estado validando la máquina de estados (requiere ROLE_ADMIN)")
    public ResponseEntity<TicketResponse> updateStatus(@PathVariable Long id,
                                                       @Valid @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(ticketService.updateStatus(id, request));
    }

    @PatchMapping("/{id}/assign")
    @Operation(summary = "Asignar un agente al ticket (requiere ROLE_ADMIN)")
    public ResponseEntity<TicketResponse> assign(@PathVariable Long id,
                                                 @Valid @RequestBody AssignTicketRequest request) {
        return ResponseEntity.ok(ticketService.assign(id, request));
    }
}
