package com.amat.admanagement.service;

import com.amat.admanagement.dto.UsersRequest;
import com.amat.admanagement.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class UserService {

    @Autowired
    UserRepository repository;


    public UserService(UserRepository repository) {
        this.repository = repository;
    }

    public Map<String, Object> fetchAllObjects(UsersRequest request) {
        return repository.getUsersPaged(request);
    }

}

