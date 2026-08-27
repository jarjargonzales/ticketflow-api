package com.tucv.ticketflow.controller;

import com.tucv.ticketflow.dto.response.TicketResponse;
import com.tucv.ticketflow.enums.TicketPriority;
import com.tucv.ticketflow.enums.TicketStatus;
import com.tucv.ticketflow.exception.InvalidStateTransitionException;
import com.tucv.ticketflow.exception.ResourceNotFoundException;
import com.tucv.ticketflow.service.TicketService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests de capa web con MockMvc. Los filtros de seguridad se desactivan
 * (addFilters = false): la seguridad se prueba aparte y aquí el foco es
 * el contrato REST (códigos HTTP y cuerpo de error consistente).
 */
@WebMvcTest(TicketController.class)
@AutoConfigureMockMvc(addFilters = false)
class TicketControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TicketService ticketService;

    @Test
    @DisplayName("POST /api/tickets con body válido devuelve 201")
    void createReturns201() throws Exception {
        when(ticketService.create(any())).thenReturn(sampleResponse());

        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "No funciona el correo",
                                  "description": "El cliente de correo no sincroniza",
                                  "priority": "HIGH",
                                  "reporterEmail": "user@tucv.com"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.priority").value("HIGH"));
    }

    @Test
    @DisplayName("POST /api/tickets con título vacío devuelve 400 con fieldErrors")
    void createWithBlankTitleReturns400() throws Exception {
        mockMvc.perform(post("/api/tickets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "",
                                  "description": "Descripción válida",
                                  "priority": "LOW",
                                  "reporterEmail": "user@tucv.com"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"))
                .andExpect(jsonPath("$.fieldErrors.title").exists())
                .andExpect(jsonPath("$.path").value("/api/tickets"));
    }

    @Test
    @DisplayName("GET /api/tickets lista con filtros opcionales")
    void findAllReturns200() throws Exception {
        when(ticketService.findAll(eq(TicketStatus.OPEN), eq(TicketPriority.HIGH), any()))
                .thenReturn(List.of(sampleResponse()));

        mockMvc.perform(get("/api/tickets")
                        .param("status", "OPEN")
                        .param("priority", "HIGH")
                        .param("assigneeEmail", "agente@tucv.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1));
    }

    @Test
    @DisplayName("GET /api/tickets/{id} inexistente devuelve 404 con error JSON consistente")
    void findByIdReturns404() throws Exception {
        when(ticketService.findById(99L))
                .thenThrow(new ResourceNotFoundException("Ticket no encontrado con id: 99"));

        mockMvc.perform(get("/api/tickets/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Ticket no encontrado con id: 99"))
                .andExpect(jsonPath("$.path").value("/api/tickets/99"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    @DisplayName("PATCH /api/tickets/{id}/status con transición inválida devuelve 409")
    void invalidTransitionReturns409() throws Exception {
        when(ticketService.updateStatus(eq(1L), any()))
                .thenThrow(new InvalidStateTransitionException(TicketStatus.OPEN, TicketStatus.CLOSED));

        mockMvc.perform(patch("/api/tickets/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\": \"CLOSED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.message").value(
                        org.hamcrest.Matchers.containsString("OPEN -> CLOSED")));
    }

    private TicketResponse sampleResponse() {
        return TicketResponse.builder()
                .id(1L)
                .title("No funciona el correo")
                .description("El cliente de correo no sincroniza")
                .status(TicketStatus.OPEN)
                .priority(TicketPriority.HIGH)
                .reporterEmail("user@tucv.com")
                .dueAt(LocalDateTime.now().plusHours(24))
                .overdue(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
