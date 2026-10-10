package com.example.bookingengine.domain;

import java.time.LocalTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Employee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true)
    private String username;

    private String clientId;

    private String businessUnit;

    private Long costCentreId;

    @Enumerated(EnumType.STRING)
    private Gender gender;

    private LocalTime shiftStart;

    private LocalTime shiftEnd;

    protected Employee() {
    }

    public Employee(String name, String username, String clientId, String businessUnit,
                    Long costCentreId, Gender gender, LocalTime shiftStart, LocalTime shiftEnd) {
        this.name = name;
        this.username = username;
        this.clientId = clientId;
        this.businessUnit = businessUnit;
        this.costCentreId = costCentreId;
        this.gender = gender;
        this.shiftStart = shiftStart;
        this.shiftEnd = shiftEnd;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUsername() {
        return username;
    }

    public String getClientId() {
        return clientId;
    }

    public String getBusinessUnit() {
        return businessUnit;
    }

    public Long getCostCentreId() {
        return costCentreId;
    }

    public Gender getGender() {
        return gender;
    }

    public LocalTime getShiftStart() {
        return shiftStart;
    }

    public LocalTime getShiftEnd() {
        return shiftEnd;
    }
}
