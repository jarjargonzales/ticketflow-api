package com.tucv.ticketflow.enums;

/**
 * Estado del ticket. Máquina de estados:
 *
 *   OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED (terminal)
 *   RESOLVED -> IN_PROGRESS  (reapertura)
 */
public enum TicketStatus {

    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED;

    /**
     * Indica si la transición desde este estado hacia {@code target} es válida.
     */
    public boolean canTransitionTo(TicketStatus target) {
        if (target == null) {
            return false;
        }
        return switch (this) {
            case OPEN -> target == IN_PROGRESS;
            case IN_PROGRESS -> target == RESOLVED;
            case RESOLVED -> target == CLOSED || target == IN_PROGRESS;
            case CLOSED -> false;
        };
    }

    /**
     * Estados en los que el ticket sigue "vivo" (cuentan para el indicador overdue).
     */
    public boolean isOpen() {
        return this == OPEN || this == IN_PROGRESS;
    }
}
