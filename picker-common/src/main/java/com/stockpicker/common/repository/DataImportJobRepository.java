package com.stockpicker.common.repository;

import com.stockpicker.common.entity.DataImportJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.UUID;

@Repository
public interface DataImportJobRepository extends JpaRepository<DataImportJob, UUID> {
}
