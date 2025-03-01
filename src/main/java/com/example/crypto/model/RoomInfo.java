package com.example.crypto.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Data
@AllArgsConstructor
@Builder
@NoArgsConstructor
public class RoomInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    private byte[] p;
    private byte[] g;
    @Column(unique = true)
    private long roomId;
}
