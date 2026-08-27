package com.tucv.ticketflow.service.impl;

import com.tucv.ticketflow.dto.response.DashboardStatsResponse;
import com.tucv.ticketflow.enums.TicketPriority;
import com.tucv.ticketflow.enums.TicketStatus;
import com.tucv.ticketflow.repository.TicketRepository;
import com.tucv.ticketflow.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardServiceImpl implements DashboardService {

    private final TicketRepository ticketRepository;

    @Override
    public DashboardStatsResponse getStats() {
        Map<TicketStatus, Long> byStatus = Arrays.stream(TicketStatus.values())
                .collect(Collectors.toMap(s -> s,
                        ticketRepository::countByStatus,
                        (a, b) -> a,
                        () -> new EnumMap<>(TicketStatus.class)));

        Map<TicketPriority, Long> byPriority = Arrays.stream(TicketPriority.values())
                .collect(Collectors.toMap(p -> p,
                        ticketRepository::countByPriority,
                        (a, b) -> a,
                        () -> new EnumMap<>(TicketPriority.class)));

        long overdue = ticketRepository.countByStatusInAndDueAtBefore(
                List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS), LocalDateTime.now());

        return DashboardStatsResponse.builder()
                .total(ticketRepository.count())
                .byStatus(toStringKeyMap(byStatus))
                .byPriority(toStringKeyMap(byPriority))
                .overdueCount(overdue)
                .build();
    }

    private static <E extends Enum<E>> Map<String, Long> toStringKeyMap(Map<E, Long> map) {
        return map.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey().name(), Map.Entry::getValue,
                        (a, b) -> a, java.util.LinkedHashMap::new));
    }
}
