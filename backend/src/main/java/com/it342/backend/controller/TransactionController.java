package com.it342.backend.controller;

import com.it342.backend.dto.BuyerActionRequest;
import com.it342.backend.dto.CreateTransactionRequest;
import com.it342.backend.model.EscrowTransaction;
import com.it342.backend.model.Item;
import com.it342.backend.model.TransactionStatus;
import com.it342.backend.model.User;
import com.it342.backend.repository.EscrowTransactionRepository;
import com.it342.backend.repository.ItemRepository;
import com.it342.backend.security.SessionService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/transactions")
@CrossOrigin(origins = "*")
public class TransactionController {

    private final EscrowTransactionRepository transactionRepository;
    private final ItemRepository itemRepository;
    private final SessionService sessionService;

    public TransactionController(
            EscrowTransactionRepository transactionRepository,
            ItemRepository itemRepository,
            SessionService sessionService
    ) {
        this.transactionRepository = transactionRepository;
        this.itemRepository = itemRepository;
        this.sessionService = sessionService;
    }

    @PostMapping
    public EscrowTransaction createTransaction(@RequestBody CreateTransactionRequest request) {
        if (request.getItemId() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "itemId is required");
        }

        String buyerEmail = normalize(request.getBuyerEmail());
        if (buyerEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "buyerEmail is required");
        }

        Item item = itemRepository.findById(request.getItemId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));

        String sellerEmail = normalize(item.getSellerEmail());
        if (sellerEmail.isBlank()) {
            sellerEmail = normalize(item.getSellerName());
        }
        if (sellerEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Listing seller identity is missing");
        }

        if (buyerEmail.equalsIgnoreCase(sellerEmail)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "You cannot create a transaction for your own listing"
            );
        }

        List<TransactionStatus> activeStates = List.of(
                TransactionStatus.PENDING,
                TransactionStatus.PAYMENT_HELD,
                TransactionStatus.DELIVERY_CONFIRMED
        );
        if (transactionRepository.existsByItemIdAndStatusIn(item.getId(), activeStates)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "This listing already has an active transaction"
            );
        }

        EscrowTransaction tx = new EscrowTransaction();
        tx.setItemId(item.getId());
        tx.setItemTitle(item.getTitle());
        tx.setItemPrice(item.getPrice());
        tx.setBuyerEmail(buyerEmail);
        tx.setBuyerName(
                normalize(request.getBuyerName()).isBlank()
                        ? fallbackBuyerNameFromEmail(buyerEmail)
                        : request.getBuyerName().trim()
        );
        tx.setSellerEmail(sellerEmail);
        tx.setSellerName(normalize(item.getSellerName()).isBlank() ? sellerEmail : item.getSellerName());
        tx.setStatus(TransactionStatus.PENDING);

        return transactionRepository.save(tx);
    }

    @GetMapping
    public List<EscrowTransaction> getTransactions(
            @RequestParam(required = false) String email,
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        String normalizedEmail = normalize(email);
        if (normalizedEmail.isBlank()) {
            sessionService.requireAdminUser(sessionToken);
            return transactionRepository.findAllByOrderByCreatedAtDesc();
        }

        return transactionRepository.findByBuyerEmailIgnoreCaseOrSellerEmailIgnoreCaseOrderByCreatedAtDesc(
                normalizedEmail,
                normalizedEmail
        );
    }

    @PutMapping("/{id}/hold")
    public EscrowTransaction holdPayment(
            @PathVariable Long id,
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        User admin = sessionService.requireAdminUser(sessionToken);

        EscrowTransaction tx = findById(id);
        ensureStatus(tx, TransactionStatus.PENDING, "Only pending transactions can be held");

        tx.setStatus(TransactionStatus.PAYMENT_HELD);
        tx.setAdminEmail(admin.getEmail());
        return transactionRepository.save(tx);
    }

    @PutMapping("/{id}/confirm-delivery")
    public EscrowTransaction confirmDelivery(
            @PathVariable Long id,
            @RequestBody BuyerActionRequest request
    ) {
        String buyerEmail = normalize(request.getBuyerEmail());
        if (buyerEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "buyerEmail is required");
        }

        EscrowTransaction tx = findById(id);
        ensureStatus(tx, TransactionStatus.PAYMENT_HELD, "Delivery can be confirmed only after payment is held");
        if (!buyerEmail.equalsIgnoreCase(normalize(tx.getBuyerEmail()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the buyer can confirm delivery");
        }

        tx.setStatus(TransactionStatus.DELIVERY_CONFIRMED);
        tx.setDeliveryConfirmedAt(LocalDateTime.now());
        return transactionRepository.save(tx);
    }

    @PutMapping("/{id}/complete")
    public EscrowTransaction completeTransaction(
            @PathVariable Long id,
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        User admin = sessionService.requireAdminUser(sessionToken);

        EscrowTransaction tx = findById(id);
        ensureStatus(
                tx,
                TransactionStatus.DELIVERY_CONFIRMED,
                "Only delivery-confirmed transactions can be completed"
        );

        tx.setStatus(TransactionStatus.COMPLETED);
        tx.setAdminEmail(admin.getEmail());
        tx.setCompletedAt(LocalDateTime.now());
        return transactionRepository.save(tx);
    }

    @PutMapping("/{id}/refund")
    public EscrowTransaction refundTransaction(
            @PathVariable Long id,
            @RequestHeader(value = "X-Session-Token", required = false) String sessionToken
    ) {
        User admin = sessionService.requireAdminUser(sessionToken);

        EscrowTransaction tx = findById(id);
        if (tx.getStatus() != TransactionStatus.PAYMENT_HELD
                && tx.getStatus() != TransactionStatus.DELIVERY_CONFIRMED) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Only held or delivery-confirmed transactions can be refunded"
            );
        }

        tx.setStatus(TransactionStatus.REFUNDED);
        tx.setAdminEmail(admin.getEmail());
        tx.setRefundedAt(LocalDateTime.now());
        return transactionRepository.save(tx);
    }

    private EscrowTransaction findById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
    }

    private void ensureStatus(EscrowTransaction tx, TransactionStatus expected, String message) {
        if (tx.getStatus() != expected) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String fallbackBuyerNameFromEmail(String email) {
        return email.contains("@")
                ? email.substring(0, email.indexOf('@'))
                : email;
    }
}
