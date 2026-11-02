package com.myworld.amaray.repository;

import com.myworld.amaray.exception.NotFoundException;
import com.myworld.amaray.model.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.FlushModeType;
import jakarta.persistence.TypedQuery;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.*;

/** Один доступ ко всем таблицам мира по RefType, чтобы не писать девять одинаковых репозиториев. */
@Repository
@RequiredArgsConstructor
public class RecordStore {

    private static final Map<RefType, Class<? extends BaseEntity>> CLASSES = new EnumMap<>(RefType.class);

    static {
        CLASSES.put(RefType.CHARACTER, Characters.class);
        CLASSES.put(RefType.FACTION, Faction.class);
        CLASSES.put(RefType.ROLE, Role.class);
        CLASSES.put(RefType.RACE, Race.class);
        CLASSES.put(RefType.LAND, Land.class);
        CLASSES.put(RefType.SETTLEMENT, Settlement.class);
        CLASSES.put(RefType.PLACE, Place.class);
        CLASSES.put(RefType.ITEM, Item.class);
        CLASSES.put(RefType.EVENT, Event.class);
    }

    private final EntityManager em;

    public Class<? extends BaseEntity> classOf(RefType type) {
        Class<? extends BaseEntity> c = CLASSES.get(type);
        if (c == null) throw new IllegalArgumentException("Не таблица записей: " + type);
        return c;
    }

    public BaseEntity newInstance(RefType type) {
        try {
            return classOf(type).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    public BaseEntity find(RefType type, Long id) {
        return em.find(classOf(type), id);
    }

    public BaseEntity require(RefType type, Long id) {
        BaseEntity e = find(type, id);
        if (e == null) throw new NotFoundException("Запись " + type.path() + " с id " + id + " не найдена");
        return e;
    }

    public List<BaseEntity> all(RefType type) {
        List<? extends BaseEntity> list = em.createQuery(
                "from " + classOf(type).getSimpleName() + " e order by e.id", classOf(type)).getResultList();
        return new ArrayList<BaseEntity>(list);
    }

    public List<BaseEntity> byIds(RefType type, Collection<Long> ids) {
        if (ids.isEmpty()) return List.of();
        List<? extends BaseEntity> list = em.createQuery(
                        "from " + classOf(type).getSimpleName() + " e where e.id in :ids order by e.id", classOf(type))
                .setParameter("ids", ids).getResultList();
        return new ArrayList<BaseEntity>(list);
    }

    public BaseEntity save(BaseEntity e) {
        if (e.getId() == null) {
            em.persist(e);
            return e;
        }
        return em.merge(e);
    }

    public void delete(BaseEntity e) {
        em.remove(em.contains(e) ? e : em.merge(e));
    }

    /** Запись изменилась, в Sheets она теперь отличается. */
    public void markPending(RefType type, Long id) {
        if (!type.isRecord()) return;
        BaseEntity e = find(type, id);
        if (e != null) e.setSyncState(SyncState.PENDING);
    }

    /** Есть ли запись с такими значениями полей (null означает IS NULL). excludeId исключает саму запись. */
    public boolean exists(RefType type, Map<String, Object> conds, Long excludeId) {
        StringBuilder q = new StringBuilder("select count(e) from " + classOf(type).getSimpleName() + " e where 1=1");
        Map<String, Object> params = new LinkedHashMap<>();
        int i = 0;
        for (Map.Entry<String, Object> c : conds.entrySet()) {
            if (c.getValue() == null) {
                q.append(" and e.").append(c.getKey()).append(" is null");
            } else {
                String p = "p" + i++;
                q.append(" and e.").append(c.getKey()).append(" = :").append(p);
                params.put(p, c.getValue());
            }
        }
        if (excludeId != null) {
            q.append(" and e.id <> :ex");
            params.put("ex", excludeId);
        }
        TypedQuery<Long> query = em.createQuery(q.toString(), Long.class);
        for (Map.Entry<String, Object> p : params.entrySet()) {
            query.setParameter(p.getKey(), p.getValue());
        }
        // не сбрасываем в БД ещё не проверенные изменения самой записи
        query.setFlushMode(FlushModeType.COMMIT);
        return query.getSingleResult() > 0;
    }
}
