package com.amat.admanagement.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.core.AttributesMapper;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.util.*;

@Slf4j
public class GroupAttributesMapper implements AttributesMapper<Map<String, Object>> {

    private final Set<String> attributes;

    public GroupAttributesMapper(Set<String> attributes) {
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> mapFromAttributes(Attributes attrs) throws NamingException {
        Map<String, Object> group = new LinkedHashMap<>();

        if (attrs == null) {
            return group;
        }

        // Iterate over all returned attributes
        NamingEnumeration<? extends Attribute> allAttrs = attrs.getAll();
        while (allAttrs.hasMore()) {
            Attribute attr = allAttrs.next();
            String attrId = attr.getID();
            Object value;

            // Handle multi-valued attributes (like 'member')
            if (attr.size() > 1) {
                List<Object> values = new ArrayList<>();
                NamingEnumeration<?> allValues = attr.getAll();
                while (allValues.hasMore()) {
                    values.add(allValues.next());
                }
                value = values;
            } else {
                value = attr.get();
            }

            // Convert objectGUID to readable UUID if present
            if ("objectGUID".equalsIgnoreCase(attrId) && value instanceof byte[]) {
                value = convertObjectGUIDToString((byte[]) value);
            }

            group.put(attrId, value);
        }

        return group;
    }

    private String convertObjectGUIDToString(byte[] objectGUID) {
        // Convert the binary GUID to standard UUID format
        return String.format(
                "%02x%02x%02x%02x-%02x%02x-%02x%02x-%02x%02x-%02x%02x%02x%02x%02x%02x",
                objectGUID[3], objectGUID[2], objectGUID[1], objectGUID[0],
                objectGUID[5], objectGUID[4],
                objectGUID[7], objectGUID[6],
                objectGUID[8], objectGUID[9],
                objectGUID[10], objectGUID[11],
                objectGUID[12], objectGUID[13],
                objectGUID[14], objectGUID[15]
        );
    }
}
