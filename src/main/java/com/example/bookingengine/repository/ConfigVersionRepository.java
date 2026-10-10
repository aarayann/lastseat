package com.example.bookingengine.repository;

import com.example.bookingengine.domain.ConfigVersion;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ConfigVersionRepository extends JpaRepository<ConfigVersion, String> {
}
