package com.myworld.amaray.service;

import com.myworld.amaray.exception.BadRequestException;
import com.myworld.amaray.model.*;
import com.myworld.amaray.repository.CellTextRepository;
import com.myworld.amaray.repository.LinkRepository;
import com.myworld.amaray.repository.RecordStore;
import com.myworld.amaray.repository.TagRepository;
import com.myworld.amaray.schema.ColumnKind;
import com.myworld.amaray.schema.ColumnSpec;
import com.myworld.amaray.schema.Schema;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;

/**
 * Сортировка таблиц по правилам из дока (раздел «Описание столбцов и значений строк», блок «Сортировки»).
 *
 * Виды сортировки:
 *  - обычная: текст по алфавиту, числа по значению (имена, названия, годы, рост, промежуток времени);
 *  - по списку значений: ячейки сравниваются словарно по всему списку в том порядке, в каком значения вписаны,
 *    более короткий список при совпадении идёт раньше; теги по порядку в справочнике, ссылки по имени записи;
 *  - с выбором (параметр choice): таблица, часть пары (tag или ref) или верхняя папка (id тега).
 *    Остаются только записи со значением из выбранного, остальные скрываются (их число в hidden).
 *
 * Порядок групп всегда такой: значения, затем ячейки только со свободным текстом (по алфавиту), затем пустые.
 * direction=desc разворачивает порядок внутри групп, но не сами группы. При равенстве записи идут по имени.
 */
