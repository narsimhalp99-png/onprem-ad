package com.amat.admanagement.controller;

import com.amat.admanagement.dto.UsersRequest;
import com.amat.admanagement.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ad-management/users")
public class UserController {

    @Autowired
    UserService service;

    @PostMapping
    public Map<String, Object> getAllbjects(@RequestBody UsersRequest request) {

        log.info("API HIT: getAllObjects (Users)");

        log.debug(
                "UsersRequest received | filter={} | pageNumber={} | pageSize={}",
                request.getFilter(),
                request.getPageNumber(),
                request.getPageSize()
        );

        Map<String, Object> response = service.fetchAllObjects(request);

        log.info(
                "Users fetched successfully | resultKeys={}",
                response != null ? response.keySet() : "null"
        );

        return response;
    }

}
