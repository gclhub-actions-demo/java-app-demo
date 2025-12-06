package com.example.demo.controller;

import com.example.demo.Application;
import com.example.demo.model.Customer;
import com.example.demo.repository.CustomerRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = Application.class)
@AutoConfigureMockMvc
class CustomerControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        customerRepository.deleteAll();
    }

    @Test
    void testCreateCustomer() throws Exception {
        Customer customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john.doe@example.com");
        customer.setPhone("555-1234");

        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customer)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.firstName").value("John"))
                .andExpect(jsonPath("$.email").value("john.doe@example.com"));
    }

    @Test
    void testGetCustomerById() throws Exception {
        Customer customer = new Customer();
        customer.setFirstName("Jane");
        customer.setLastName("Smith");
        customer.setEmail("jane.smith@example.com");
        customer.setActive(true);
        Customer savedCustomer = customerRepository.save(customer);

        mockMvc.perform(get("/api/customers/" + savedCustomer.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Jane"))
                .andExpect(jsonPath("$.email").value("jane.smith@example.com"));
    }

    @Test
    void testGetAllCustomers() throws Exception {
        Customer customer1 = new Customer();
        customer1.setFirstName("John");
        customer1.setLastName("Doe");
        customer1.setEmail("john@example.com");
        customer1.setActive(true);
        customerRepository.save(customer1);

        Customer customer2 = new Customer();
        customer2.setFirstName("Jane");
        customer2.setLastName("Smith");
        customer2.setEmail("jane@example.com");
        customer2.setActive(true);
        customerRepository.save(customer2);

        mockMvc.perform(get("/api/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2));
    }

    @Test
    void testUpdateCustomer() throws Exception {
        Customer customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john.doe@example.com");
        customer.setActive(true);
        Customer savedCustomer = customerRepository.save(customer);

        savedCustomer.setFirstName("Johnny");
        savedCustomer.setPhone("555-9999");

        mockMvc.perform(put("/api/customers/" + savedCustomer.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(savedCustomer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.firstName").value("Johnny"))
                .andExpect(jsonPath("$.phone").value("555-9999"));
    }

    @Test
    void testDeleteCustomer() throws Exception {
        Customer customer = new Customer();
        customer.setFirstName("John");
        customer.setLastName("Doe");
        customer.setEmail("john.doe@example.com");
        customer.setActive(true);
        Customer savedCustomer = customerRepository.save(customer);

        mockMvc.perform(delete("/api/customers/" + savedCustomer.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/customers/" + savedCustomer.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateCustomerWithInvalidData() throws Exception {
        Customer customer = new Customer();
        customer.setFirstName("");
        customer.setEmail("invalid-email");

        mockMvc.perform(post("/api/customers")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(customer)))
                .andExpect(status().isBadRequest());
    }
}
