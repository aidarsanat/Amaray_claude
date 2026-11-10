package com.myworld.amaray.dto.record;

/** state: NOT_BORN, ALIVE или DEAD. Для DEAD age это возраст на момент смерти. */
public record AgeAtEvent(Integer age, String state) {
}
