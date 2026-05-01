package com.stockpicker.api.controller;

import com.stockpicker.common.entity.SchemaTable;
import com.stockpicker.common.repository.SchemaTableRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/v1/schema")
@RequiredArgsConstructor
public class SchemaController {

    private final SchemaTableRepository schemaTableRepository;

    @GetMapping("/fields")
    public ResponseEntity<List<SchemaTable>> getFilterableFields() {
        return ResponseEntity.ok(schemaTableRepository.findByFilterableTrue());
    }

    @GetMapping("/fields/sortable")
    public ResponseEntity<List<SchemaTable>> getSortableFields() {
        return ResponseEntity.ok(schemaTableRepository.findBySortableTrue());
    }

    @GetMapping("/fields/{entity}")
    public ResponseEntity<List<SchemaTable>> getFieldsByEntity(@PathVariable String entity) {
        return ResponseEntity.ok(schemaTableRepository.findBySourceEntity(entity));
    }
}
