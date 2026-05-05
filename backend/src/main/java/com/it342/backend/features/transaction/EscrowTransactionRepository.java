package com.it342.backend.features.transaction;

import com.it342.backend.features.transaction.EscrowTransaction;
import com.it342.backend.features.transaction.TransactionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EscrowTransactionRepository extends JpaRepository<EscrowTransaction, Long> {

    List<EscrowTransaction> findByBuyerEmailIgnoreCaseOrSellerEmailIgnoreCaseOrderByCreatedAtDesc(
            String buyerEmail,
            String sellerEmail
    );

    List<EscrowTransaction> findAllByOrderByCreatedAtDesc();

    boolean existsByItemIdAndStatusIn(Long itemId, List<TransactionStatus> statuses);
}
