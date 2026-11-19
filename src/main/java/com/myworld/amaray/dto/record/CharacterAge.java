package com.myworld.amaray.dto.record;

/**
 * Возраст персонажа в ячейке «Возраст».
 * state: ALIVE (жив к текущему году мира), DEAD (умер, age = возраст на момент смерти),
 * NOT_BORN (ещё не родился, age = null), MANUAL (нет года мира или года рождения, age = ручное значение или null).
 */
public record CharacterAge(Integer age, String state) {
}
