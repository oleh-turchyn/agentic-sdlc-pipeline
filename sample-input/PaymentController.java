package com.example.payments;

import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.math.BigDecimal;

/**
 * PaymentController - handles payment processing operations.
 * Realistic Spring Boot controller with several common issues.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    // ❌ DIP violation - direct instantiation
    private PaymentService paymentService = new PaymentService();
    private FraudDetectionService fraudService = new FraudDetectionService();
    private NotificationService notificationService = new NotificationService();
    private AuditService auditService = new AuditService();

    // ❌ God method - too many responsibilities
    @PostMapping("/process")
    public Map<String, Object> processPayment(
            @RequestParam String userId,
            @RequestParam String cardNumber,
            @RequestParam double amount,
            @RequestParam String currency) {

        Map<String, Object> response = new HashMap<>();

        // ❌ No input validation
        // ❌ PCI compliance issue - logging card number
        System.out.println("Processing payment for card: " + cardNumber);

        // ❌ Hardcoded business rules
        if (amount > 10000) {
            response.put("status", "REJECTED");
            response.put("reason", "Amount exceeds limit");
            return response;
        }

        // ❌ Hardcoded currency list
        if (!currency.equals("USD") && !currency.equals("EUR") && !currency.equals("GBP")) {
            response.put("status", "REJECTED");
            return response;
        }

        // ❌ Fraud check result ignored silently
        boolean isFraud = fraudService.check(userId, amount);
        if (isFraud) {
            response.put("status", "REJECTED");
            return response;
        }

        // ❌ No transaction management
        String paymentId = paymentService.process(cardNumber, amount, currency);
        auditService.log(userId, paymentId, amount);

        // ❌ Sensitive data in response
        response.put("paymentId", paymentId);
        response.put("cardNumber", cardNumber);
        response.put("status", "SUCCESS");
        response.put("amount", amount);

        // ❌ Sync notification blocks response
        notificationService.sendReceipt(userId, paymentId, amount);

        return response;
    }

    @GetMapping("/history")
    public List<Map<String, Object>> getPaymentHistory(@RequestParam String userId) {
        // ❌ No authorization - any user can see any user's history
        // ❌ No pagination
        return paymentService.getHistory(userId);
    }

    @PostMapping("/refund")
    public Map<String, Object> refund(@RequestParam String paymentId, @RequestParam double amount) {
        Map<String, Object> response = new HashMap<>();

        // ❌ No validation that refund amount <= original payment
        // ❌ No check if payment exists
        // ❌ No authorization check
        String refundId = paymentService.refund(paymentId, amount);
        response.put("refundId", refundId);
        response.put("status", "REFUNDED");
        return response;
    }

    // ❌ Missing endpoints from spec: GET /payments/{paymentId}, POST /payments/void

    // Stub classes
    static class PaymentService {
        String process(String card, double amount, String currency) { return "PAY-" + UUID.randomUUID(); }
        List<Map<String, Object>> getHistory(String userId) { return new ArrayList<>(); }
        String refund(String paymentId, double amount) { return "REF-" + UUID.randomUUID(); }
    }
    static class FraudDetectionService {
        boolean check(String userId, double amount) { return false; }
    }
    static class NotificationService {
        void sendReceipt(String userId, String paymentId, double amount) {}
    }
    static class AuditService {
        void log(String userId, String paymentId, double amount) {}
    }
}
