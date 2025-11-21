package com.amat.commonutils;


import com.amat.accessmanagement.entity.UserEntity;
import com.amat.accessmanagement.service.UserEnrollmentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/common-utils")
public class CommonsController {

    @Autowired
    UserEnrollmentService svc;

    @GetMapping("/getLoggedInUserDetails")
    public ResponseEntity<Object> getUser(@RequestParam(name = "additionalDetails", defaultValue = "false") boolean additionalDetails, HttpServletRequest servletRequest) {
        Long employeeId = Long.valueOf(servletRequest.getHeader("employeeId"));

        UserEntity user = svc.getUser(employeeId, additionalDetails);

        if (user == null) {
            return ResponseEntity.status(404).body(
                    Map.of(
                            "status", "failed",
                            "message", "User not found: " + employeeId
                    )
            );
        }

        return ResponseEntity.ok(
                Map.of(
                        "status", "success",
                        "data", user
                )
        );
    }

}
