package com.example.demo.service;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Customer;
import com.example.demo.model.Order;
import com.example.demo.model.OrderItem;
import com.example.demo.model.Product;
import com.example.demo.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CustomerService customerService;

    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void readsOrdersByAllSupportedFilters() {
        Order order = new Order();
        when(orderRepository.findAll()).thenReturn(List.of(order));
        when(orderRepository.findByCustomerId(5L)).thenReturn(List.of(order));
        when(orderRepository.findByStatus("PENDING")).thenReturn(List.of(order));

        assertEquals(List.of(order), orderService.getAllOrders());
        assertEquals(List.of(order), orderService.getOrdersByCustomerId(5L));
        assertEquals(List.of(order), orderService.getOrdersByStatus("PENDING"));
    }

    @Test
    void loadsOrderAndReportsMissingOrder() {
        Order order = new Order();
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));
        when(orderRepository.findById(4L)).thenReturn(Optional.empty());

        assertEquals(order, orderService.getOrderById(3L));
        assertThrows(ResourceNotFoundException.class, () -> orderService.getOrderById(4L));
    }

    @Test
    void createsOrderAndCalculatesTotal() {
        Customer customer = new Customer();
        customer.setId(5L);
        Product product = product(6L, true, 10);
        Order order = new Order();
        order.setCustomer(customer);
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(2);
        order.addItem(item);

        when(customerService.getCustomerById(5L)).thenReturn(customer);
        when(productService.getProductById(6L)).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Order saved = orderService.createOrder(order);

        assertNotNull(saved.getOrderDate());
        assertEquals("CONFIRMED", saved.getStatus());
        assertEquals(new BigDecimal("20.00"), saved.getTotalAmount());
        assertEquals(new BigDecimal("10.00"), item.getPrice());
        verify(productService).updateStock(6L, -2);
    }

    @Test
    void rejectsUnavailableProductAndInsufficientStock() {
        Customer customer = new Customer();
        customer.setId(5L);
        Product unavailable = product(6L, false, 10);
        when(customerService.getCustomerById(5L)).thenReturn(customer);
        when(productService.getProductById(6L)).thenReturn(unavailable);
        assertThrows(BusinessException.class, () -> orderService.createOrder(order(customer, 6L, 1)));

        Product available = product(6L, true, 10);
        available.setStockQuantity(0);
        when(productService.getProductById(6L)).thenReturn(available);
        assertThrows(BusinessException.class, () -> orderService.createOrder(order(customer, 6L, 1)));
    }

    @Test
    void updatesStatusAndRejectsUnknownStatus() {
        Order order = new Order();
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        assertEquals(order, orderService.updateOrderStatus(3L, "SHIPPED"));
        assertEquals("SHIPPED", order.getStatus());
        assertThrows(BusinessException.class, () -> orderService.updateOrderStatus(3L, "UNKNOWN"));
    }

    @Test
    void cancelsOrderAndRestoresStockButProtectsTerminalOrders() {
        Product product = product(6L, true, 10);
        Order order = order(new Customer(), 6L, 2);
        order.setStatus("CONFIRMED");
        order.getItems().get(0).setProduct(product);
        when(orderRepository.findById(3L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        orderService.cancelOrder(3L);

        assertEquals("CANCELLED", order.getStatus());
        verify(productService).updateStock(6L, 2);
        assertThrows(BusinessException.class, () -> orderService.cancelOrder(3L));

        order.setStatus("DELIVERED");
        assertThrows(BusinessException.class, () -> orderService.cancelOrder(3L));
    }

    private static Product product(Long id, boolean available, int stock) {
        Product product = new Product();
        product.setId(id);
        product.setName("Widget");
        product.setPrice(new BigDecimal("10.00"));
        product.setStockQuantity(stock);
        product.setAvailable(available);
        return product;
    }

    private static Order order(Customer customer, Long productId, int quantity) {
        customer.setId(5L);
        Order order = new Order();
        order.setCustomer(customer);
        Product product = new Product();
        product.setId(productId);
        OrderItem item = new OrderItem();
        item.setProduct(product);
        item.setQuantity(quantity);
        order.addItem(item);
        return order;
    }
}
