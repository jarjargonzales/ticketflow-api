package com.tucv.ticketflow.service.impl;

import com.tucv.ticketflow.dto.mapper.TicketMapper;
import com.tucv.ticketflow.dto.request.CommentCreateRequest;
import com.tucv.ticketflow.dto.response.CommentResponse;
import com.tucv.ticketflow.entity.Ticket;
import com.tucv.ticketflow.entity.TicketComment;
import com.tucv.ticketflow.exception.ResourceNotFoundException;
import com.tucv.ticketflow.repository.TicketCommentRepository;
import com.tucv.ticketflow.repository.TicketRepository;
import com.tucv.ticketflow.service.CommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

    private final TicketCommentRepository commentRepository;
    private final TicketRepository ticketRepository;

    @Override
    public CommentResponse addComment(Long ticketId, CommentCreateRequest request) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket no encontrado con id: " + ticketId));

        TicketComment comment = TicketComment.builder()
                .ticket(ticket)
                .authorEmail(request.getAuthorEmail())
                .message(request.getMessage())
                .build();

        return TicketMapper.toResponse(commentRepository.save(comment));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentResponse> findByTicket(Long ticketId) {
        if (!ticketRepository.existsById(ticketId)) {
            throw new ResourceNotFoundException("Ticket no encontrado con id: " + ticketId);
        }
        return commentRepository.findByTicketIdOrderByCreatedAtAsc(ticketId).stream()
                .map(TicketMapper::toResponse)
                .toList();
    }
}
