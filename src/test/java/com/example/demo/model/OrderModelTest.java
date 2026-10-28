package com.example.demo.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class OrderModelTest {

    @Test
    void addingAndRemovingItemsMaintainsBothSidesOfRelationship() {
        Order order = new Order();
        OrderItem item = new OrderItem();

        order.addItem(item);

        assertEquals(1, order.getItems().size());
        assertSame(order, item.getOrder());

        order.removeItem(item);

        assertEquals(0, order.getItems().size());
        assertNull(item.getOrder());
    }
}
