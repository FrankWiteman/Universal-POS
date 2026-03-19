package com.universalpos.repository;

import com.universalpos.domain.TransactionItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TransactionItemRepository extends JpaRepository<TransactionItem, Long> {

    List<TransactionItem> findByTransaction_TxnId(Long txnId);
}
