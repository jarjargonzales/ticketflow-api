package com.tucv.ticketflow.controller;

import com.tucv.ticketflow.dto.request.CommentCreateRequest;
import com.tucv.ticketflow.dto.response.CommentResponse;
import com.tucv.ticketflow.service.CommentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tickets/{ticketId}/comments")
@RequiredArgsConstructor
@Tag(name = "Comentarios", description = "Comentarios asociados a un ticket")
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @Operation(summary = "Agregar un comentario al ticket")
    public ResponseEntity<CommentResponse> addComment(@PathVariable Long ticketId,
                                                      @Valid @RequestBody CommentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.addComment(ticketId, request));
    }

    @GetMapping
    @Operation(summary = "Listar comentarios del ticket (orden cronológico)")
    public ResponseEntity<List<CommentResponse>> findByTicket(@PathVariable Long ticketId) {
        return ResponseEntity.ok(commentService.findByTicket(ticketId));
    }
}
