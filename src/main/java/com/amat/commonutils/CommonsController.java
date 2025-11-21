package com.amat.commonutils;


import com.amat.accessmanagement.entity.UserEntity;
import com.amat.accessmanagement.service.UserEnrollmentService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/common-utils")
public class CommonsController {

    @Autowired
    UserEnrollmentService svc;

    @GetMapping("/getLoggedInUserDetails/{employeeId}")
    public ResponseEntity<UserEntity> getUser(@PathVariable Long employeeId, @RequestParam(name = "additionalDetails", defaultValue = "false") boolean additionalDetails, HttpServletRequest servletRequest) {
        return ResponseEntity.ok(svc.getUser(employeeId,additionalDetails));
    }

}
