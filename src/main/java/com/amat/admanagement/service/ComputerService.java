package com.amat.admanagement.service;

import com.amat.admanagement.dto.ComputersRequest;
import com.amat.admanagement.repository.ComputerRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class ComputerService {

    @Autowired
    ComputerRepository repository;


    public Map<String, Object> fetchAllObjects(ComputersRequest request) {
        return repository.getComputersPaged(request);
    }
}
