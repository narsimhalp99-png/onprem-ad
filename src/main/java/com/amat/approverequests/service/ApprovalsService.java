package com.amat.approverequests.service;


import com.amat.accessmanagement.service.RoleService;
import com.amat.admanagement.service.GroupsService;
import com.amat.admanagement.service.UserService;
import com.amat.approverequests.dto.getApprovalsDTO;
import com.amat.approverequests.repository.ServerElevationRequestRepository;
import com.amat.serverelevation.entity.ApprovalDetails;
import com.amat.serverelevation.entity.ServerElevationRequest;
import com.amat.serverelevation.repository.ApprovalDetailsRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class ApprovalsService {


    @Autowired
    ServerElevationRequestRepository serverElevationRequestRepository;

    @Autowired
    RoleService roleService;

    @Autowired
    ApprovalDetailsRepository approvalRepo;

    public Page<ServerElevationRequest> getRequests(getApprovalsDTO approvalsDTO,
                                                    String loggedInUser,
                                                    boolean isSelf,
                                                    Pageable pageable) {

        if (!isSelf) {
            boolean isAdmin = roleService.hasRole(loggedInUser, "ServerElevation-Administrator");
            if (!isAdmin) {
                throw new AccessDeniedException("Access Denied");
            }
        }

        Specification<ServerElevationRequest> spec =
                ServerElevationRequestSpecification.applyFilters(approvalsDTO, loggedInUser, isSelf);

        Page<ServerElevationRequest> pageData = serverElevationRequestRepository.findAll(spec, pageable);

        // 🔽 Sorting by approval date if approval exists
        List<String> requestIds = pageData.getContent().stream()
                .map(ServerElevationRequest::getRequestId)
                .toList();

        List<ApprovalDetails> approvals =
                approvalRepo.findByRequestIdInOrderByApprovalRequestDateDesc(requestIds);

        // No mapping required — just sorted correctly if present
        return pageData;
    }


}
