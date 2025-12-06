package com.example.demo.service;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Customer;
import com.example.demo.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service class for Customer business logic.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;

    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        log.debug("Fetching all customers");
        return customerRepository.findAll();
    }

    @Transactional(readOnly = true)
    public Customer getCustomerById(Long id) {
        log.debug("Fetching customer with id: {}", id);
        return customerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Customer", id));
    }

    @Transactional
    public Customer createCustomer(Customer customer) {
        log.debug("Creating new customer with email: {}", customer.getEmail());
        
        // Check if email already exists
        if (customerRepository.findByEmail(customer.getEmail()).isPresent()) {
            throw new BusinessException("Customer with email " + customer.getEmail() 
                    + " already exists");
        }
        
        return customerRepository.save(customer);
    }

    @Transactional
    public Customer updateCustomer(Long id, Customer customerDetails) {
        log.debug("Updating customer with id: {}", id);
        
        Customer customer = getCustomerById(id);
        
        // Check if email is being changed and if new email already exists
        if (!customer.getEmail().equals(customerDetails.getEmail())) {
            if (customerRepository.findByEmail(customerDetails.getEmail()).isPresent()) {
                throw new BusinessException("Customer with email " 
                        + customerDetails.getEmail() + " already exists");
            }
        }
        
        customer.setFirstName(customerDetails.getFirstName());
        customer.setLastName(customerDetails.getLastName());
        customer.setEmail(customerDetails.getEmail());
        customer.setPhone(customerDetails.getPhone());
        customer.setActive(customerDetails.isActive());
        
        return customerRepository.save(customer);
    }

    @Transactional
    public void deleteCustomer(Long id) {
        log.debug("Deleting customer with id: {}", id);
        Customer customer = getCustomerById(id);
        customerRepository.delete(customer);
    }

    @Transactional(readOnly = true)
    public List<Customer> getActiveCustomers() {
        log.debug("Fetching active customers");
        return customerRepository.findByActive(true);
    }

    @Transactional(readOnly = true)
    public List<Customer> searchCustomersByLastName(String lastName) {
        log.debug("Searching customers by last name: {}", lastName);
        return customerRepository.findByLastNameContainingIgnoreCase(lastName);
    }
}
