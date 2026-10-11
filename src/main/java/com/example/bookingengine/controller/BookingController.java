package com.example.bookingengine.controller;

import com.example.bookingengine.dto.BookingRequest;
import com.example.bookingengine.dto.BookingView;
import com.example.bookingengine.dto.DecisionResponse;
import com.example.bookingengine.service.BookingService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bookings")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    @PostMapping("/evaluate")
    public DecisionResponse evaluate(@Valid @RequestBody BookingRequest request,
                                     @RequestParam(name = "explain", defaultValue = "true") boolean explain,
                                     Authentication auth) {
        return bookingService.evaluate(request, auth.getName(), isAdmin(auth), explain);
    }

    @PostMapping
    public ResponseEntity<DecisionResponse> book(@Valid @RequestBody BookingRequest request,
                                                 @RequestParam(name = "explain", defaultValue = "false") boolean explain,
                                                 Authentication auth) {
        DecisionResponse response = bookingService.book(request, auth.getName(), isAdmin(auth), explain);
        HttpStatus status = switch (response.decision()) {
            case ALLOW -> HttpStatus.CREATED;
            case NEEDS_APPROVAL -> HttpStatus.ACCEPTED;
            case DENY -> HttpStatus.UNPROCESSABLE_ENTITY;
        };
        return ResponseEntity.status(status).body(response);
    }

    @GetMapping("/{id}")
    public BookingView get(@PathVariable("id") Long id, Authentication auth) {
        return bookingService.get(id, auth.getName(), isAdmin(auth));
    }

    @PostMapping("/{id}/cancel")
    public BookingView cancel(@PathVariable("id") Long id, Authentication auth) {
        return bookingService.cancel(id, auth.getName(), isAdmin(auth));
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
