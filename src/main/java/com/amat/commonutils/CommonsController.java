package com.amat.commonutils;

import com.amat.accessmanagement.dto.UserSearchResponseDTO;
import com.amat.accessmanagement.entity.UserEntity;
import com.amat.accessmanagement.service.SearchUsersService;
import com.amat.accessmanagement.service.UserEnrollmentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/common-utils")
public class CommonsController {

    @Autowired
    UserEnrollmentService svc;

    @Autowired
    SearchUsersService searchUsersService;

    @GetMapping("/getLoggedInUserDetails")
    public ResponseEntity<Object> getUser(
            @RequestParam(name = "additionalDetails", defaultValue = "false") boolean additionalDetails,
            HttpServletRequest servletRequest) {

        log.info(
                "API HIT: getLoggedInUserDetails | additionalDetails={}",
                additionalDetails
        );

        String employeeId = servletRequest.getHeader("employeeId");

        log.debug("EmployeeId extracted from header | employeeId={}", employeeId);

        UserEntity user = svc.getUser(employeeId, additionalDetails);

        if (user == null) {

            log.warn(
                    "Logged-in user not found | employeeId={}",
                    employeeId
            );

            return ResponseEntity.status(404).body(
                    Map.of(
                            "status", "failed",
                            "message", "User not found: " + employeeId
                    )
            );
        }

        log.info(
                "Logged-in user details fetched successfully | employeeId={}",
                employeeId
        );

        return ResponseEntity.ok(
                Map.of(
                        "status", "success",
                        "data", user
                )
        );
    }

    @GetMapping("/searchUsers")
    public Page<UserSearchResponseDTO> getUsers(
            @RequestParam(required = false) String searchString,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    ) {
        log.info(
                "ENTER /searchUsers | searchString={} | page={} | size={}",
                searchString,
                page,
                size
        );

        Page<UserSearchResponseDTO> response =
                searchUsersService.searchUsers(searchString, page, size);

        log.info(
                "EXIT /searchUsers | totalElements={} | totalPages={} | returnedSize={}",
                response.getTotalElements(),
                response.getTotalPages(),
                response.getNumberOfElements()
        );

        return response;
    }

}
