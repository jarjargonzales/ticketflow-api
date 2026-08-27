package com.tucv.ticketflow.repository;

import com.tucv.ticketflow.entity.Ticket;
import com.tucv.ticketflow.enums.TicketPriority;
import com.tucv.ticketflow.enums.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface TicketRepository extends JpaRepository<Ticket, Long> {

    @Query("""
            SELECT t FROM Ticket t
            WHERE (:status IS NULL OR t.status = :status)
              AND (:priority IS NULL OR t.priority = :priority)
              AND (:assigneeEmail IS NULL OR t.assigneeEmail = :assigneeEmail)
            ORDER BY t.createdAt DESC
            """)
    List<Ticket> findByFilters(@Param("status") TicketStatus status,
                               @Param("priority") TicketPriority priority,
                               @Param("assigneeEmail") String assigneeEmail);

    long countByStatus(TicketStatus status);

    long countByPriority(TicketPriority priority);

    long countByStatusInAndDueAtBefore(Collection<TicketStatus> statuses, LocalDateTime now);
}
