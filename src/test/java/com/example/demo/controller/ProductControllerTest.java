package com.example.demo.controller;

import com.example.demo.model.Product;
import com.example.demo.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    @Test
    void filtersProductsByAvailabilityNameAndStock() {
        List<Product> products = List.of(new Product());
        when(productService.getAllProducts()).thenReturn(products);
        when(productService.getAvailableProducts()).thenReturn(products);
        when(productService.searchProductsByName("widget")).thenReturn(products);
        when(productService.getLowStockProducts(4)).thenReturn(products);

        assertEquals(products, productController.getAllProducts(null, null, null).getBody());
        assertEquals(products, productController.getAllProducts(true, null, null).getBody());
        assertEquals(products, productController.getAllProducts(null, "widget", null).getBody());
        assertEquals(products, productController.getAllProducts(null, null, 4).getBody());

        verify(productService).getAllProducts();
        verify(productService).getAvailableProducts();
        verify(productService).searchProductsByName("widget");
        verify(productService).getLowStockProducts(4);
    }

    @Test
    void supportsProductLifecycleEndpoints() {
        Product product = new Product();
        when(productService.getProductById(2L)).thenReturn(product);
        when(productService.createProduct(product)).thenReturn(product);
        when(productService.updateProduct(2L, product)).thenReturn(product);

        assertEquals(HttpStatus.OK, productController.getProductById(2L).getStatusCode());
        assertEquals(HttpStatus.CREATED, productController.createProduct(product).getStatusCode());
        assertEquals(HttpStatus.OK, productController.updateProduct(2L, product).getStatusCode());
        assertEquals(HttpStatus.NO_CONTENT, productController.deleteProduct(2L).getStatusCode());

        verify(productService).deleteProduct(2L);
    }
}
