package com.example.bookingengine.repository;

import java.util.List;

import com.example.bookingengine.domain.RuleConfig;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RuleConfigRepository extends JpaRepository<RuleConfig, Long> {

    List<RuleConfig> findByClientIdOrderByIdAsc(String clientId);
}
