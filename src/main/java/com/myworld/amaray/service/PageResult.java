package com.myworld.amaray.service;

import java.util.List;

/** items: страница списка, total: сколько записей показано всего, hidden: сколько скрыла сортировка с выбором. */
public record PageResult<T>(List<T> items, long total, int hidden) {
}
