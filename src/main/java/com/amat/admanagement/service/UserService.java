package com.amat.admanagement.service;

import com.amat.admanagement.dto.UsersRequest;
import com.amat.admanagement.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Slf4j
@Service
public class UserService {

    @Autowired
    UserRepository repository;

    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public Map<String, Object> fetchAllObjects(UsersRequest request) {

        log.info("START fetchAllObjects (Users)");

        log.debug(
                "UsersRequest | filter={} | pageNumber={} | pageSize={}",
                request.getFilter(),
                request.getPageNumber(),
                request.getPageSize()
        );

        Map<String, Object> response = repository.getUsersPaged(request);

        log.info(
                "END fetchAllObjects (Users) | responseKeys={}",
                response != null ? response.keySet() : "null"
        );

        return response;
    }
}
