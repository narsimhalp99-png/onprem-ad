package com.amat.accessmanagement.service;



import com.amat.accessmanagement.dto.UserPreferencesRequest;
import com.amat.accessmanagement.dto.UserPreferencesResponse;
import com.amat.commonutils.entity.UserPreferences;
import com.amat.accessmanagement.repository.UserEnrollmentRepository;
import com.amat.accessmanagement.repository.UserPreferencesRepository;
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
import java.util.stream.Collectors;

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
            String addFavTiles,
            String removeFavTiles
    ) {
        log.info("START :: Updating user preferences | employeeId={}", employeeId);

        if (employeeId == null || employeeId.isBlank()) {
            log.warn("Invalid employeeId provided");
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "EmployeeId must not be empty"
            );
        }

        try {
            // 1. Validate user exists
            if (!userEnrollmentRepository.findByEmployeeId(employeeId).isPresent()) {
                log.warn("User not found in enrollment | employeeId={}", employeeId);
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid employeeId"
                );
            }

            // 2. Fetch existing preferences or create new
            UserPreferences prefs = repo.findById(employeeId)
                    .orElseGet(() -> {
                        log.info(
                                "No existing preferences found, creating new | employeeId={}",
                                employeeId
                        );
                        return UserPreferences.builder()
                                .employeeId(employeeId)
                                .favTiles("")
                                .build();
                    });

            Set<String> favTiles = new HashSet<>();

            // 3. Load existing tiles
            if (prefs.getFavTiles() != null && !prefs.getFavTiles().isBlank()) {
                favTiles.addAll(
                        Arrays.stream(prefs.getFavTiles().split(";"))
                                .map(String::trim)
                                .filter(tile -> !tile.isEmpty())
                                .toList()
                );
            }

            log.debug("Existing tiles | employeeId={} | tiles={}", employeeId, favTiles);

            // 4. Add new tiles
            if (addFavTiles != null && !addFavTiles.isBlank()) {
                Set<String> tilesToAdd = Arrays.stream(addFavTiles.split(";"))
                        .map(String::trim)
                        .filter(tile -> !tile.isEmpty())
                        .collect(Collectors.toSet());

                favTiles.addAll(tilesToAdd);

                log.info(
                        "Added tiles | employeeId={} | tiles={}",
                        employeeId,
                        tilesToAdd
                );
            }

            // 5. Remove tiles
            if (removeFavTiles != null && !removeFavTiles.isBlank()) {
                Set<String> tilesToRemove = Arrays.stream(removeFavTiles.split(";"))
                        .map(String::trim)
                        .filter(tile -> !tile.isEmpty())
                        .collect(Collectors.toSet());

                favTiles.removeAll(tilesToRemove);

                log.info(
                        "Removed tiles | employeeId={} | tiles={}",
                        employeeId,
                        tilesToRemove
                );
            }

            // 6. Persist
            String updatedFavTiles = String.join(";", favTiles);

            prefs.setFavTiles(updatedFavTiles);
            prefs.setUpdatedAt(LocalDateTime.now());

            repo.save(prefs);

            log.info(
                    "SUCCESS :: Preferences updated | employeeId={} | favTiles={}",
                    employeeId,
                    updatedFavTiles
            );

        } catch (ResponseStatusException ex) {
            log.error(
                    "BUSINESS ERROR :: Preference update failed | employeeId={} | reason={}",
                    employeeId,
                    ex.getReason()
            );
            throw ex;

        } catch (Exception ex) {
            log.error(
                    "SYSTEM ERROR :: Unexpected error updating preferences | employeeId={}",
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

    @Transactional
    public void updateOOODetails(String employeeId, UserPreferencesRequest req) {

        log.info("START :: Updating OOO details | employeeId={}", employeeId);

        if (employeeId == null || employeeId.isBlank()) {
            log.warn("Invalid employeeId provided");
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "EmployeeId must not be empty"
            );
        }

        try {
            //  Validate user exists
            if (!userEnrollmentRepository.findByEmployeeId(employeeId).isPresent()) {
                log.warn("User not found in enrollment | employeeId={}", employeeId);
                throw new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Invalid employeeId"
                );
            }

            //  Fetch or create preferences
            UserPreferences prefs = repo.findById(employeeId)
                    .orElseGet(() -> {
                        log.info("Creating new user_preferences record | employeeId={}", employeeId);
                        return UserPreferences.builder()
                                .employeeId(employeeId)
                                .build();
                    });

            //  Validate OOO fields
            if (req.isOooEnabled()) {

                if (req.getOooStartDate() == null || req.getOooEndDate() == null) {
                    log.warn("OOO enabled but dates missing | employeeId={}", employeeId);
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "OOO start date and end date are required"
                    );
                }

                if (req.getOooEndDate().isBefore(req.getOooStartDate())) {
                    log.warn("Invalid OOO date range | employeeId={}", employeeId);
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "OOO end date must be after start date"
                    );
                }

                if (req.getOooApprover() == null || req.getOooApprover().isBlank()) {
                    log.warn("OOO approver missing | employeeId={}", employeeId);
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "OOO approver is required"
                    );
                }
            }

            //  Update OOO fields
            prefs.setOooEnabled(req.isOooEnabled());
            prefs.setOooStartDate(req.getOooStartDate());
            prefs.setOooEndDate(req.getOooEndDate());
            prefs.setOooApprover(req.getOooApprover());
            prefs.setUpdatedAt(LocalDateTime.now());

            repo.save(prefs);

            log.info(
                    "SUCCESS :: OOO details updated | employeeId={} | enabled={} | start={} | end={} | approver={}",
                    employeeId,
                    req.isOooEnabled(),
                    req.getOooStartDate(),
                    req.getOooEndDate(),
                    req.getOooApprover()
            );

        } catch (ResponseStatusException ex) {
            log.error(
                    "BUSINESS ERROR :: OOO update failed | employeeId={} | reason={}",
                    employeeId,
                    ex.getReason()
            );
            throw ex;

        } catch (Exception ex) {
            log.error(
                    "SYSTEM ERROR :: Unexpected error updating OOO | employeeId={}",
                    employeeId,
                    ex
            );
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update OOO details"
            );
        } finally {
            log.info("END :: Updating OOO details | employeeId={}", employeeId);
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

