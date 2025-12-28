package com.amat.commonutils.utis;


import com.amat.commonutils.entity.UserPreferences;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Slf4j
@Service
public class CommonUtils {

    public boolean isUserOutOfOffice(UserPreferences prefs) {
        if (prefs == null) return false;

        if (!Boolean.TRUE.equals(prefs.getOooEnabled())) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        return prefs.getOooStartDate() != null
                && prefs.getOooEndDate() != null
                && !now.isBefore(prefs.getOooStartDate())
                && !now.isAfter(prefs.getOooEndDate());
    }

}
