package com.amat.admanagement.service;


import com.amat.admanagement.model.PrivilegedUsers;
import com.amat.admanagement.repository.PrivilegedUsersRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PrivilegedUsersService {

    @Autowired
    private PrivilegedUsersRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public PrivilegedUsers createUser(PrivilegedUsers user) {
        // Encode password before saving
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }
}
