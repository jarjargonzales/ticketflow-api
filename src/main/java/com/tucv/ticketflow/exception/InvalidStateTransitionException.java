package com.tucv.ticketflow.exception;

import com.tucv.ticketflow.enums.TicketStatus;

/**
 * Se lanza cuando se intenta una transición de estado no permitida
 * por la máquina de estados (HTTP 409).
 */
public class InvalidStateTransitionException extends RuntimeException {

    public InvalidStateTransitionException(TicketStatus current, TicketStatus target) {
        super("Transición de estado no permitida: " + current + " -> " + target
                + ". Flujo válido: OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED "
                + "(y reapertura RESOLVED -> IN_PROGRESS).");
    }
}
