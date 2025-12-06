package com.example.demo.repository;

import com.example.demo.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository interface for Product entity.
 */
@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    List<Product> findByAvailable(boolean available);

    List<Product> findByNameContainingIgnoreCase(String name);

    List<Product> findByStockQuantityLessThan(Integer quantity);
}
