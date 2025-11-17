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
        PrivilegedUsers user = privilegedUsersRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        log.info("user fetched is:::{}",user, user.toString());
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                Collections.singletonList(new SimpleGrantedAuthority(user.getRole()))
        );
    }
}
