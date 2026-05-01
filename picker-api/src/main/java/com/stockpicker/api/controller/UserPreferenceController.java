package com.stockpicker.api.controller;

import com.stockpicker.api.service.UserPreferenceService;
import com.stockpicker.common.dto.FilterCriteria;
import com.stockpicker.common.entity.UserPreference;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/preferences")
@RequiredArgsConstructor
public class UserPreferenceController {

    private final UserPreferenceService preferenceService;

    @GetMapping
    public ResponseEntity<List<UserPreference>> getUserPreferences(
            @RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(preferenceService.getUserPreferences(userId));
    }

    @PostMapping
    public ResponseEntity<UserPreference> savePreference(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestBody Map<String, Object> body) {
        String name = (String) body.get("name");

        @SuppressWarnings("unchecked")
        List<FilterCriteria> criteria = ((List<Map<String, Object>>) body.get("criteria"))
            .stream()
            .map(m -> FilterCriteria.builder()
                .field((String) m.get("field"))
                .operator(com.stockpicker.common.enums.FilterOperator.valueOf((String) m.get("operator")))
                .value(m.get("value"))
                .valueTo(m.get("valueTo"))
                .build())
            .toList();

        UserPreference saved = preferenceService.savePreference(userId, name, criteria);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePreference(@PathVariable Long id) {
        preferenceService.deletePreference(id);
        return ResponseEntity.noContent().build();
    }
}
