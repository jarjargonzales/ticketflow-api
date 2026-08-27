package com.tucv.ticketflow.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.util.Map;

@Getter
@Builder
public class DashboardStatsResponse {

    private long total;
    private Map<String, Long> byStatus;
    private Map<String, Long> byPriority;
    private long overdueCount;
}
