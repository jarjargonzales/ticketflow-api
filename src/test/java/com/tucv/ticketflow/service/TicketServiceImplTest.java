package com.tucv.ticketflow.service;

import com.tucv.ticketflow.dto.request.AssignTicketRequest;
import com.tucv.ticketflow.dto.request.StatusUpdateRequest;
import com.tucv.ticketflow.dto.request.TicketCreateRequest;
import com.tucv.ticketflow.dto.response.TicketResponse;
import com.tucv.ticketflow.entity.Ticket;
import com.tucv.ticketflow.enums.TicketPriority;
import com.tucv.ticketflow.enums.TicketStatus;
import com.tucv.ticketflow.exception.InvalidStateTransitionException;
import com.tucv.ticketflow.exception.ResourceNotFoundException;
import com.tucv.ticketflow.repository.TicketRepository;
import com.tucv.ticketflow.service.impl.TicketServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
// LENIENT: el stub común de save() no aplica a los tests que no persisten
// (p. ej. transiciones inválidas o findById con 404).
@MockitoSettings(strictness = Strictness.LENIENT)
class TicketServiceImplTest {

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketServiceImpl ticketService;

    @BeforeEach
    void setUp() {
        when(ticketRepository.save(any(Ticket.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    // ------------------------------------------------------------------
    // Regla 1: SLA al crear
    // ------------------------------------------------------------------

    @ParameterizedTest(name = "prioridad {0} -> dueAt en {1} horas")
    @CsvSource({"CRITICAL,4", "HIGH,24", "MEDIUM,72", "LOW,168"})
    @DisplayName("create: status OPEN y dueAt según SLA de la prioridad")
    void createCalculatesDueAtByPriority(TicketPriority priority, long expectedHours) {
        TicketCreateRequest request = new TicketCreateRequest();
        request.setTitle("Falla en producción");
        request.setDescription("El módulo de pagos no responde");
        request.setPriority(priority);
        request.setReporterEmail("user@tucv.com");

        LocalDateTime before = LocalDateTime.now();
        TicketResponse response = ticketService.create(request);
        LocalDateTime after = LocalDateTime.now();

        assertThat(response.getStatus()).isEqualTo(TicketStatus.OPEN);
        assertThat(response.getDueAt()).isBetween(
                before.plusHours(expectedHours).minusSeconds(1),
                after.plusHours(expectedHours).plusSeconds(1));
    }

    // ------------------------------------------------------------------
    // Regla 2: máquina de estados
    // ------------------------------------------------------------------

    @Test
    @DisplayName("updateStatus: OPEN -> IN_PROGRESS es válida")
    void openToInProgressIsValid() {
        Ticket ticket = ticketWithStatus(TicketStatus.OPEN);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        TicketResponse response = ticketService.updateStatus(1L, statusRequest(TicketStatus.IN_PROGRESS));

        assertThat(response.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("updateStatus: IN_PROGRESS -> RESOLVED guarda resolvedAt")
    void inProgressToResolvedSetsResolvedAt() {
        Ticket ticket = ticketWithStatus(TicketStatus.IN_PROGRESS);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        TicketResponse response = ticketService.updateStatus(1L, statusRequest(TicketStatus.RESOLVED));

        assertThat(response.getStatus()).isEqualTo(TicketStatus.RESOLVED);
        assertThat(response.getResolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("updateStatus: RESOLVED -> IN_PROGRESS (reopen) limpia resolvedAt")
    void reopenClearsResolvedAt() {
        Ticket ticket = ticketWithStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now().minusHours(2));
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        TicketResponse response = ticketService.updateStatus(1L, statusRequest(TicketStatus.IN_PROGRESS));

        assertThat(response.getStatus()).isEqualTo(TicketStatus.IN_PROGRESS);
        assertThat(response.getResolvedAt()).isNull();
    }

    @Test
    @DisplayName("updateStatus: RESOLVED -> CLOSED es válida")
    void resolvedToClosedIsValid() {
        Ticket ticket = ticketWithStatus(TicketStatus.RESOLVED);
        ticket.setResolvedAt(LocalDateTime.now().minusHours(1));
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        TicketResponse response = ticketService.updateStatus(1L, statusRequest(TicketStatus.CLOSED));

        assertThat(response.getStatus()).isEqualTo(TicketStatus.CLOSED);
    }

    @Test
    @DisplayName("updateStatus: OPEN -> RESOLVED lanza InvalidStateTransitionException (409)")
    void openToResolvedIsRejected() {
        Ticket ticket = ticketWithStatus(TicketStatus.OPEN);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.updateStatus(1L, statusRequest(TicketStatus.RESOLVED)))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("OPEN -> RESOLVED");
    }

    @Test
    @DisplayName("updateStatus: CLOSED es terminal, cualquier transición lanza 409")
    void closedIsTerminal() {
        Ticket ticket = ticketWithStatus(TicketStatus.CLOSED);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        assertThatThrownBy(() -> ticketService.updateStatus(1L, statusRequest(TicketStatus.OPEN)))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("CLOSED");
    }

    // ------------------------------------------------------------------
    // Otras reglas
    // ------------------------------------------------------------------

    @Test
    @DisplayName("findById: ticket inexistente lanza ResourceNotFoundException (404)")
    void findByIdNotFound() {
        when(ticketRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> ticketService.findById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    @DisplayName("assign: guarda el email del agente asignado")
    void assignSetsAssigneeEmail() {
        Ticket ticket = ticketWithStatus(TicketStatus.OPEN);
        when(ticketRepository.findById(1L)).thenReturn(Optional.of(ticket));

        AssignTicketRequest request = new AssignTicketRequest();
        request.setAssigneeEmail("agente@tucv.com");

        TicketResponse response = ticketService.assign(1L, request);

        assertThat(response.getAssigneeEmail()).isEqualTo("agente@tucv.com");
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    private Ticket ticketWithStatus(TicketStatus status) {
        Ticket ticket = Ticket.builder()
                .title("Ticket de prueba")
                .description("Descripción")
                .status(status)
                .priority(TicketPriority.MEDIUM)
                .reporterEmail("user@tucv.com")
                .dueAt(LocalDateTime.now().plusHours(72))
                .build();
        ReflectionTestUtils.setField(ticket, "id", 1L);
        return ticket;
    }

    private StatusUpdateRequest statusRequest(TicketStatus status) {
        StatusUpdateRequest request = new StatusUpdateRequest();
        request.setStatus(status);
        return request;
    }
}
