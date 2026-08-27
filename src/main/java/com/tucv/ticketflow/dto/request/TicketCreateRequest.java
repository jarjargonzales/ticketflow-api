package com.tucv.ticketflow.dto.request;

import com.tucv.ticketflow.enums.TicketPriority;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TicketCreateRequest {

    @NotBlank(message = "El título es obligatorio")
    @Size(max = 120, message = "El título no puede superar 120 caracteres")
    private String title;

    @NotBlank(message = "La descripción es obligatoria")
    @Size(max = 2000, message = "La descripción no puede superar 2000 caracteres")
    private String description;

    @NotNull(message = "La prioridad es obligatoria (LOW, MEDIUM, HIGH, CRITICAL)")
    private TicketPriority priority;

    @NotBlank(message = "El email del reportante es obligatorio")
    @Email(message = "El email del reportante no tiene un formato válido")
    private String reporterEmail;
}
