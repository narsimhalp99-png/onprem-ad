package com.amat.admanagement.service;

import com.amat.admanagement.dto.ComputersRequest;
import com.amat.admanagement.repository.ComputerRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class ComputerService {

    @Autowired
    ComputerRepository repository;

    public Map<String, Object> fetchAllObjects(ComputersRequest request) {

        log.info("START fetchAllObjects (Computers)");

        log.debug(
                "ComputersRequest | filter={} | pageNumber={} | pageSize={}",
                request.getFilter(),
                request.getPageNumber(),
                request.getPageSize()
        );

        Map<String, Object> response = repository.getComputersPaged(request);

        log.info(
                "END fetchAllObjects (Computers) | responseKeys={}",
                response != null ? response.keySet() : "null"
        );

        return response;
    }
}
