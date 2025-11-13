package com.ldap.myidcustomerservice.repository;

import com.ldap.myidcustomerservice.dto.UsersRequest;
import com.ldap.myidcustomerservice.mapper.LdapUserProperties;
import com.ldap.myidcustomerservice.mapper.UserAttributesMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.control.PagedResultsDirContextProcessor;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.support.LdapContextSource;
import org.springframework.stereotype.Repository;

import javax.naming.PartialResultException;
import javax.naming.directory.SearchControls;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Repository
public class UserRepository {

    @Value("${ldap.user.base-filter:''}")
    String userBaseFilter;

    @Value("${spring.ldap.base:''}")
    String defaultBase;

    @Autowired
    LdapTemplate ldapTemplate;

    @Autowired
    LdapUserProperties ldapUserProperties;



    public Map<String, Object> getUsersPaged(UsersRequest request) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> page = Collections.emptyList();
        String searchBaseOU =  request.getSearchBaseOU()!=null ? request.getSearchBaseOU() : "";
//        searchBaseOU = searchBaseOU.replaceAll(",DC=.*", "");
        searchBaseOU = searchBaseOU.replaceAll(",?" + Pattern.quote(defaultBase) + "$", "");
//        searchBaseOU = searchBaseOU.isEmpty() ? null : searchBaseOU;
        // Combine default + custom attributes
        Set<String> attributes = new LinkedHashSet<>(ldapUserProperties.getDefaultAttributes());
        if (request.getAddtnlAttributes() != null && !request.getAddtnlAttributes().isEmpty()) {
            attributes.addAll(request.getAddtnlAttributes());
        }
        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setReturningAttributes(attributes.toArray(new String[0]));

        // Step 1: Get total count first (to calculate total pages)
        int totalCount = getTotalUserCount(request.getFilter(),searchBaseOU);
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
                    "",
                    getFilter(request.getFilter()),
                    searchControls,
                    new UserAttributesMapper(attributes),
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

    private int getTotalUserCount(String filter,String searchBaseOU) {



        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningAttributes(new String[]{"distinguishedName"});
        List<?> allUsers = ldapTemplate.search("", getFilter(filter), controls, (AttributesMapper<Object>) attrs -> null);

        return allUsers.size();


    }


    private String getFilter(String filter){
        String baseFilter = userBaseFilter;

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
