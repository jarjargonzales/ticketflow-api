package com.tucv.ticketflow.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssignTicketRequest {

    @NotBlank(message = "El email del agente asignado es obligatorio")
    @Email(message = "El email del agente no tiene un formato válido")
    private String assigneeEmail;
}
