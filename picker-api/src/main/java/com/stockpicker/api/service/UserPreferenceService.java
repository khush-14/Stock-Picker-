package com.stockpicker.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stockpicker.common.dto.FilterCriteria;
import com.stockpicker.common.entity.UserPreference;
import com.stockpicker.common.repository.UserPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserPreferenceService {

    private final UserPreferenceRepository preferenceRepository;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public List<UserPreference> getUserPreferences(UUID userId) {
        return preferenceRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional
    public UserPreference savePreference(UUID userId, String name, List<FilterCriteria> criteria) {
        String json;
        try {
            json = objectMapper.writeValueAsString(criteria);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize filter criteria", e);
        }

        UserPreference preference = UserPreference.builder()
            .userId(userId)
            .preferenceName(name)
            .filterCriteria(json)
            .build();
        return preferenceRepository.save(preference);
    }

    @Transactional
    public void deletePreference(Long id) {
        if (!preferenceRepository.existsById(id)) {
            throw new NoSuchElementException("Preference not found: " + id);
        }
        preferenceRepository.deleteById(id);
    }
}
