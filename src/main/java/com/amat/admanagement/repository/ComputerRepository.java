package com.amat.admanagement.repository;

import com.amat.admanagement.dto.ComputersRequest;
import com.amat.admanagement.mapper.ComputerAttributesMapper;
import com.amat.admanagement.mapper.LdapComputerProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.control.PagedResultsDirContextProcessor;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.stereotype.Repository;

import javax.naming.directory.SearchControls;
import java.util.*;
import java.util.regex.Pattern;

@Repository
public class ComputerRepository {


    @Value("${ldap.computer.base-filter:''}")
    String computerBaseFilter;

    @Value("${spring.ldap.base:''}")
    String defaultBase;

    @Autowired
    LdapTemplate ldapTemplate;

    @Autowired
    LdapComputerProperties ldapComputerProperties;


    public Map<String, Object> getComputersPaged(ComputersRequest request) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> page = Collections.emptyList();
        String searchBaseOU =  request.getSearchBaseOU()!=null ? request.getSearchBaseOU() : "";
        searchBaseOU = searchBaseOU.replaceAll(",?" + Pattern.quote(defaultBase) + "$", "");
//        searchBaseOU = searchBaseOU.replaceAll(",DC=.*", "");
        // Combine default + custom attributes
        Set<String> attributes = new LinkedHashSet<>(ldapComputerProperties.getDefaultAttributes());
        if (request.getAddtnlAttributes() != null && !request.getAddtnlAttributes().isEmpty()) {
            attributes.addAll(request.getAddtnlAttributes());
        }
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setReturningAttributes(attributes.toArray(new String[0]));

        // Step 1: Get total count first (to calculate total pages)
        int totalCount = getTotalComputersCount(request.getFilter(),searchBaseOU);
        int totalPages = (int) Math.ceil((double) totalCount / request.getPageSize());

        // Step 2: Handle out-of-bound pages
        if (request.getPageNumber() >= totalPages) {
            response.put("data", Collections.emptyList());
            response.put("pageNumber", request.getPageNumber());
            response.put("pageSize", request.getPageSize());
            response.put("totalPages", totalPages);
            response.put("totalCount", totalCount);
            response.put("hasMore", false);
            return response;
        }

        // Step 3: Paginated fetch
        PagedResultsDirContextProcessor processor = new PagedResultsDirContextProcessor(request.getPageSize());
        int currentPage = 0;
        boolean hasMorePages = false;

        while (true) {
            page = ldapTemplate.search(
                    searchBaseOU,
                    getFilter(request.getFilter()),
                    searchControls,
                    new ComputerAttributesMapper(attributes),
                    processor
            );

            hasMorePages = processor.getCookie() != null;

            if (currentPage == request.getPageNumber() || !hasMorePages) {
                break;
            }

            processor = new PagedResultsDirContextProcessor(request.getPageSize(), processor.getCookie());
            currentPage++;
        }

        response.put("data", page);
        response.put("pageNumber", request.getPageNumber());
        response.put("pageSize", request.getPageSize());
        response.put("totalPages", totalPages);
        response.put("totalCount", totalCount);
        response.put("hasMore", hasMorePages);

        return response;
    }

    private int getTotalComputersCount(String filter,String searchBaseOU) {


        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningAttributes(new String[]{"distinguishedName"});
        List<?> allComputers = ldapTemplate.search(searchBaseOU, getFilter(filter), controls, (AttributesMapper<Object>) attrs -> null);
        return allComputers.size();
    }


    private String getFilter(String filter){
        String baseFilter = computerBaseFilter;

        String combinedFilter;
        if (filter != null && !filter.trim().isEmpty()) {
            // Remove wrapping parentheses if UI already sends a full filter
            String trimmed = filter.trim();
            combinedFilter = trimmed.startsWith("(")
                    ? "(&" + baseFilter + trimmed + ")"
                    : "(&" + baseFilter + "(" + trimmed + "))";
        } else {
            combinedFilter = baseFilter;
        }

        return combinedFilter;
    }

}
