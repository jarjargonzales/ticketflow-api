package com.tucv.ticketflow.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class CommentResponse {

    private Long id;
    private Long ticketId;
    private String authorEmail;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
