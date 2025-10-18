package com.ldap.myidcustomerservice.controller;


import com.ldap.myidcustomerservice.dto.ComputersRequest;
import com.ldap.myidcustomerservice.service.ComputerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/activeDirectory/computers")
public class ComputersController {

    @Autowired
    ComputerService service;

    @PostMapping("/getComputers")
    public Map<String, Object> getAllbjects(@RequestBody ComputersRequest request) {
        return service.fetchAllObjects(request);
    }



}
