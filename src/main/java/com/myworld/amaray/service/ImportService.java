package com.myworld.amaray.service;

import com.myworld.amaray.dto.common.CellDto;
import com.myworld.amaray.dto.record.RecordRequest;
import com.myworld.amaray.model.RefType;
import com.myworld.amaray.model.SyncState;
import com.myworld.amaray.repository.RecordStore;
import com.myworld.amaray.schema.ColumnKind;
import com.myworld.amaray.schema.ColumnSpec;
import com.myworld.amaray.schema.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Импорт CSV. Заголовки: имена полей (name, description, firstName...) и имена столбцов со связями.
 * Значения связей попадают в свободный текст ячейки (как «фракция, похожая на викингов»),
 * потом их можно заменить плашками. Записи получают IMPORTED.
 * Класс намеренно без @Transactional: каждая строка создаётся в своей транзакции, ошибка одной строки не отменяет остальные.
 */
@Service
@RequiredArgsConstructor
public class ImportService {

    public record Result(int created, List<String> skipped, List<String> ignoredColumns) {}

    private final RecordService records;
    private final RecordStore store;

    public Result importCsv(RefType type, byte[] bytes) throws IOException {
        String content = new String(bytes, StandardCharsets.UTF_8);
        if (content.startsWith("\uFEFF")) content = content.substring(1);

        int nl = content.indexOf('\n');
        String firstLine = nl < 0 ? content : content.substring(0, nl);
        char delim = count(firstLine, ';') > count(firstLine, ',') ? ';' : ',';

        List<List<String>> rows = parse(content, delim);
        if (rows.isEmpty()) return new Result(0, List.of("Файл пуст"), List.of());

        Set<String> scalars = new HashSet<>(store.newInstance(type).toFields().keySet());
        scalars.remove("appearance");
        Set<String> textColumns = new HashSet<>();
        for (ColumnSpec c : Schema.columns(type)) {
            if (c.kind() != ColumnKind.MIRROR) textColumns.add(c.field());
        }

        List<String> headers = rows.get(0).stream().map(String::trim).toList();
        List<String> ignored = headers.stream()
                .filter(h -> !h.isEmpty() && !scalars.contains(h) && !textColumns.contains(h)).toList();

        int created = 0;
        List<String> skipped = new ArrayList<>();
        for (int r = 1; r < rows.size(); r++) {
            List<String> row = rows.get(r);
            Map<String, Object> fields = new LinkedHashMap<>();
            Map<String, CellDto> cells = new LinkedHashMap<>();
            for (int i = 0; i < headers.size() && i < row.size(); i++) {
                String h = headers.get(i);
                String v = row.get(i);
                if (scalars.contains(h)) fields.put(h, v);
                else if (textColumns.contains(h) && !v.isBlank()) cells.put(h, new CellDto(null, null, v));
            }
            try {
                records.create(type, new RecordRequest(fields, cells), SyncState.IMPORTED);
                created++;
            } catch (RuntimeException e) {
                skipped.add("строка " + (r + 1) + ": " + e.getMessage());
            }
        }
        return new Result(created, skipped, ignored);
    }

    private static int count(String s, char ch) {
        int n = 0;
        for (int i = 0; i < s.length(); i++) if (s.charAt(i) == ch) n++;
        return n;
    }

    /** Разбор CSV с кавычками и переносами строк внутри ячеек. */
    static List<List<String>> parse(String s, char delim) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cur = new StringBuilder();
        boolean inQuotes = false;
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            if (inQuotes) {
                if (ch == '"') {
                    if (i + 1 < s.length() && s.charAt(i + 1) == '"') {
                        cur.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    cur.append(ch);
                }
            } else if (ch == '"') {
                inQuotes = true;
            } else if (ch == delim) {
                row.add(cur.toString());
                cur.setLength(0);
            } else if (ch == '\n' || ch == '\r') {
                if (ch == '\r' && i + 1 < s.length() && s.charAt(i + 1) == '\n') i++;
                row.add(cur.toString());
                cur.setLength(0);
                addRow(rows, row);
                row = new ArrayList<>();
            } else {
                cur.append(ch);
            }
        }
        if (cur.length() > 0 || !row.isEmpty()) {
            row.add(cur.toString());
            addRow(rows, row);
        }
        return rows;
    }

    private static void addRow(List<List<String>> rows, List<String> row) {
        if (row.stream().anyMatch(c -> !c.isBlank())) rows.add(row); // пустые строки пропускаем
    }
}
