package com.amat.commonutils.entity;


import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Generated;
import org.hibernate.annotations.GenerationTime;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Audit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Generated(GenerationTime.INSERT)
    @Column(
            name = "created_date",
            nullable = false,
            updatable = false,
            insertable = false,
            columnDefinition = "DATETIME DEFAULT GETDATE()"
    )
    private LocalDateTime createdDate;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private String actor;

    @Column(nullable = false)
    private String target;

    @Column(nullable = false)
    private String operation;

    @Column(name = "object_name", columnDefinition = "nvarchar(max)")
    private String objectName;

    @Column(name = "old_value", columnDefinition = "nvarchar(max)")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "nvarchar(max)")
    private String newValue;

    @Column(columnDefinition = "nvarchar(max)")
    private String comment;
}
