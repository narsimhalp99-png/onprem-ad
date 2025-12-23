package com.amat.admanagement.repository;

import com.amat.admanagement.dto.UsersRequest;
import com.amat.admanagement.mapper.LdapUserProperties;
import com.amat.admanagement.mapper.UserAttributesMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.control.PagedResultsDirContextProcessor;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.stereotype.Repository;

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

        log.info("START getUsersPaged");

        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> page = Collections.emptyList();

        String searchBaseOU = request.getSearchBaseOU() != null ? request.getSearchBaseOU() : "";
        searchBaseOU = searchBaseOU.replaceAll(",?" + Pattern.quote(defaultBase) + "$", "");

        log.debug(
                "Resolved searchBaseOU | searchBaseOU={} | defaultBase={}",
                searchBaseOU,
                defaultBase
        );

        // Combine default + custom attributes
        Set<String> attributes =
                new LinkedHashSet<>(ldapUserProperties.getDefaultAttributes());

        if (request.getAddtnlAttributes() != null && !request.getAddtnlAttributes().isEmpty()) {
            attributes.addAll(request.getAddtnlAttributes());
        }

        log.debug("LDAP attributes requested | attributes={}", attributes);

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setReturningAttributes(attributes.toArray(new String[0]));

        // Step 1: Get total count first (to calculate total pages)
        int totalCount = getTotalUserCount(request.getFilter(), searchBaseOU);
        int totalPages =
                (int) Math.ceil((double) totalCount / request.getPageSize());

        log.info(
                "Total users count resolved | totalCount={} | totalPages={}",
                totalCount,
                totalPages
        );

        // Step 2: Handle out-of-bound pages
        if (request.getPageNumber() >= totalPages) {

            log.warn(
                    "Requested page out of bounds | pageNumber={} | totalPages={}",
                    request.getPageNumber(),
                    totalPages
            );

            response.put("data", Collections.emptyList());
            response.put("pageNumber", request.getPageNumber());
            response.put("pageSize", request.getPageSize());
            response.put("totalPages", totalPages);
            response.put("totalCount", totalCount);
            response.put("hasMore", false);

            log.info("END getUsersPaged (out-of-bound page)");

            return response;
        }

        // Step 3: Paginated fetch
        PagedResultsDirContextProcessor processor =
                new PagedResultsDirContextProcessor(request.getPageSize());

        int currentPage = 0;
        boolean hasMorePages = false;

        log.debug(
                "Starting LDAP paginated user search | pageSize={} | targetPage={}",
                request.getPageSize(),
                request.getPageNumber()
        );

        while (true) {

            page = ldapTemplate.search(
                    "",
                    getFilter(request.getFilter()),
                    searchControls,
                    new UserAttributesMapper(attributes),
                    processor
            );

            hasMorePages = processor.getCookie() != null;

            log.debug(
                    "LDAP page fetched | currentPage={} | pageSize={} | hasMorePages={}",
                    currentPage,
                    page != null ? page.size() : 0,
                    hasMorePages
            );

            if (currentPage == request.getPageNumber() || !hasMorePages) {
                break;
            }

            processor =
                    new PagedResultsDirContextProcessor(
                            request.getPageSize(),
                            processor.getCookie()
                    );
            currentPage++;
        }

        response.put("data", page);
        response.put("pageNumber", request.getPageNumber());
        response.put("pageSize", request.getPageSize());
        response.put("totalPages", totalPages);
        response.put("totalCount", totalCount);
        response.put("hasMore", hasMorePages);

        log.info(
                "END getUsersPaged | returnedCount={} | pageNumber={}",
                page != null ? page.size() : 0,
                request.getPageNumber()
        );

        return response;
    }

    private int getTotalUserCount(String filter, String searchBaseOU) {

        log.debug(
                "START getTotalUserCount | filter={} | searchBaseOU={}",
                filter,
                searchBaseOU
        );

        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningAttributes(new String[]{"distinguishedName"});

        List<?> allUsers =
                ldapTemplate.search(
                        "",
                        getFilter(filter),
                        controls,
                        (AttributesMapper<Object>) attrs -> null
                );

        int count = allUsers.size();

        log.debug(
                "END getTotalUserCount | count={}",
                count
        );

        return count;
    }

    private String getFilter(String filter) {

        log.debug(
                "START getFilter | uiFilter={}",
                filter
        );

        String baseFilter = userBaseFilter;
        String combinedFilter;

        if (filter != null && !filter.trim().isEmpty()) {
            String trimmed = filter.trim();
            combinedFilter =
                    trimmed.startsWith("(")
                            ? "(&" + baseFilter + trimmed + ")"
                            : "(&" + baseFilter + "(" + trimmed + "))";
        } else {
            combinedFilter = baseFilter;
        }

        log.debug(
                "END getFilter | combinedFilter={}",
                combinedFilter
        );

        return combinedFilter;
    }
}
