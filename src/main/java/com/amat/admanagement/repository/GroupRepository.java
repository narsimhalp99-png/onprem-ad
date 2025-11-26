package com.amat.admanagement.repository;

import com.amat.admanagement.dto.GroupsRequest;
import com.amat.admanagement.dto.ManageGroupRequest;
import com.amat.admanagement.dto.ModifyGroupResponse;
import com.amat.admanagement.mapper.GroupAttributesMapper;
import com.amat.admanagement.mapper.LdapGroupProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.ldap.control.PagedResultsDirContextProcessor;
import org.springframework.ldap.core.AttributesMapper;
import org.springframework.ldap.core.LdapTemplate;
import org.springframework.ldap.support.LdapNameBuilder;
import org.springframework.stereotype.Repository;

import javax.naming.Name;
import javax.naming.NamingEnumeration;
import javax.naming.directory.*;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Repository
public class GroupRepository {

    @Value("${ldap.group.base-filter:(objectClass=group)}")
    String groupBaseFilter;

    @Value("${spring.ldap.base:''}")
    String defaultBase;

    @Autowired
    LdapTemplate ldapTemplate;

    @Autowired
    LdapGroupProperties ldapGroupProperties;

    public Map<String, Object> getGroupsPaged(GroupsRequest request) {
        Map<String, Object> response = new HashMap<>();
        List<Map<String, Object>> page = Collections.emptyList();
        String searchBaseOU = request.getSearchBaseOU() != null ? request.getSearchBaseOU() : "";
        searchBaseOU = searchBaseOU.replaceAll(",?" + Pattern.quote(defaultBase) + "$", "");
//        searchBaseOU = searchBaseOU.replaceAll(",DC=.*", "");
        // Combine default + custom attributes
        Set<String> attributes = new LinkedHashSet<>(ldapGroupProperties.getDefaultAttributes());
        if (request.getAddtnlAttributes() != null && !request.getAddtnlAttributes().isEmpty()) {
            attributes.addAll(request.getAddtnlAttributes());
        }

        // Always include 'member' attribute for recursive resolution
        attributes.add("member");

        SearchControls searchControls = new SearchControls();
        searchControls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        searchControls.setReturningAttributes(attributes.toArray(new String[0]));

        // Step 1: Get total count
        int totalCount = getTotalGroupCount(request.getFilter(), searchBaseOU);
        int totalPages = (int) Math.ceil((double) totalCount / request.getPageSize());

        if (request.getPageNumber() >= totalPages) {
            response.put("data", Collections.emptyList());
            response.put("pageNumber", request.getPageNumber());
            response.put("pageSize", request.getPageSize());
            response.put("totalPages", totalPages);
            response.put("totalCount", totalCount);
            response.put("hasMore", false);
            return response;
        }

        // Step 2: Paginated fetch
        PagedResultsDirContextProcessor processor = new PagedResultsDirContextProcessor(request.getPageSize());
        int currentPage = 0;
        boolean hasMorePages = false;

        while (true) {
            page = ldapTemplate.search(
                    searchBaseOU,
                    getFilter(request.getFilter()),
                    searchControls,
                    new GroupAttributesMapper(attributes),
                    processor
            );

            //  If recursive flag is enabled, expand all nested members
            if (request.isFetchRecursiveMembers()) {
                for (Map<String, Object> group : page) {
                    Object membersObj = group.get("member");
                    Set<String> recursiveMembers = new LinkedHashSet<>();
                    if (membersObj != null) {
                        processMembersObject(membersObj, recursiveMembers);
                    }
                    group.put("member", new ArrayList<>(recursiveMembers));
                }
            }

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

    private int getTotalGroupCount(String filter, String searchBaseOU) {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.SUBTREE_SCOPE);
        controls.setReturningAttributes(new String[]{"distinguishedName"});
        List<?> allGroups = ldapTemplate.search(searchBaseOU, getFilter(filter), controls, (AttributesMapper<Object>) attrs -> null);
        return allGroups.size();
    }

    private String getFilter(String filter) {
        if (filter != null && !filter.trim().isEmpty()) {
            String trimmed = filter.trim();
            return trimmed.startsWith("(")
                    ? "(&" + groupBaseFilter + trimmed + ")"
                    : "(&" + groupBaseFilter + "(" + trimmed + "))";
        }
        return groupBaseFilter;
    }

    // Process the members object (Collection, Array, or single String)
    private void processMembersObject(Object membersObj, Set<String> recursiveMembers) {
        if (membersObj instanceof Collection) {
            for (Object o : (Collection<?>) membersObj) {
                processMemberDn(String.valueOf(o), recursiveMembers);
            }
        } else if (membersObj.getClass().isArray()) {
            for (Object o : (Object[]) membersObj) {
                processMemberDn(String.valueOf(o), recursiveMembers);
            }
        } else {
            processMemberDn(String.valueOf(membersObj), recursiveMembers);
        }
    }

    // Add DN to set, and recursively fetch nested members if it's a group
    private void processMemberDn(String memberDn, Set<String> allMembers) {
//        if (!allMembers.add(memberDn)) {
//            return; // Already processed
//        }

        try {
            // Check if DN is a group
            boolean isGroup = isGroupObject(memberDn);
            if (isGroup) {
                List<String> nestedMembers = getGroupMembers(memberDn);
                for (String nestedDn : nestedMembers) {
                    processMemberDn(nestedDn, allMembers);
                }
            }else {
                allMembers.add(memberDn);
            }
        } catch (Exception e) {
            log.info("Exception is ::{}",e.getLocalizedMessage());
        }
    }

    // Check if DN is a group
    private boolean isGroupObject(String dn) {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.OBJECT_SCOPE);
        controls.setReturningAttributes(new String[]{"objectClass"});
        String base = "DC=mycomp,DC=com";
        String relativeDn = dn.endsWith("," + base)
                ? dn.substring(0, dn.length() - ("," + base).length())
                : dn;
        List<Boolean> results = ldapTemplate.search(
                relativeDn,
                "(objectClass=group)",
                controls,
                (AttributesMapper<Boolean>) attrs -> true
        );

        return !results.isEmpty();
    }

