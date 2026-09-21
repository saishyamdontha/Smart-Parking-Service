package com.example.parking.model;

import jakarta.persistence.*;

@Entity
@Table(name = "parking_spaces")
public class ParkingSpace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String area;

    @Column(name = "space_number", nullable = false)
    private String spaceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParkingSpaceStatus status;

    public ParkingSpace() {
    }

    public ParkingSpace(String area, String spaceNumber,
                         ParkingSpaceStatus status) {
        this.area = area;
        this.spaceNumber = spaceNumber;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public String getArea() {
        return area;
    }

    public String getSpaceNumber() {
        return spaceNumber;
    }

    public ParkingSpaceStatus getStatus() {
        return status;
    }

    public void setStatus(ParkingSpaceStatus status) {
        this.status = status;
    }
}
