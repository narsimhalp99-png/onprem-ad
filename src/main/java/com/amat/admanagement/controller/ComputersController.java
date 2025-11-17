package com.amat.admanagement.controller;


import com.amat.admanagement.dto.ComputersRequest;
import com.amat.admanagement.service.ComputerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/myidcustomapi/ad-management")
public class ComputersController {

    @Autowired
    ComputerService service;

    @PostMapping("/computers")
    public Map<String, Object> getAllbjects(@RequestBody ComputersRequest request) {
        return service.fetchAllObjects(request);
    }



}
