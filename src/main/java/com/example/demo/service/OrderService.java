package com.example.demo.service;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Customer;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import com.example.demo.model.Product;
import com.example.demo.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Service class for Order business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final CustomerService customerService;
    private final ProductService productService;

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        log.debug("Fetching all orders");
        return orderRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Long id) {
        log.debug("Fetching order with id: {}", id);
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", id));
    }

    @Transactional
    public Order createOrder(Order order) {
        log.debug("Creating new order for customer id: {}", order.getCustomer().getId());
        
        // Validate customer exists
        Customer customer = customerService.getCustomerById(order.getCustomer().getId());
        order.setCustomer(customer);
        
        // Set order date if not provided
        if (order.getOrderDate() == null) {
            order.setOrderDate(LocalDateTime.now());
        }
        
        // Validate and process order items
        BigDecimal totalAmount = BigDecimal.ZERO;
        for (OrderItem item : order.getItems()) {
            Product product = productService.getProductById(item.getProduct().getId());
            
            // Check if product is available
            if (!product.isAvailable()) {
                throw new BusinessException("Product " + product.getName() + " is not available");
            }
            
            // Check stock availability
            if (product.getStockQuantity() < item.getQuantity()) {
                throw new BusinessException("Insufficient stock for product: " + product.getName());
            }
            
            // Set product and calculate price
            item.setProduct(product);
            item.setPrice(product.getPrice());
            
            // Update stock
            productService.updateStock(product.getId(), -item.getQuantity());
            
            // Calculate total
            BigDecimal itemTotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(item.getQuantity()));
            totalAmount = totalAmount.add(itemTotal);
        }
        
        order.setTotalAmount(totalAmount);
        order.setStatus("CONFIRMED");
        
        return orderRepository.save(order);
    }

    @Transactional
    public Order updateOrderStatus(Long id, String status) {
        log.debug("Updating order {} status to: {}", id, status);
        
        Order order = getOrderById(id);
        
        // Validate status
        List<String> validStatuses = List.of("PENDING", "CONFIRMED", "SHIPPED", 
                "DELIVERED", "CANCELLED");
        if (!validStatuses.contains(status)) {
            throw new BusinessException("Invalid order status: " + status);
        }
        
        order.setStatus(status);
        return orderRepository.save(order);
    }

    @Transactional
    public void cancelOrder(Long id) {
        log.debug("Cancelling order with id: {}", id);
        
        Order order = getOrderById(id);
        
        if ("DELIVERED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new BusinessException("Cannot cancel order with status: " + order.getStatus());
        }
        
        // Restore stock for cancelled order
        for (OrderItem item : order.getItems()) {
            productService.updateStock(item.getProduct().getId(), item.getQuantity());
        }
        
        order.setStatus("CANCELLED");
        orderRepository.save(order);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByCustomerId(Long customerId) {
        log.debug("Fetching orders for customer id: {}", customerId);
        return orderRepository.findByCustomerId(customerId);
    }

    @Transactional(readOnly = true)
    public List<Order> getOrdersByStatus(String status) {
        log.debug("Fetching orders with status: {}", status);
        return orderRepository.findByStatus(status);
    }
}
