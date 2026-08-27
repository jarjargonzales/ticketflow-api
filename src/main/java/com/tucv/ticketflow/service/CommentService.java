package com.tucv.ticketflow.service;

import com.tucv.ticketflow.dto.request.CommentCreateRequest;
import com.tucv.ticketflow.dto.response.CommentResponse;

import java.util.List;

public interface CommentService {

    CommentResponse addComment(Long ticketId, CommentCreateRequest request);

    List<CommentResponse> findByTicket(Long ticketId);
}
