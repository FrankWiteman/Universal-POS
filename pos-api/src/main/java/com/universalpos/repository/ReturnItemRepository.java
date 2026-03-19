package com.universalpos.repository;

import com.universalpos.domain.ReturnItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReturnItemRepository extends JpaRepository<ReturnItem, Long> {

    List<ReturnItem> findByReturnTransaction_TxnId(Long returnTxnId);

    /** How many of a given item have already been returned across all return transactions */
    @Query("SELECT COALESCE(SUM(ri.qtyReturned), 0) FROM ReturnItem ri " +
           "WHERE ri.originalItem.itemId = :itemId")
    Integer sumReturnedQtyForItem(@Param("itemId") Long itemId);
}
