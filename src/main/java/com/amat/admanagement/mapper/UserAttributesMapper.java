package com.amat.admanagement.mapper;

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
        log.debug("UserAttributesMapper initialized | attributes={}", attributes);
    }

    @Override
    public Map<String, Object> mapFromAttributes(Attributes attrs)
            throws javax.naming.NamingException {

        log.debug("START mapFromAttributes (User)");

        Map<String, Object> user = new LinkedHashMap<>();

        for (String attrName : attributes) {

            Attribute attr = attrs.get(attrName);

            if (attr != null) {

                log.debug("Processing LDAP user attribute | attributeName={}", attrName);

                // Special handling for binary attribute: objectGUID
                if ("objectGUID".equalsIgnoreCase(attrName)) {
                    try {
                        Object raw = attr.get(); // returns Object
                        if (raw instanceof byte[] guidBytes) {

                            String guid = bytesToGUID(guidBytes).toLowerCase(Locale.ROOT);
                            user.put("objectGUID", guid);

                            log.debug("objectGUID parsed successfully | guid={}", guid);

                        } else {
                            log.warn(
                                    "objectGUID returned as unexpected type | type={}",
                                    raw != null ? raw.getClass().getName() : "null"
                            );
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

                    log.debug(
                            "Multi-valued attribute processed | attributeName={} | valuesCount={}",
                            attrName,
                            values.size()
                    );
                }

                // Handle single-valued attributes
                else {
                    Object value = attr.get();
                    user.put(attrName, value != null ? value.toString() : null);

                    log.debug(
                            "Single-valued attribute processed | attributeName={} | valuePresent={}",
                            attrName,
                            value != null
                    );
                }
            }

            // Handle derived "enabled" flag from userAccountControl
            else if ("enabled".equalsIgnoreCase(attrName) && attrs.get("userAccountControl") != null) {
                try {
                    int uac = Integer.parseInt(attrs.get("userAccountControl").get().toString());
                    boolean enabled = (uac & 2) == 0; // bit 2 unset => account enabled
                    user.put("enabled", enabled);

                    log.debug(
                            "Derived enabled flag from userAccountControl | enabled={}",
                            enabled
                    );

                } catch (NumberFormatException e) {
                    log.warn(
                            "Failed to parse userAccountControl for enabled flag",
                            e
                    );
                    user.put("enabled", null);
                }
            }

            // Attribute missing
            else {
                user.put(attrName, null);

                log.debug(
                        "LDAP attribute missing, setting null | attributeName={}",
                        attrName
                );
            }
        }

        log.debug(
                "END mapFromAttributes (User) | attributesMapped={}",
                user.keySet()
        );

        return user;
    }

    /**
     * Convert AD binary objectGUID (little-endian) into standard UUID string format
     */
    private String bytesToGUID(byte[] bytes) {

        log.debug("START bytesToGUID");

        if (bytes == null || bytes.length != 16) {
            log.warn("Invalid objectGUID byte array | length={}", bytes != null ? bytes.length : 0);
            return null;
        }

        String guid = String.format(
                "%02X%02X%02X%02X-%02X%02X-%02X%02X-%02X%02X-%02X%02X%02X%02X%02X%02X",
                bytes[3] & 0xFF, bytes[2] & 0xFF, bytes[1] & 0xFF, bytes[0] & 0xFF,
                bytes[5] & 0xFF, bytes[4] & 0xFF,
                bytes[7] & 0xFF, bytes[6] & 0xFF,
                bytes[8] & 0xFF, bytes[9] & 0xFF,
                bytes[10] & 0xFF, bytes[11] & 0xFF,
                bytes[12] & 0xFF, bytes[13] & 0xFF,
                bytes[14] & 0xFF, bytes[15] & 0xFF
        );

        log.debug("END bytesToGUID | guid={}", guid);

        return guid;
    }
}
