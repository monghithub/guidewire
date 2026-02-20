package com.guidewire.billing.repository;

import com.guidewire.billing.entity.Invoice;
import com.guidewire.billing.entity.InvoiceStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, UUID> {

    List<Invoice> findByPolicyId(UUID policyId);

    List<Invoice> findByCustomerId(UUID customerId);

    List<Invoice> findByStatus(InvoiceStatus status);

    @Query("""
            SELECT i FROM Invoice i
            WHERE (:policyId IS NULL OR i.policyId = :policyId)
              AND (:customerId IS NULL OR i.customerId = :customerId)
              AND (:status IS NULL OR i.status = :status)
            ORDER BY i.createdAt DESC
            """)
    Page<Invoice> findWithFilters(
            @Param("policyId") UUID policyId,
            @Param("customerId") UUID customerId,
            @Param("status") InvoiceStatus status,
            Pageable pageable
    );
}
