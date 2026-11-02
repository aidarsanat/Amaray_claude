package com.myworld.amaray.repository;

import com.myworld.amaray.model.Tag;
import com.myworld.amaray.model.TagCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TagRepository extends JpaRepository<Tag, Long> {

    List<Tag> findByCategoryOrderBySortOrderAscIdAsc(TagCategory category);
}
