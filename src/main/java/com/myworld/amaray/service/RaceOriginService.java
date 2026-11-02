package com.myworld.amaray.service;

import com.myworld.amaray.dto.common.RefDto;
import com.myworld.amaray.dto.record.RaceOriginDto;
import com.myworld.amaray.model.BaseEntity;
import com.myworld.amaray.model.Link;
import com.myworld.amaray.model.RefType;
import com.myworld.amaray.model.Tag;
import com.myworld.amaray.repository.LinkRepository;
import com.myworld.amaray.repository.RecordStore;
import com.myworld.amaray.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/** Происхождение расы собирается по цепочке: у каждой расы хранится только родитель и причина. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RaceOriginService {

    private final LinkRepository links;
    private final RecordStore store;
    private final TagRepository tags;

    public RaceOriginDto describe(Long raceId) {
        store.require(RefType.RACE, raceId);

        List<BaseEntity> line = new ArrayList<>(); // сама раса, родитель, прародитель...
        List<Tag> causes = new ArrayList<>();      // причина изменения для каждой из них
        RefDto parentDto = null;
        RefDto causeDto = null;
        Set<Long> seen = new HashSet<>();
        Long cur = raceId;

        while (cur != null && seen.add(cur)) {
            BaseEntity race = store.find(RefType.RACE, cur);
            if (race == null) break;
            Link origin = links.findByOwnerTypeAndOwnerIdAndColOrderByPos(RefType.RACE, cur, "origin")
                    .stream().findFirst().orElse(null);
            Tag cause = origin != null && origin.getTargetId() != null
                    ? tags.findById(origin.getTargetId()).orElse(null) : null;
            Long parentId = origin != null && origin.getRefType() == RefType.RACE ? origin.getRefId() : null;

            line.add(race);
            causes.add(cause);
            if (line.size() == 1) {
                if (cause != null) {
                    causeDto = new RefDto(cause.getId(), cause.getName(), RefType.TAG, TagService.effectiveColor(cause));
                }
                BaseEntity parent = parentId == null ? null : store.find(RefType.RACE, parentId);
                if (parent != null) parentDto = new RefDto(parent.getId(), parent.displayName(), RefType.RACE, null);
            }
            cur = parentId;
        }

        String chain = line.stream().map(BaseEntity::displayName).collect(Collectors.joining(" → "));
        StringBuilder formula = new StringBuilder(line.get(line.size() - 1).displayName());
        for (int i = line.size() - 2; i >= 0; i--) {
            if (causes.get(i) != null) formula.append(" + ").append(causes.get(i).getName());
        }
        return new RaceOriginDto(parentDto, causeDto, formula.toString(), chain);
    }
}