@Component
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class RecordSorter {

    /** Столбцы, где значения из разных верхних папок означают разное: выбор папки. */
    private static final Map<RefType, Set<String>> FOLDER_CHOICE = Map.of(
            RefType.CHARACTER, Set.of("affiliation", "role"),
            RefType.FACTION, Set.of("power"),
            RefType.ITEM, Set.of("quality"),
            RefType.ROLE, Set.of("rating"));

    /** Теги-столбцы с обычной сортировкой по названию тега, а не по справочнику. */
    private static final Set<String> TAGS_BY_NAME = Set.of("ageRange");

    /** Теги-столбцы с числовой сортировкой по названию тега («0» … «15»). */
    private static final Set<String> TAGS_AS_NUMBERS = Set.of("appearanceRating");

    /** Простые поля, которые не сортируются: длинные поля и служебное. */
    private static final Set<String> NOT_SORTABLE = Set.of("description", "features", "image", "appearance");

    private final LinkRepository links;
    private final CellTextRepository texts;
    private final TagService tagService;
    private final TagRepository tags;
    private final RecordStore store;
    private final WorldSettingsService settings;

    public record Result(List<BaseEntity> items, int hidden) {}

    /** Ключ записи: группа (0 значения, 1 только текст, 2 пусто), значения и текст. */
    private record Key(int group, List<Comparable<?>> values, String text) {
        static final Key EMPTY = new Key(2, List.of(), null);

        static Key of(List<Comparable<?>> values, String text) {
            if (!values.isEmpty()) return new Key(0, values, null);
            if (text != null && !text.isBlank()) return new Key(1, List.of(), text);
            return EMPTY;
        }

        static Key single(Comparable<?> v) {
            return v == null ? EMPTY : new Key(0, List.of(v), null);
        }
    }

    public Result sort(RefType type, List<BaseEntity> items, String sortByParam, String choice,
                       boolean desc, boolean showHidden) {
        String sortBy = (sortByParam == null || sortByParam.isBlank()) ? "name" : sortByParam.trim();
        if (items.isEmpty()) return new Result(items, 0);

        Map<Long, Key> keys = new HashMap<>();
        boolean withChoice = keysFor(type, items, sortBy, blankToNull(choice), keys);

        List<BaseEntity> shown = new ArrayList<>();
        List<BaseEntity> hidden = new ArrayList<>();
        for (BaseEntity e : items) {
            Key k = keys.getOrDefault(e.getId(), Key.EMPTY);
            if (withChoice && k.group() != 0) hidden.add(e);
            else shown.add(e);
        }

        Comparator<BaseEntity> byName = Comparator.comparing(e -> nz(e.displayName()), String.CASE_INSENSITIVE_ORDER);
        shown.sort((x, y) -> {
            int r = compare(keys.getOrDefault(x.getId(), Key.EMPTY), keys.getOrDefault(y.getId(), Key.EMPTY), desc);
            return r != 0 ? r : byName.compare(x, y);
        });

        if (showHidden) {
            hidden.sort(byName);
            shown.addAll(hidden); // скрытые показываются в конце списка
        }
        return new Result(shown, hidden.size());
    }

    // ───────────────────────── Ключи ─────────────────────────

    /** Заполняет keys. Возвращает true, если это сортировка с выбором. */
    private boolean keysFor(RefType type, List<BaseEntity> items, String sortBy, String choice, Map<Long, Key> keys) {
        switch (sortBy) {
            case "id" -> { items.forEach(e -> keys.put(e.getId(), Key.single(e.getId()))); return false; }
            case "name" -> { items.forEach(e -> keys.put(e.getId(), Key.single(blankToNull(e.displayName())))); return false; }
            case "syncState" -> { items.forEach(e -> keys.put(e.getId(), Key.single(e.getSyncState().ordinal()))); return false; }
        }

        if (type == RefType.CHARACTER && sortBy.equals("age")) { // вычисленный возраст по «текущему году мира»
            Integer year = settings.worldYear();
            items.forEach(e -> keys.put(e.getId(), Key.single(ChronologyService.ageAt((Characters) e, year).age())));
            return false;
        }
        if (type == RefType.CHARACTER && sortBy.equals("height")) { // Внешность.рост
            items.forEach(e -> {
                Appearance a = ((Characters) e).getAppearance();
                keys.put(e.getId(), Key.single(a == null ? null : a.getHeight()));
            });
            return false;
        }

        Optional<ColumnSpec> column = Schema.find(type, sortBy);
        if (column.isEmpty()) { // простое поле: обычная сортировка
            if (NOT_SORTABLE.contains(sortBy) || !items.get(0).toFields().containsKey(sortBy)) {
                throw new BadRequestException("Нельзя сортировать по '" + sortBy + "'");
            }
            items.forEach(e -> {
                Object v = e.toFields().get(sortBy);
                keys.put(e.getId(), Key.single(v instanceof String s ? blankToNull(s) : (Comparable<?>) v));
            });
            return false;
        }

        ColumnSpec spec = column.get();
        if (spec.kind() == ColumnKind.MIRROR) {
            throw new BadRequestException("Столбец '" + sortBy + "' не сортируется");
        }

        boolean folderChoice = FOLDER_CHOICE.getOrDefault(type, Set.of()).contains(sortBy);
        boolean tableChoice = spec.kind() == ColumnKind.REFS && spec.targets().size() > 1;
        boolean pairChoice = spec.kind() == ColumnKind.PAIRS;
        boolean withChoice = folderChoice || tableChoice || pairChoice;
        if (withChoice && choice == null) {
            throw new BadRequestException("Для сортировки по '" + sortBy + "' нужен параметр choice: " + expected(spec, folderChoice));
        }

        Map<Long, List<Link>> byOwner = new HashMap<>();
        for (Link l : links.findByOwnerTypeAndColOrderByPos(type, sortBy)) {
            byOwner.computeIfAbsent(l.getOwnerId(), k -> new ArrayList<>()).add(l);
        }
        Map<Long, String> cellTexts = withChoice ? Map.of() : cellTexts(type, items, sortBy);

        Function<Link, Comparable<?>> value; // значение одного Link для сравнения, null = не участвует
        if (pairChoice) {
            value = pairValue(spec, choice, byOwner);
        } else if (tableChoice) {
            RefType table = parseTable(choice, spec);
            Map<Long, String> names = names(table, targetIds(byOwner, table));
            value = l -> l.getTargetType() == table ? names.get(l.getTargetId()) : null;
        } else if (folderChoice) {
            value = folderValue(spec, parseId(choice), byOwner);
        } else if (spec.kind() == ColumnKind.TAGS) {
            value = tagValue(spec, sortBy);
        } else { // REFS из одной таблицы: по имени записи
            RefType table = spec.targets().iterator().next();
            Map<Long, String> names = names(table, targetIds(byOwner, table));
            value = l -> l.getTargetType() == table ? names.get(l.getTargetId()) : null;
        }

        for (BaseEntity e : items) {
            List<Comparable<?>> vals = new ArrayList<>();
            for (Link l : byOwner.getOrDefault(e.getId(), List.of())) {
                Comparable<?> v = value.apply(l);
                if (v != null) vals.add(v);
            }
            keys.put(e.getId(), Key.of(vals, cellTexts.get(e.getId())));
        }
        return withChoice;
    }

    /** Теги: по справочнику, либо по названию (промежуток), либо числом (оценка внешности). */
    private Function<Link, Comparable<?>> tagValue(ColumnSpec spec, String field) {
        if (TAGS_BY_NAME.contains(field) || TAGS_AS_NUMBERS.contains(field)) {
            Map<Long, String> names = new HashMap<>();
            for (Tag t : tags.findByCategoryOrderBySortOrderAscIdAsc(spec.category())) names.put(t.getId(), t.getName());
            if (TAGS_AS_NUMBERS.contains(field)) {
                return l -> l.getTargetType() == RefType.TAG ? number(names.get(l.getTargetId())) : null;
            }
            return l -> l.getTargetType() == RefType.TAG ? names.get(l.getTargetId()) : null;
        }
        Map<Long, Integer> rank = tagService.ranks(spec.category());
        return l -> l.getTargetType() == RefType.TAG ? rank.get(l.getTargetId()) : null;
    }

    /** Пары: choice=tag сравнивает теги по справочнику, choice=ref сравнивает ссылки по имени. */
    private Function<Link, Comparable<?>> pairValue(ColumnSpec spec, String choice, Map<Long, List<Link>> byOwner) {
        if (choice.equalsIgnoreCase("tag")) {
            Map<Long, Integer> rank = tagService.ranks(spec.category());
            return l -> l.getTargetType() == RefType.TAG ? rank.get(l.getTargetId()) : null;
        }
        if (choice.equalsIgnoreCase("ref")) {
            Map<RefType, Map<Long, String>> names = new EnumMap<>(RefType.class);
            for (RefType t : spec.targets()) names.put(t, names(t, refIds(byOwner, t)));
            return l -> l.getRefType() == null ? null : names.getOrDefault(l.getRefType(), Map.of()).get(l.getRefId());
        }
        throw new BadRequestException("choice для пары: tag или ref");
    }

    /**
     * Выбор верхней папки. Для тегов (мощь, качество, оценка) берутся теги из этой папки.
     * Для ссылок (фракция и роль персонажа) берутся записи, у которых «Тип» лежит в этой папке.
     */
    private Function<Link, Comparable<?>> folderValue(ColumnSpec spec, Long folderId, Map<Long, List<Link>> byOwner) {
        Tag folder = tagService.get(folderId);
        Set<Long> inFolder = tagService.descendantIds(folderId);

        if (spec.kind() == ColumnKind.TAGS) {
            if (folder.getCategory() != spec.category()) {
                throw new BadRequestException("Папка " + folderId + " не из справочника столбца '" + spec.field() + "'");
            }
            Map<Long, Integer> rank = tagService.ranks(spec.category());
            return l -> l.getTargetType() == RefType.TAG && inFolder.contains(l.getTargetId()) ? rank.get(l.getTargetId()) : null;
        }

        RefType table = spec.targets().iterator().next();
        ColumnSpec typeCol = Schema.require(table, "type");
        if (folder.getCategory() != typeCol.category()) {
            throw new BadRequestException("Папка " + folderId + " не из типов таблицы " + table.path());
        }
        Set<Long> matching = new HashSet<>(); // записи таблицы, у которых тип в выбранной папке
        for (Link t : links.findByOwnerTypeAndColOrderByPos(table, "type")) {
            if (t.getTargetType() == RefType.TAG && inFolder.contains(t.getTargetId())) matching.add(t.getOwnerId());
        }
        Map<Long, String> names = names(table, targetIds(byOwner, table));
        return l -> l.getTargetType() == table && matching.contains(l.getTargetId()) ? names.get(l.getTargetId()) : null;
    }

    // ───────────────────────── Сравнение ─────────────────────────

    private static int compare(Key a, Key b, boolean desc) {
        if (a.group() != b.group()) return Integer.compare(a.group(), b.group()); // группы не разворачиваются
        int r = switch (a.group()) {
            case 0 -> compareLists(a.values(), b.values());
            case 1 -> a.text().compareToIgnoreCase(b.text());
            default -> 0;
        };
        return desc ? -r : r;
    }

    /** Словарно: первое, при равенстве второе и т.д.; более короткий список при совпадении раньше. */
    @SuppressWarnings({"unchecked", "rawtypes"})
    static int compareLists(List<Comparable<?>> a, List<Comparable<?>> b) {
        for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
            Object x = a.get(i), y = b.get(i);
            int c = (x instanceof String s && y instanceof String t)
                    ? s.compareToIgnoreCase(t)
                    : ((Comparable) x).compareTo(y);
            if (c != 0) return c;
        }
        return Integer.compare(a.size(), b.size());
    }

    // ───────────────────────── Вспомогательное ─────────────────────────

    private Map<Long, String> cellTexts(RefType type, List<BaseEntity> items, String col) {
        Map<Long, String> out = new HashMap<>();
        List<Long> ids = items.stream().map(BaseEntity::getId).toList();
        for (CellText t : texts.findByOwnerTypeAndOwnerIdIn(type, ids)) {
            if (col.equals(t.getCol())) out.put(t.getOwnerId(), t.getContent());
        }
        return out;
    }

    private Map<Long, String> names(RefType table, Collection<Long> ids) {
        Map<Long, String> out = new HashMap<>();
        for (BaseEntity e : store.byIds(table, ids)) out.put(e.getId(), nz(e.displayName()));
        return out;
    }

    private static Set<Long> targetIds(Map<Long, List<Link>> byOwner, RefType table) {
        Set<Long> ids = new HashSet<>();
        byOwner.values().forEach(ls -> ls.forEach(l -> { if (l.getTargetType() == table) ids.add(l.getTargetId()); }));
        return ids;
    }

    private static Set<Long> refIds(Map<Long, List<Link>> byOwner, RefType table) {
        Set<Long> ids = new HashSet<>();
        byOwner.values().forEach(ls -> ls.forEach(l -> { if (l.getRefType() == table) ids.add(l.getRefId()); }));
        return ids;
    }

    private static RefType parseTable(String choice, ColumnSpec spec) {
        try {
            RefType t = RefType.valueOf(choice.trim().toUpperCase());
            if (spec.targets().contains(t)) return t;
        } catch (IllegalArgumentException ignored) {
        }
        throw new BadRequestException("choice для '" + spec.field() + "': одна из таблиц " + spec.targets());
    }

    private static Long parseId(String choice) {
        try {
            return Long.parseLong(choice.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("choice для выбора папки: id тега-папки");
        }
    }

    private static String expected(ColumnSpec spec, boolean folderChoice) {
        if (folderChoice) return "id верхней папки";
        if (spec.kind() == ColumnKind.PAIRS) return "tag или ref";
        return "одна из таблиц " + spec.targets();
    }

    private static Double number(String s) {
        if (s == null) return null;
        try {
            return Double.parseDouble(s.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
