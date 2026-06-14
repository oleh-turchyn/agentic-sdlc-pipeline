package com.example.orders;

import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

/**
 * OrderController - handles order management operations.
 * Intentionally contains several issues for the agentic pipeline to detect.
 */
public class OrderController {

    //  SOLID violation: direct instantiation instead of dependency injection
    private OrderService orderService = new OrderService();
    private UserService userService = new UserService();
    private EmailService emailService = new EmailService();
    private InventoryService inventoryService = new InventoryService();

    //  God method: does too many things, violates SRP
    public Map<String, Object> createOrder(String userId, List<String> productIds, String paymentMethod) {
        Map<String, Object> result = new HashMap<>();

        //  No null checks / input validation
        List<Map<String, Object>> products = new ArrayList<>();
        for (String productId : productIds) {
            Map<String, Object> product = inventoryService.getProduct(productId);
            products.add(product);
        }

        //  Business logic mixed with controller logic
        double total = 0;
        for (Map<String, Object> product : products) {
            //  Unsafe casting, no error handling
            total += (double) product.get("price");
        }

        //  Hardcoded discount logic in controller
        if (total > 100) {
            total = total * 0.9; // 10% discount
        }

        //  No transaction management
        String orderId = orderService.createOrder(userId, products, total);
        userService.updateOrderHistory(userId, orderId);

        //  Side effect in controller (sending email)
        emailService.sendOrderConfirmation(userId, orderId, total);

        //  Returning raw Map instead of typed DTO
        result.put("orderId", orderId);
        result.put("total", total);
        result.put("status", "CREATED");
        return result;
    }

    //  Missing: getOrderById, updateOrder, cancelOrder (defined in openapi.yaml but not implemented)

    public List<Map<String, Object>> getAllOrders(String userId) {
        //  No pagination
        //  No authorization check - any user can see any user's orders
        return orderService.getAllOrders(userId);
    }

    //  deleteOrder is in openapi.yaml but completely missing here

    // Stub inner classes to make the file compile standalone
    static class OrderService {
        String createOrder(String userId, List<Map<String, Object>> products, double total) { return "ORD-123"; }
        List<Map<String, Object>> getAllOrders(String userId) { return new ArrayList<>(); }
    }
    static class UserService {
        void updateOrderHistory(String userId, String orderId) {}
    }
    static class EmailService {
        void sendOrderConfirmation(String userId, String orderId, double total) {}
    }
    static class InventoryService {
        Map<String, Object> getProduct(String productId) { return new HashMap<>(); }
    }
}
