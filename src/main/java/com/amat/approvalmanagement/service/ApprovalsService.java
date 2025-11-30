package com.amat.approvalmanagement.service;



import com.amat.accessmanagement.service.RoleService;
import com.amat.approvalmanagement.dto.ApprovalDetailsFilterDTO;
import com.amat.approvalmanagement.dto.ApprovalDetailsSearchDTO;
import com.amat.approvalmanagement.repository.ApprovalDetailsFilterRepository;
import com.amat.serverelevation.entity.ApprovalDetails;
import com.amat.serverelevation.entity.ServerElevationRequest;
import com.amat.serverelevation.repository.ApprovalDetailsRepository;
import com.amat.serverelevation.service.ServerElevationRequestSpecification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;


import java.util.Map;


@Slf4j
@Service
public class ApprovalsService {


    @Autowired
    ApprovalDetailsFilterRepository approvalRepo;

    @Autowired
    RoleService roleService;


    public Object getApprovalDetails(ApprovalDetailsFilterDTO filter, int page, int size, String loggedInUser, boolean isSelf) {

        if (!isSelf) {
            boolean isAdmin = roleService.hasRole(loggedInUser, "ServerElevation-Administrator");
            if (!isAdmin) {
                return ResponseEntity
                        .status(HttpStatus.FORBIDDEN)
                        .body(Map.of(
                                "status", "FAILED",
                                "message", "Access Denied: You are not authorized to view this information"
                        ));
            }
        }

        if(filter.getRequestorEmpId()!=null && !filter.getRequestorEmpId().isBlank()){
            loggedInUser = filter.getRequestorEmpId();
            isSelf=true;
        }


        String validSortField = (filter.getSortField() != null && !filter.getSortField() .isEmpty())
                ? filter.getSortField()
                : "approvalRequestDate";

        Sort.Direction direction = (filter.getSortDirection()  != null && filter.getSortDirection().equalsIgnoreCase("desc"))
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;

        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, validSortField));


        Specification<ApprovalDetails> spec = ApprovalDetailsRequestSpecification.applyFilters(filter, loggedInUser, isSelf);


        return approvalRepo.findAll(spec, pageable);
    }



}
