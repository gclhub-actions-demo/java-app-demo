package com.example.demo.service;

import com.example.demo.exception.BusinessException;
import com.example.demo.exception.ResourceNotFoundException;
import com.example.demo.model.Customer;
import com.example.demo.repository.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerServiceTest {

    @Mock
    private CustomerRepository customerRepository;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(1L);
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john.doe@example.com");
        customer.setPhone("555-1234");
        customer.setActive(true);
    }

    @Test
    void testGetAllCustomers() {
        List<Customer> customers = Arrays.asList(customer);
        when(customerRepository.findAll()).thenReturn(customers);

        List<Customer> result = customerService.getAllCustomers();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(customerRepository, times(1)).findAll();
    }

    @Test
    void testGetCustomerById() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        Customer result = customerService.getCustomerById(1L);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        verify(customerRepository, times(1)).findById(1L);
    }

    @Test
    void testGetCustomerByIdNotFound() {
        when(customerRepository.findById(1L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> {
            customerService.getCustomerById(1L);
        });
    }

    @Test
    void testCreateCustomer() {
        when(customerRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        Customer result = customerService.createCustomer(customer);

        assertNotNull(result);
        assertEquals("John", result.getFirstName());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    void testCreateCustomerWithDuplicateEmail() {
        when(customerRepository.findByEmail(anyString())).thenReturn(Optional.of(customer));

        assertThrows(BusinessException.class, () -> {
            customerService.createCustomer(customer);
        });
    }

    @Test
    void testUpdateCustomer() {
        Customer updatedCustomer = new Customer();
        updatedCustomer.setFirstName("Jane");
        updatedCustomer.setLastName("Smith");
        updatedCustomer.setEmail("jane.smith@example.com");
        updatedCustomer.setPhone("555-5678");
        updatedCustomer.setActive(true);

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(customerRepository.save(any(Customer.class))).thenReturn(updatedCustomer);

        Customer result = customerService.updateCustomer(1L, updatedCustomer);

        assertNotNull(result);
        assertEquals("Jane", result.getFirstName());
        verify(customerRepository, times(1)).save(any(Customer.class));
    }

    @Test
    void testDeleteCustomer() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        customerService.deleteCustomer(1L);

        verify(customerRepository, times(1)).delete(customer);
    }

    @Test
    void testGetActiveCustomers() {
        List<Customer> customers = Arrays.asList(customer);
        when(customerRepository.findByActive(true)).thenReturn(customers);

        List<Customer> result = customerService.getActiveCustomers();

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(customerRepository, times(1)).findByActive(true);
    }
}
