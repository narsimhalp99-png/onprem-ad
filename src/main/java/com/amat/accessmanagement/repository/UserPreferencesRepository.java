package com.amat.accessmanagement.repository;

import com.amat.accessmanagement.entity.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, String> {

}
