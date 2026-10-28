package com.example.demo.controller;

import com.example.demo.model.Order;
import com.example.demo.service.OrderService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController orderController;

    @Test
    void filtersOrdersByCustomerAndStatus() {
        List<Order> orders = List.of(new Order());
        when(orderService.getAllOrders()).thenReturn(orders);
        when(orderService.getOrdersByCustomerId(5L)).thenReturn(orders);
        when(orderService.getOrdersByStatus("PENDING")).thenReturn(orders);

        assertEquals(orders, orderController.getAllOrders(null, null).getBody());
        assertEquals(orders, orderController.getAllOrders(5L, "PENDING").getBody());
        assertEquals(orders, orderController.getAllOrders(null, "PENDING").getBody());

        verify(orderService).getAllOrders();
        verify(orderService).getOrdersByCustomerId(5L);
        verify(orderService).getOrdersByStatus("PENDING");
    }

    @Test
    void supportsOrderLifecycleEndpoints() {
        Order order = new Order();
        when(orderService.getOrderById(3L)).thenReturn(order);
        when(orderService.createOrder(order)).thenReturn(order);
        when(orderService.updateOrderStatus(3L, "SHIPPED")).thenReturn(order);

        assertEquals(HttpStatus.OK, orderController.getOrderById(3L).getStatusCode());
        assertEquals(HttpStatus.CREATED, orderController.createOrder(order).getStatusCode());
        assertEquals(HttpStatus.OK, orderController.updateOrderStatus(
                3L, Map.of("status", "SHIPPED")).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, orderController.cancelOrder(3L).getStatusCode());

        verify(orderService).cancelOrder(3L);
    }
}
