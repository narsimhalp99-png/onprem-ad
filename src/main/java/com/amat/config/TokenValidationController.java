package com.amat.config;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class TokenValidationController {

    @GetMapping("/api/token/validate")
    public Map<String, Object> validateToken(@AuthenticationPrincipal Jwt jwt) {



        return Map.of(
                "tokenValid", true,
                "subject", jwt.getSubject(),
                "issuedAt", jwt.getIssuedAt(),
                "expiresAt", jwt.getExpiresAt(),
                "issuer", jwt.getIssuer(),
                "claims", jwt.getClaims()
        );
    }
}
