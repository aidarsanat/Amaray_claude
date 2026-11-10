package com.myworld.amaray.repository;

import com.myworld.amaray.model.RecordImage;
import com.myworld.amaray.model.RefType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecordImageRepository extends JpaRepository<RecordImage, Long> {

    List<RecordImage> findByOwnerTypeAndOwnerIdOrderByPos(RefType ownerType, Long ownerId);

    void deleteByOwnerTypeAndOwnerId(RefType ownerType, Long ownerId);
}
