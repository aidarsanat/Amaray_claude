package com.myworld.amaray.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashMap;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "characters")
public class Characters extends BaseEntity {

    @Column(nullable = false)
    private String firstName;

    @Column
    private String lastName;

    @Column
    private String nickname;

    @Column
    private Integer birthYear; // точный год рождения, нужен для расчёта возраста на момент события

    @Column
    private Integer age;

    @Embedded
    private Appearance appearance;

    @Override
    public String displayName() {
        String first = firstName == null ? "" : firstName;
        return lastName == null ? first : first + " " + lastName;
    }

    @Override
    public String mainName() {
        return firstName;
    }

    @Override
    public String searchText() {
        return nickname == null ? displayName() : displayName() + " " + nickname;
    }

    @Override
    public Map<String, Object> toFields() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("firstName", firstName);
        m.put("lastName", lastName);
        m.put("nickname", nickname);
        m.put("birthYear", birthYear);
        m.put("age", age);
        if (appearance == null) {
            m.put("appearance", null);
        } else {
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("height", appearance.getHeight());
            a.put("skinColor", appearance.getSkinColor());
            a.put("hairColor", appearance.getHairColor());
            a.put("eyeColor", appearance.getEyeColor());
            a.put("features", appearance.getFeatures());
            m.put("appearance", a);
        }
        return m;
    }

    @Override
    public void applyFields(Map<String, Object> f) {
        if (f.containsKey("firstName")) firstName = Fields.str(f.get("firstName"));
        if (f.containsKey("lastName")) lastName = Fields.str(f.get("lastName"));
        if (f.containsKey("nickname")) nickname = Fields.str(f.get("nickname"));
        if (f.containsKey("birthYear")) birthYear = Fields.integer(f.get("birthYear"), "birthYear");
        if (f.containsKey("age")) age = Fields.integer(f.get("age"), "age");
        if (f.containsKey("appearance")) {
            Map<String, Object> a = Fields.map(f.get("appearance"), "appearance");
            if (a == null) {
                appearance = null;
            } else {
                Appearance ap = appearance != null ? appearance : new Appearance();
                if (a.containsKey("height")) ap.setHeight(Fields.integer(a.get("height"), "height"));
                if (a.containsKey("skinColor")) ap.setSkinColor(Fields.color(a.get("skinColor"), "skinColor"));
                if (a.containsKey("hairColor")) ap.setHairColor(Fields.color(a.get("hairColor"), "hairColor"));
                if (a.containsKey("eyeColor")) ap.setEyeColor(Fields.color(a.get("eyeColor"), "eyeColor"));
                if (a.containsKey("features")) ap.setFeatures(Fields.text(a.get("features")));
                appearance = ap;
            }
        }
    }
}
