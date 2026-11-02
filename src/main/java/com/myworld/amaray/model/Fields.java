package com.myworld.amaray.model;

import com.myworld.amaray.exception.BadRequestException;

import java.util.Map;
import java.util.regex.Pattern;

/** Разбор скалярных значений из JSON-карты. */
public final class Fields {

    private static final Pattern COLOR = Pattern.compile("^#[0-9A-Fa-f]{6}$");

    private Fields() {}

    /** Короткая строка: обрезаем пробелы, пустая становится null. */
    public static String str(Object o) {
        if (o == null) return null;
        String s = o.toString().trim();
        return s.isEmpty() ? null : s;
    }

    /** Длинный текст: пробелы не трогаем, пустой становится null. */
    public static String text(Object o) {
        if (o == null) return null;
        String s = o.toString();
        return s.isBlank() ? null : s;
    }

    public static Integer integer(Object o, String name) {
        if (o == null) return null;
        if (o instanceof Number n) return n.intValue();
        String s = o.toString().trim();
        if (s.isEmpty()) return null;
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            throw new BadRequestException("Поле '" + name + "' должно быть числом");
        }
    }

    public static String color(Object o, String name) {
        String s = str(o);
        if (s == null) return null;
        if (!COLOR.matcher(s).matches()) {
            throw new BadRequestException("Поле '" + name + "' должно быть цветом вида #RRGGBB");
        }
        return s.toUpperCase();
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> map(Object o, String name) {
        if (o == null) return null;
        if (o instanceof Map<?, ?> m) return (Map<String, Object>) m;
        throw new BadRequestException("Поле '" + name + "' должно быть объектом");
    }
}