    // Fetch members of a group DN
    private List<String> getGroupMembers(String dn) {
        SearchControls controls = new SearchControls();
        controls.setSearchScope(SearchControls.OBJECT_SCOPE);
        controls.setReturningAttributes(new String[]{"member"});
        String base = "DC=mycomp,DC=com";
        String relativeDn = dn.endsWith("," + base)
                ? dn.substring(0, dn.length() - ("," + base).length())
                : dn;

        List<List<String>> results = ldapTemplate.search(
                relativeDn,
                "(objectClass=group)",
                controls,
                (AttributesMapper<List<String>>) attrs -> {
                    Attribute memberAttr = attrs.get("member");
                    List<String> list = new ArrayList<>();
                    if (memberAttr != null) {
                        NamingEnumeration<?> all = memberAttr.getAll();
                        while (all.hasMore()) {
                            list.add(String.valueOf(all.next()));
                        }
                    }
                    return list;
                }
        );

        return results.isEmpty() ? Collections.emptyList() : results.get(0);
    }

    public ModifyGroupResponse modifyGroupMembers(ManageGroupRequest request) {
        ModifyGroupResponse response = new ModifyGroupResponse();
        List<String> userNotExistList = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        response.setUserNotExist(userNotExistList);
        response.setErrors(errors);

        try {
            // Validate group DN
            if (!isDnExists(request.getGroupDn())) {
                throw new IllegalArgumentException("Group DN does not exist: " + request.getGroupDn());
            }

            // Validate operation
            String operation = request.getOperation().toUpperCase();
            if (!operation.equals("ADD") && !operation.equals("REMOVE")) {
                throw new IllegalArgumentException("Invalid operation: " + operation + ". Use ADD or REMOVE.");
            }

            // Validate user list
            List<String> userDns = request.getUserDns();
            if (userDns == null || userDns.isEmpty()) {
                throw new IllegalArgumentException("User DN list cannot be empty.");
            }

            if (userDns.size() > 100) {
                throw new IllegalArgumentException("Maximum 100 user DNs can be processed at a time.");
            }

            // Prepare group name (without DC parts)
            String grpName = request.getGroupDn().replaceAll(",DC=.*", "");
            Name groupName = LdapNameBuilder.newInstance(grpName).build();

            // Step 1: Separate valid and invalid users
            List<String> validUserDns = new ArrayList<>();
            for (String userDn : userDns) {
                if (isDnExists(userDn)) {
                    validUserDns.add(userDn);
                } else {
                    userNotExistList.add(userDn);
                }
            }

            // Step 2: If there are valid users, process them in ONE LDAP call
            if (!validUserDns.isEmpty()) {
                Attribute memberAttr = new BasicAttribute("member");
                validUserDns.forEach(memberAttr::add);

                ModificationItem modItem;
                if ("ADD".equalsIgnoreCase(operation)) {
                    modItem = new ModificationItem(DirContext.ADD_ATTRIBUTE, memberAttr);
                } else {
                    modItem = new ModificationItem(DirContext.REMOVE_ATTRIBUTE, memberAttr);
                }

                try {
                    ldapTemplate.modifyAttributes(groupName, new ModificationItem[]{modItem});
                } catch (Exception e) {
                    errors.add("LDAP operation failed: " + e.getLocalizedMessage());
                }
            }

            // Step 3: Prepare final response
            if (errors.isEmpty() && userNotExistList.isEmpty()) {
                response.setStatusCode("SUCCESS");
                response.setMessage("All members processed successfully (" + operation + ")");
            } else if (!validUserDns.isEmpty()) {
                response.setStatusCode("PARTIAL_SUCCESS");
                response.setMessage("Some members failed or were invalid during " + operation + " operation.");
            } else {
                response.setStatusCode("FAILED");
                response.setMessage("No valid members to process. All user DNs were invalid.");
            }

        } catch (Exception e) {
            response.getErrors().add(e.getLocalizedMessage());
            response.setStatusCode("FAILED");
            response.setMessage("Group modification failed: " + e.getLocalizedMessage());
        }

        return response;
    }


    private boolean isDnExists(String dn) {
        try {
            String lookUpDn = dn.replaceAll(",DC=.*", "");
            ldapTemplate.lookup(lookUpDn);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

}
