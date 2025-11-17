package com.ldap.myidcustomerservice.controller;

import com.ldap.myidcustomerservice.dto.UsersRequest;
import com.ldap.myidcustomerservice.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/myidcustomapi/ad-management")
public class UserController {

    @Autowired
    UserService service;

    @PostMapping("/users")
    public Map<String, Object> getAllbjects(@RequestBody UsersRequest request) {
        return service.fetchAllObjects(request);
    }


}

