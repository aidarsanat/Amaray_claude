package com.myworld.amaray.service;

import com.myworld.amaray.dto.common.RefDto;
import com.myworld.amaray.exception.BadRequestException;
import com.myworld.amaray.model.BaseEntity;
import com.myworld.amaray.model.RefType;
import com.myworld.amaray.repository.LinkRepository;
import com.myworld.amaray.repository.RecordStore;
import com.myworld.amaray.schema.ColumnKind;
import com.myworld.amaray.schema.ColumnSpec;
import com.myworld.amaray.schema.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/** Подсказки по записям для окна выбора. */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RefService {

    private static final int LIMIT_PER_TYPE = 30;

    private final RecordStore store;
    private final LinkRepository links;
    private final TagService tagService;

    /** typeTagId сужает ответ до записей этого типа (или вложенных в него), если у таблицы есть столбец "type". */
    public List<RefDto> suggest(List<RefType> types, String q, Long typeTagId) {
        String needle = q == null ? "" : q.trim().toLowerCase();
        List<RefDto> out = new ArrayList<>();

        for (RefType t : types) {
            if (!t.isRecord()) throw new BadRequestException("Теги ищутся через /api/tags/suggest");

            Set<Long> byType = null;
            Optional<ColumnSpec> typeCol = Schema.find(t, "type");
            if (typeTagId != null && typeCol.isPresent() && typeCol.get().kind() == ColumnKind.TAGS) {
                byType = new HashSet<>(links.ownersByTarget(t, "type", RefType.TAG, tagService.descendantIds(typeTagId)));
            }

            List<BaseEntity> found = new ArrayList<>();
            for (BaseEntity e : store.all(t)) {
                if (byType != null && !byType.contains(e.getId())) continue;
                if (!e.searchText().toLowerCase().contains(needle)) continue;
                found.add(e);
            }
            // сначала те, что начинаются с набранного
            found.sort(Comparator
                    .comparing((BaseEntity e) -> !e.displayName().toLowerCase().startsWith(needle))
                    .thenComparing(e -> e.displayName().toLowerCase()));

            found.stream().limit(LIMIT_PER_TYPE)
                    .forEach(e -> out.add(new RefDto(e.getId(), e.displayName(), t, null)));
        }
        return out;
    }
}
