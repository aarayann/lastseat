package com.example.bookingengine.repository;

import java.util.Optional;

import com.example.bookingengine.domain.Employee;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByUsername(String username);
}
