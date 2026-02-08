package com.example.project2.repository;

import com.example.project2.model.AppUser;
import com.example.project2.model.UserSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserSettingsRepository extends JpaRepository<UserSettings, Long> {
    Optional<UserSettings> findByUser(AppUser user);
}
