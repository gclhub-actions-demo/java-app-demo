package com.example.demo.service;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Product;
import com.example.demo.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class for Product business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        log.debug("Fetching all products");
        return productRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Product getProductById(Long id) {
        log.debug("Fetching product with id: {}", id);
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
    }

    @Transactional
    public Product createProduct(Product product) {
        log.debug("Creating new product: {}", product.getName());
        return productRepository.save(product);
    }

    @Transactional
    public Product updateProduct(Long id, Product productDetails) {
        log.debug("Updating product with id: {}", id);
        
        Product product = getProductById(id);
        
        product.setName(productDetails.getName());
        product.setDescription(productDetails.getDescription());
        product.setPrice(productDetails.getPrice());
        product.setStockQuantity(productDetails.getStockQuantity());
        product.setAvailable(productDetails.isAvailable());
        
        return productRepository.save(product);
    }

    @Transactional
    public void deleteProduct(Long id) {
        log.debug("Deleting product with id: {}", id);
        Product product = getProductById(id);
        productRepository.delete(product);
    }

    @Transactional(readOnly = true)
    public List<Product> getAvailableProducts() {
        log.debug("Fetching available products");
        return productRepository.findByAvailable(true);
    }

    @Transactional(readOnly = true)
    public List<Product> searchProductsByName(String name) {
        log.debug("Searching products by name: {}", name);
        return productRepository.findByNameContainingIgnoreCase(name);
    }

    @Transactional(readOnly = true)
    public List<Product> getLowStockProducts(Integer threshold) {
        log.debug("Fetching low stock products with threshold: {}", threshold);
        return productRepository.findByStockQuantityLessThan(threshold);
    }

    @Transactional
    public void updateStock(Long productId, Integer quantity) {
        log.debug("Updating stock for product id: {} with quantity: {}", productId, quantity);
        
        Product product = getProductById(productId);
        
        if (product.getStockQuantity() + quantity < 0) {
            throw new BusinessException("Insufficient stock for product: " + product.getName());
        }
        
        product.setStockQuantity(product.getStockQuantity() + quantity);
        productRepository.save(product);
    }
}
