package com.amat.admanagement.model;



import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "privileged_users")
public class PrivilegedUsers {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;
    private String role;
    private String status;
}
