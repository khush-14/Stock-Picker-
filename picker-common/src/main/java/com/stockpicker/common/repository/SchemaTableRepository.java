package com.stockpicker.common.repository;

import com.stockpicker.common.entity.SchemaTable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SchemaTableRepository extends JpaRepository<SchemaTable, Long> {

    Optional<SchemaTable> findByFieldName(String fieldName);

    List<SchemaTable> findByFilterableTrue();

    List<SchemaTable> findBySortableTrue();

    List<SchemaTable> findBySourceEntity(String sourceEntity);
}
