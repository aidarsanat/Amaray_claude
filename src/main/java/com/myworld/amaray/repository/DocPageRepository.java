package com.myworld.amaray.repository;

import com.myworld.amaray.model.DocPage;
import com.myworld.amaray.model.RefType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocPageRepository extends JpaRepository<DocPage, Long> {

    List<DocPage> findByOwnerTypeIsNullOrderBySortOrderAscIdAsc();

    List<DocPage> findByOwnerTypeAndOwnerIdOrderBySortOrderAscIdAsc(RefType ownerType, Long ownerId);

    void deleteByOwnerTypeAndOwnerId(RefType ownerType, Long ownerId);
}
