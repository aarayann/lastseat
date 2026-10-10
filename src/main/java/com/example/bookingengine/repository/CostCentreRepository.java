package com.example.bookingengine.repository;

import java.util.Optional;

import com.example.bookingengine.domain.CostCentre;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CostCentreRepository extends JpaRepository<CostCentre, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select c from CostCentre c where c.id = :id")
    Optional<CostCentre> lockById(@Param("id") Long id);
}
