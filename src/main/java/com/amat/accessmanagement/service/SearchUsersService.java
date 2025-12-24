package com.amat.accessmanagement.service;


import com.amat.accessmanagement.dto.UserSearchResponseDTO;
import com.amat.accessmanagement.entity.UserEntity;
import com.amat.accessmanagement.repository.UserEnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchUsersService {

    @Autowired
    UserEnrollmentRepository userEnrollmentRepository;

    public Page<UserSearchResponseDTO> searchUsers(
            String searchString,
            int page,
            int size
    ) {

        log.info(
                "ENTER searchUsers | searchString={} | page={} | size={}",
                searchString,
                page,
                size
        );

        Pageable pageable = PageRequest.of(page, size);

        log.debug(
                "Pageable created | page={} | size={}",
                page,
                size
        );

        Page<UserEntity> users =
                userEnrollmentRepository.searchUsers(searchString, pageable);

        log.info(
                "Users fetched from repository | totalElements={} | totalPages={} | returnedSize={}",
                users.getTotalElements(),
                users.getTotalPages(),
                users.getNumberOfElements()
        );

        Page<UserSearchResponseDTO> response =
                users.map(user ->
                        new UserSearchResponseDTO(
                                user.getEmployeeId(),
                                user.getDisplayName(),
                                user.getEmail()
                        )
                );

        log.info(
                "EXIT searchUsers | responseSize={}",
                response.getNumberOfElements()
        );

        return response;
    }

}
