package com.amat.admanagement.controller;

import com.amat.admanagement.dto.UsersRequest;
import com.amat.admanagement.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/myidcustomapi/ad-management/users")
public class UserController {

    @Autowired
    UserService service;

    @PostMapping
    public Map<String, Object> getAllbjects(@RequestBody UsersRequest request) {
        return service.fetchAllObjects(request);
    }


}

