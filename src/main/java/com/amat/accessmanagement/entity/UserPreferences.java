package com.amat.accessmanagement.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_preferences")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferences {

    @Id
    @Column(name = "employee_id", nullable = false)
    private String employeeId;

    @Column(columnDefinition = "nvarchar(max)")
    private String favTiles;

    @Generated(GenerationTime.INSERT)
    @Column(
            nullable = false,
            updatable = false,
            insertable = false,
            columnDefinition = "DATETIME DEFAULT GETDATE()"
    )
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}