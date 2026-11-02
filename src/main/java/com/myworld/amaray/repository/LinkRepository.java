package com.myworld.amaray.repository;

import com.myworld.amaray.model.Link;
import com.myworld.amaray.model.RefType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface LinkRepository extends JpaRepository<Link, Long> {

    List<Link> findByOwnerTypeAndOwnerIdAndColOrderByPos(RefType ownerType, Long ownerId, String col);

    List<Link> findByOwnerTypeAndOwnerIdInOrderByPos(RefType ownerType, Collection<Long> ownerIds);

    List<Link> findByOwnerTypeAndColOrderByPos(RefType ownerType, String col);

    /** Все, кто ссылается на запись или тег. */
    List<Link> findByTargetTypeAndTargetId(RefType targetType, Long targetId);

    /** Пары, где запись стоит во второй части. */
    List<Link> findByRefTypeAndRefId(RefType refType, Long refId);

    /** Зеркало: кто из таблиц-хозяев ссылается на запись в нужном столбце. */
    List<Link> findByTargetTypeAndTargetIdAndColAndOwnerTypeIn(
            RefType targetType, Long targetId, String col, Collection<RefType> ownerTypes);

    List<Link> findByTargetTypeAndTargetIdInAndColAndOwnerTypeIn(
            RefType targetType, Collection<Long> targetIds, String col, Collection<RefType> ownerTypes);

    void deleteByOwnerTypeAndOwnerId(RefType ownerType, Long ownerId);

    void deleteByOwnerTypeAndOwnerIdAndCol(RefType ownerType, Long ownerId, String col);

    /** Для фильтров: записи, у которых в столбце есть одно из значений. */
    @Query("select distinct l.ownerId from Link l where l.ownerType = :ot and l.col = :col "
            + "and l.targetType = :tt and l.targetId in :ids")
    List<Long> ownersByTarget(@Param("ot") RefType ownerType, @Param("col") String col,
                              @Param("tt") RefType targetType, @Param("ids") Collection<Long> ids);

    @Query("select distinct l.ownerId from Link l where l.ownerType = :ot and l.col = :col "
            + "and l.refType = :rt and l.refId = :rid")
    List<Long> ownersByRef(@Param("ot") RefType ownerType, @Param("col") String col,
                           @Param("rt") RefType refType, @Param("rid") Long refId);
}
