CREATE TABLE parking_spaces (
    id BIGSERIAL PRIMARY KEY,
    area VARCHAR(100) NOT NULL,
    space_number VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL
);

INSERT INTO parking_spaces (area, space_number, status)
VALUES
('Area A', 'A1', 'AVAILABLE'),
('Area A', 'A2', 'AVAILABLE'),
('Area A', 'A3', 'AVAILABLE'),
('Area A', 'A4', 'OCCUPIED'),
('Area A', 'A5', 'AVAILABLE'),
('Area B', 'B1', 'AVAILABLE'),
('Area B', 'B2', 'AVAILABLE'),
('Area B', 'B3', 'OCCUPIED'),
('Area B', 'B4', 'AVAILABLE'),
('Area B', 'B5', 'AVAILABLE');
