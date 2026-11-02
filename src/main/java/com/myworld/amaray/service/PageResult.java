package com.myworld.amaray.service;

import java.util.List;

public record PageResult<T>(List<T> items, long total) {
}
