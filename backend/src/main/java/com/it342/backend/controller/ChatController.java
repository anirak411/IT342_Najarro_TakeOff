package com.it342.backend.controller;

import com.it342.backend.model.ChatMessage;
import com.it342.backend.model.EscrowTransaction;
import com.it342.backend.model.User;
import com.it342.backend.model.UserRole;
import com.it342.backend.repository.ChatMessageRepository;
import com.it342.backend.repository.EscrowTransactionRepository;
import com.it342.backend.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@RestController
@RequestMapping("/api/messages")
@CrossOrigin(origins = "*")
public class ChatController {

    private static final String CHANNEL_SUPPORT = "SUPPORT";
    private static final String CHANNEL_LISTING_REQUEST = "LISTING_REQUEST";

    private final ChatMessageRepository chatMessageRepository;
    private final UserRepository userRepository;
    private final EscrowTransactionRepository transactionRepository;

    public ChatController(
            ChatMessageRepository chatMessageRepository,
            UserRepository userRepository,
            EscrowTransactionRepository transactionRepository
    ) {
        this.chatMessageRepository = chatMessageRepository;
        this.userRepository = userRepository;
        this.transactionRepository = transactionRepository;
    }

    @GetMapping
    public List<ChatMessage> getConversation(
            @RequestParam String user1,
            @RequestParam String user2
    ) {
        return chatMessageRepository.findConversation(user1, user2);
    }

    @PostMapping
    public ChatMessage sendMessage(@RequestBody Map<String, String> payload) {
        String senderEmail = normalizeEmail(payload.get("senderEmail"));
        String receiverEmail = normalizeEmail(payload.get("receiverEmail"));
        String content = raw(payload.get("content"));
        String channel = normalizeChannel(payload.get("channel"));
        String transactionIdRaw = raw(payload.get("transactionId"));

        if (senderEmail.isBlank() || receiverEmail.isBlank() || content.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "senderEmail, receiverEmail, and content are required"
            );
        }

        User sender = userRepository.findByEmailIgnoreCase(senderEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "senderEmail is invalid"));
        User receiver = userRepository.findByEmailIgnoreCase(receiverEmail)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "receiverEmail is invalid"));

        enforceAdminMessagingPolicy(sender, receiver, channel, transactionIdRaw);

        ChatMessage message = new ChatMessage();
        message.setSenderEmail(senderEmail);
        message.setReceiverEmail(receiverEmail);
        message.setContent(content);
        message.setCreatedAt(LocalDateTime.now());

        return chatMessageRepository.save(message);
    }

    private void enforceAdminMessagingPolicy(
            User sender,
            User receiver,
            String channel,
            String transactionIdRaw
    ) {
        boolean senderIsAdmin = sender.getRole() == UserRole.ADMIN;
        boolean receiverIsAdmin = receiver.getRole() == UserRole.ADMIN;
        if (senderIsAdmin || !receiverIsAdmin) {
            return;
        }

        if (CHANNEL_SUPPORT.equals(channel)) {
            return;
        }

        if (CHANNEL_LISTING_REQUEST.equals(channel)) {
            validateListingRequestParticipant(sender.getEmail(), transactionIdRaw);
            return;
        }

        throw new ResponseStatusException(
                HttpStatus.FORBIDDEN,
                "Users cannot message admins directly. Use customer support or listing request chat."
        );
    }

    private void validateListingRequestParticipant(String senderEmail, String transactionIdRaw) {
        if (transactionIdRaw.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "transactionId is required for LISTING_REQUEST messages"
            );
        }

        long transactionId;
        try {
            transactionId = Long.parseLong(transactionIdRaw);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid transactionId");
        }

        EscrowTransaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));

        String sender = normalizeEmail(senderEmail);
        boolean isParticipant =
                sender.equals(normalizeEmail(tx.getBuyerEmail()))
                        || sender.equals(normalizeEmail(tx.getSellerEmail()));

        if (!isParticipant) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Only listing request participants can message admin with LISTING_REQUEST channel"
            );
        }
    }

    private String raw(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeEmail(String value) {
        String normalized = raw(value).toLowerCase(Locale.ROOT);
        if (normalized.isBlank()
                || "null".equals(normalized)
                || "undefined".equals(normalized)) {
            return "";
        }
        return normalized;
    }

    private String normalizeChannel(String value) {
        return raw(value).toUpperCase(Locale.ROOT);
    }
}
