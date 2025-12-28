package com.amat.accessmanagement.service;



import com.amat.accessmanagement.dto.UserPreferencesResponse;
import com.amat.accessmanagement.entity.UserPreferences;
import com.amat.accessmanagement.repository.UserEnrollmentRepository;
import com.amat.accessmanagement.repository.UserPreferencesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.Optional;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class UserPreferencesService {

    @Autowired
    UserPreferencesRepository repo;

    @Autowired
    UserEnrollmentRepository userEnrollmentRepository;

    @Transactional
    public void createOrUpdatePreferences(
            String employeeId,
            List<String> newTiles
    ) {
        log.info("START :: Updating user preferences | employeeId={}", employeeId);

        if (employeeId == null || employeeId.isBlank()) {
            log.warn("Invalid employeeId provided");
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "EmployeeId must not be empty"
            );
        }

        if (newTiles == null || newTiles.isEmpty()) {
            log.warn("No new tiles provided | employeeId={}", employeeId);
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "At least one tile must be provided"
            );
        }

        try {
            UserPreferences prefs = repo.findById(employeeId)
                    .orElseGet(() -> {
                        log.info(
                                "No existing preferences found, creating new record | employeeId={}",
                                employeeId
                        );
                        return UserPreferences.builder()
                                .employeeId(employeeId)
                                .favTiles("")
                                .build();
                    });

            Set<String> existingTiles = new HashSet<>();

            if (prefs.getFavTiles() != null && !prefs.getFavTiles().isBlank()) {
                existingTiles.addAll(
                        Arrays.stream(prefs.getFavTiles().split(";"))
                                .map(String::trim)
                                .filter(tile -> !tile.isEmpty())
                                .toList()
                );
            }

            existingTiles.addAll(
                    newTiles.stream()
                            .map(String::trim)
                            .filter(tile -> !tile.isEmpty())
                            .toList()
            );

            String updatedFavTiles = String.join(";", existingTiles);

            prefs.setFavTiles(updatedFavTiles);
            prefs.setUpdatedAt(LocalDateTime.now());

            repo.save(prefs);

            log.info(
                    "SUCCESS :: Preferences updated | employeeId={}, favTiles={}",
                    employeeId,
                    updatedFavTiles
            );

        } catch (ResponseStatusException ex) {
            // already meaningful
            log.error(
                    "BUSINESS ERROR :: Failed to update preferences | employeeId={} | reason={}",
                    employeeId,
                    ex.getReason()
            );
            throw ex;

        } catch (Exception ex) {
            log.error(
                    "SYSTEM ERROR :: Unexpected error while updating preferences | employeeId={}",
                    employeeId,
                    ex
            );
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update user preferences"
            );
        } finally {
            log.info("END :: Updating user preferences | employeeId={}", employeeId);
        }
    }


    @Transactional(readOnly = true)
    public Object getPreferences(String employeeId) {

        log.info("Fetching preferences for employeeId={}", employeeId);

        boolean userExists = userEnrollmentRepository
                .findByEmployeeId(employeeId)
                .isPresent();

        if (!userExists) {
            log.warn("User not found in enrollment repository, employeeId={}", employeeId);

            return new UserPreferencesResponse(
                    employeeId,
                    List.of(),
                    "User not found"
            );
        }


        Optional<UserPreferences> optionalPrefs = repo.findById(employeeId);

        if (optionalPrefs.isEmpty()) {
            log.info("No preferences found for employeeId={}", employeeId);

            return new UserPreferencesResponse(
                    employeeId,
                    List.of(),
                    "User preferences not found"
            );
        }

        UserPreferences prefs = optionalPrefs.get();

        List<String> tiles =
                (prefs.getFavTiles() == null || prefs.getFavTiles().isBlank())
                        ? List.of()
                        : Arrays.asList(prefs.getFavTiles().split(";"));

        log.info(
                "Preferences fetched successfully for employeeId={}, tiles={}",
                employeeId,
                tiles
        );

        return new UserPreferencesResponse(
                employeeId,
                tiles,
                "User preferences retrieved successfully"
        );
    }


}

