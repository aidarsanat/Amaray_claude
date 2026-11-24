package com.myworld.amaray.service;

import com.myworld.amaray.exception.BadRequestException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Параметры списка. Всё, что не search, sortBy, choice, showHidden, direction, page, size, считается фильтром по столбцу:
 * ?race=1, ?status=5, ?lands=LAND:3 (тип нужен, если столбец принимает несколько типов).
 * choice: что выбрано в сортировке с выбором (таблица, tag/ref у пары или id верхней папки).
 * showHidden=true: записи, скрытые сортировкой с выбором, показываются в конце списка.
 */
public record ListQuery(String search, String sortBy, String choice, boolean showHidden, boolean desc,
                        Integer page, Integer size, Map<String, String> filters) {

    private static final Set<String> RESERVED =
            Set.of("search", "sortBy", "choice", "showHidden", "direction", "page", "size");

    public static ListQuery from(Map<String, String> params) {
        Map<String, String> filters = new LinkedHashMap<>();
        for (Map.Entry<String, String> e : params.entrySet()) {
            if (!RESERVED.contains(e.getKey()) && e.getValue() != null && !e.getValue().isBlank()) {
                filters.put(e.getKey(), e.getValue().trim());
            }
        }
        return new ListQuery(
                params.get("search"),
                params.get("sortBy"),
                params.get("choice"),
                "true".equalsIgnoreCase(params.get("showHidden")),
                "desc".equalsIgnoreCase(params.get("direction")),
                parseInt(params.get("page"), "page"),
                parseInt(params.get("size"), "size"),
                filters);
    }

    private static Integer parseInt(String s, String name) {
        if (s == null || s.isBlank()) return null;
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("Параметр '" + name + "' должен быть числом");
        }
    }
}
