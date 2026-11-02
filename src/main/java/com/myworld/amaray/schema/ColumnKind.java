package com.myworld.amaray.schema;

public enum ColumnKind {
    REFS,   // ссылки на записи
    TAGS,   // теги справочника
    PAIRS,  // пары «тег + необязательная ссылка»
    MIRROR  // зеркало: данные лежат у хозяина в другой таблице
}
