package com.ldap.myidcustomerservice.mapper;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ldap.core.AttributesMapper;

import javax.naming.NamingEnumeration;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import java.util.*;

@Slf4j
public class UserAttributesMapper implements AttributesMapper<Map<String, Object>> {

    private final Set<String> attributes;

    public UserAttributesMapper(Set<String> attributes) {
        this.attributes = attributes;
    }

    @Override
    public Map<String, Object> mapFromAttributes(Attributes attrs)
            throws javax.naming.NamingException {

        Map<String, Object> user = new LinkedHashMap<>();

        for (String attrName : attributes) {
            Attribute attr = attrs.get(attrName);

            if (attr != null) {

//                 Special handling for binary attribute: objectGUID
                if ("objectGUID".equalsIgnoreCase(attrName)) {
                    try {
                        Object raw = attr.get(); // returns Object
                        if (raw instanceof byte[] guidBytes) {
                            user.put("objectGUID", bytesToGUID(guidBytes).toLowerCase(Locale.ROOT));
                        } else {
                            log.warn("objectGUID returned as unexpected type: {}", raw.getClass());
                            user.put("objectGUID", null);
                        }

                    } catch (Exception e) {
                        log.warn("Failed to parse objectGUID", e);
                        user.put("objectGUID", null);
                    }
                }

                // Handle multi-valued attributes (like proxyAddresses, memberOf, etc.)
                 else if (attr.size() > 1) {
                    List<String> values = new ArrayList<>();
                    NamingEnumeration<?> enumeration = attr.getAll();
                    while (enumeration.hasMore()) {
                        Object value = enumeration.next();
                        values.add(value != null ? value.toString() : null);
                    }
                    user.put(attrName, values);
                }

                // Handle single-valued attributes
                else {
                    Object value = attr.get();
                    user.put(attrName, value != null ? value.toString() : null);
                }
            }

            // Handle derived "enabled" flag from userAccountControl
            else if ("enabled".equalsIgnoreCase(attrName) && attrs.get("userAccountControl") != null) {
                try {
                    int uac = Integer.parseInt(attrs.get("userAccountControl").get().toString());
                    boolean enabled = (uac & 2) == 0; // bit 2 unset => account enabled
                    user.put("enabled", enabled);
                } catch (NumberFormatException e) {
                    user.put("enabled", null);
                }
            }

            // Attribute missing
            else {
                user.put(attrName, null);
            }
        }

        return user;
    }

    /**
     * Convert AD binary objectGUID (little-endian) into standard UUID string format
     */
    private String bytesToGUID(byte[] bytes) {
        if (bytes == null || bytes.length != 16) {
            return null;
        }

        return String.format("%02X%02X%02X%02X-%02X%02X-%02X%02X-%02X%02X-%02X%02X%02X%02X%02X%02X",
                bytes[3] & 0xFF, bytes[2] & 0xFF, bytes[1] & 0xFF, bytes[0] & 0xFF,
                bytes[5] & 0xFF, bytes[4] & 0xFF,
                bytes[7] & 0xFF, bytes[6] & 0xFF,
                bytes[8] & 0xFF, bytes[9] & 0xFF,
                bytes[10] & 0xFF, bytes[11] & 0xFF, bytes[12] & 0xFF, bytes[13] & 0xFF, bytes[14] & 0xFF, bytes[15] & 0xFF
        );
    }
}
