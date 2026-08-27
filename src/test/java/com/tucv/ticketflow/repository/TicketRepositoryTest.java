package com.tucv.ticketflow.repository;

import com.tucv.ticketflow.config.JpaAuditingConfig;
import com.tucv.ticketflow.entity.Ticket;
import com.tucv.ticketflow.entity.TicketComment;
import com.tucv.ticketflow.enums.TicketPriority;
import com.tucv.ticketflow.enums.TicketStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests de persistencia con @DataJpaTest sobre H2 embebido.
 * Se importa JpaAuditingConfig para que created_at/updated_at se completen
 * (los slice tests no cargan la configuración completa de la app).
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class TicketRepositoryTest {

    @Autowired
    private TicketRepository ticketRepository;

    @Autowired
    private TicketCommentRepository commentRepository;

    @Test
    @DisplayName("save: la auditoría completa createdAt y updatedAt")
    void auditingFillsTimestamps() {
        Ticket saved = ticketRepository.save(newTicket("Ticket A", TicketStatus.OPEN,
                TicketPriority.HIGH, LocalDateTime.now().plusHours(24)));

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("findByFilters: filtra por status, priority y assigneeEmail")
    void findByFilters() {
        ticketRepository.save(newTicket("A", TicketStatus.OPEN, TicketPriority.HIGH,
                LocalDateTime.now().plusHours(24)));
        ticketRepository.save(newTicket("B", TicketStatus.OPEN, TicketPriority.LOW,
                LocalDateTime.now().plusDays(7)));

        Ticket assigned = newTicket("C", TicketStatus.IN_PROGRESS, TicketPriority.HIGH,
                LocalDateTime.now().plusHours(24));
        assigned.setAssigneeEmail("agente@tucv.com");
        ticketRepository.save(assigned);

        assertThat(ticketRepository.findByFilters(TicketStatus.OPEN, null, null)).hasSize(2);
        assertThat(ticketRepository.findByFilters(null, TicketPriority.HIGH, null)).hasSize(2);
        assertThat(ticketRepository.findByFilters(null, null, "agente@tucv.com")).hasSize(1);
        assertThat(ticketRepository.findByFilters(TicketStatus.IN_PROGRESS,
                TicketPriority.HIGH, "agente@tucv.com")).hasSize(1);
        assertThat(ticketRepository.findByFilters(TicketStatus.CLOSED, null, null)).isEmpty();
    }

    @Test
    @DisplayName("countByStatusInAndDueAtBefore: solo tickets vivos con dueAt vencido")
    void countOverdue() {
        // Vencido y abierto -> cuenta
        ticketRepository.save(newTicket("Vencido", TicketStatus.OPEN, TicketPriority.HIGH,
                LocalDateTime.now().minusHours(1)));
        // Vencido pero ya resuelto -> NO cuenta
        ticketRepository.save(newTicket("Resuelto", TicketStatus.RESOLVED, TicketPriority.HIGH,
                LocalDateTime.now().minusHours(1)));
        // Abierto pero dentro del SLA -> NO cuenta
        ticketRepository.save(newTicket("En tiempo", TicketStatus.IN_PROGRESS, TicketPriority.LOW,
                LocalDateTime.now().plusDays(7)));

        long overdue = ticketRepository.countByStatusInAndDueAtBefore(
                List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS), LocalDateTime.now());

        assertThat(overdue).isEqualTo(1);
    }

    @Test
    @DisplayName("ticket_comments: persistencia con FK y consulta ordenada por createdAt")
    void commentsArePersisted() {
        Ticket ticket = ticketRepository.save(newTicket("Con comentarios", TicketStatus.OPEN,
                TicketPriority.MEDIUM, LocalDateTime.now().plusHours(72)));

        commentRepository.save(TicketComment.builder()
                .ticket(ticket).authorEmail("agente@tucv.com").message("Estoy revisando").build());
        commentRepository.save(TicketComment.builder()
                .ticket(ticket).authorEmail("user@tucv.com").message("Gracias").build());

        List<TicketComment> comments =
                commentRepository.findByTicketIdOrderByCreatedAtAsc(ticket.getId());

        assertThat(comments).hasSize(2);
        assertThat(comments.get(0).getTicket().getId()).isEqualTo(ticket.getId());
    }

    private Ticket newTicket(String title, TicketStatus status, TicketPriority priority,
                             LocalDateTime dueAt) {
        return Ticket.builder()
                .title(title)
                .description("Descripción de " + title)
                .status(status)
                .priority(priority)
                .reporterEmail("user@tucv.com")
                .dueAt(dueAt)
                .build();
    }
}
