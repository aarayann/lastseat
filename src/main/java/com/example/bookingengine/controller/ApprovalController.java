package com.example.bookingengine.controller;

import java.util.List;

import com.example.bookingengine.dto.BookingView;
import com.example.bookingengine.service.BookingService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/approvals")
public class ApprovalController {

    private final BookingService bookingService;

    public ApprovalController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @GetMapping("/pending")
    public List<BookingView> pending() {
        return bookingService.pendingApprovals();
    }

    @PostMapping("/{id}/approve")
    public BookingView approve(@PathVariable("id") Long id) {
        return bookingService.decideApproval(id, true);
    }

    @PostMapping("/{id}/reject")
    public BookingView reject(@PathVariable("id") Long id) {
        return bookingService.decideApproval(id, false);
    }
}
