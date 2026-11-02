package com.myworld.amaray.repository;

import com.myworld.amaray.model.CellText;
import com.myworld.amaray.model.RefType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CellTextRepository extends JpaRepository<CellText, Long> {

    List<CellText> findByOwnerTypeAndOwnerIdIn(RefType ownerType, Collection<Long> ownerIds);

    Optional<CellText> findByOwnerTypeAndOwnerIdAndCol(RefType ownerType, Long ownerId, String col);

    void deleteByOwnerTypeAndOwnerId(RefType ownerType, Long ownerId);
}
