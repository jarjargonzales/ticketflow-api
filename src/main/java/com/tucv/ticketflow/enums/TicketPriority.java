package com.tucv.ticketflow.enums;

/**
 * Prioridad del ticket. Define el SLA (horas hasta dueAt) aplicado al crearlo.
 */
public enum TicketPriority {

    LOW(7 * 24L),
    MEDIUM(72L),
    HIGH(24L),
    CRITICAL(4L);

    private final long slaHours;

    TicketPriority(long slaHours) {
        this.slaHours = slaHours;
    }

    public long getSlaHours() {
        return slaHours;
    }
}
