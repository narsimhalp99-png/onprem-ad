package com.amat.admanagement.service;

import com.amat.admanagement.model.PrivilegedUsers;
import com.amat.admanagement.repository.PrivilegedUsersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PrivilegedUsersService {

    @Autowired
    private PrivilegedUsersRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public PrivilegedUsers createUser(PrivilegedUsers user) {

        log.info("START createPrivilegedUser | username={}", user.getUsername());

        user.setPassword(passwordEncoder.encode(user.getPassword()));

        PrivilegedUsers saved = userRepository.save(user);

        log.info("END createPrivilegedUser | username={}", user.getUsername());

        return saved;
    }
}
