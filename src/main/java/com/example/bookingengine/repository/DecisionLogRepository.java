package com.example.bookingengine.repository;

import com.example.bookingengine.domain.DecisionLog;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DecisionLogRepository extends JpaRepository<DecisionLog, Long> {
}
