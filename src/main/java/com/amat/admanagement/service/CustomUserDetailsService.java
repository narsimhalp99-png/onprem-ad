package com.amat.admanagement.service;

import com.amat.admanagement.model.PrivilegedUsers;
import com.amat.admanagement.repository.PrivilegedUsersRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Slf4j
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final PrivilegedUsersRepository privilegedUsersRepository;

    public CustomUserDetailsService(PrivilegedUsersRepository privilegedUsersRepository) {
        this.privilegedUsersRepository = privilegedUsersRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        log.info("START loadUserByUsername | username={}", username);

        PrivilegedUsers user = privilegedUsersRepository.findByUsername(username)
                .orElseThrow(() -> {
                    log.warn("User not found during authentication | username={}", username);
                    return new UsernameNotFoundException("User not found");
                });

        log.info(
                "User fetched for authentication | username={} | role={}",
                user.getUsername(),
                user.getRole()
        );

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(
                        new SimpleGrantedAuthority(user.getRole())
                )
        );
    }
}
