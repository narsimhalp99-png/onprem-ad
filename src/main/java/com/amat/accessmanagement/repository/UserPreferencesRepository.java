package com.amat.accessmanagement.repository;

import com.amat.commonutils.entity.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, String> {

}
