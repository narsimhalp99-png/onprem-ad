package com.amat.commonutil.repository;

import com.amat.commonutil.entity.UserPreferences;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserPreferencesRepository extends JpaRepository<UserPreferences, String> {

}
