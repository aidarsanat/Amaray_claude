package com.myworld.amaray.repository;

import com.myworld.amaray.model.Doc;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocRepository extends JpaRepository<Doc, Long> {

    List<Doc> findAllByOrderBySortOrderAscIdAsc();
}
