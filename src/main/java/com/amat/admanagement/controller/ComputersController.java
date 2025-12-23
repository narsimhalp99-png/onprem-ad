package com.amat.admanagement.controller;

import com.amat.admanagement.dto.ComputersRequest;
import com.amat.admanagement.service.ComputerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/ad-management/computers")
public class ComputersController {

    @Autowired
    ComputerService service;

    @PostMapping
    public Map<String, Object> getAllbjects(@RequestBody ComputersRequest request) {

        log.info("API HIT: getAllObjects (Computers)");

        log.debug(
                "ComputersRequest received | filter={} | pageNumber={} | pageSize={}",
                request.getFilter(),
                request.getPageNumber(),
                request.getPageSize()
        );

        Map<String, Object> response = service.fetchAllObjects(request);

        log.info(
                "Computers fetched successfully | resultKeys={}",
                response != null ? response.keySet() : "null"
        );

        return response;
    }

}
