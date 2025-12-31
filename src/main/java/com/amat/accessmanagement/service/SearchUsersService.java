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
                searchString, page, size
        );

        Pageable pageable = PageRequest.of(page, size);

        boolean exact = true;
        String processedSearch = searchString;

        if (searchString != null && !searchString.isBlank()) {

            if (searchString.endsWith("*")) {
                exact = false;
                processedSearch = searchString.substring(0, searchString.length() - 1);
            }
        }

        log.debug(
                "Search mode resolved | original={} | processed={} | exact={}",
                searchString, processedSearch, exact
        );

        Page<UserEntity> users =
                userEnrollmentRepository.searchUsers(
                        processedSearch,
                        exact,
                        pageable
                );

        return users.map(user ->
                new UserSearchResponseDTO(
                        user.getEmployeeId(),
                        user.getDisplayName(),
                        user.getEmail()
                )
        );
    }


}
