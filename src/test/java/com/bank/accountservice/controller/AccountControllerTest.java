package com.bank.accountservice.controller;

import com.bank.accountservice.dto.AccountResponse;
import com.bank.accountservice.dto.CreateAccountRequest;
import com.bank.accountservice.service.AccountService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AccountController.class)
class AccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private AccountService accountService;

    @Test
    void createAccount_returnsCreated() throws Exception {
        // given
        CreateAccountRequest request = new CreateAccountRequest();
        request.setCustomerName("Jane Doe");
        request.setAccountNickname("JaneyD");

        // when
        AccountResponse response = AccountResponse.builder()
                .id(UUID.randomUUID())
                .accountNumber("06-1234-1234567-50")
                .customerName("Jane Doe")
                .accountNickname("JaneyD")
                .createdAt(LocalDateTime.now())
                .build();
        when(accountService.createAccount(any())).thenReturn(response);

        // then
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerName").value("Jane Doe"));
    }

    @Test
    void createAccount_missingCustomerName_returnsBadRequest() throws Exception {
        // given
        CreateAccountRequest request = new CreateAccountRequest();

        // when
        // then
        mockMvc.perform(post("/api/v1/accounts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAccounts_noFilters_returnsBadRequest() throws Exception {
        // when
        mockMvc.perform(get("/api/v1/accounts"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("At least one filter is required: accountNumber, customerName or accountNickname."));
    }

    @Test
    void getAccounts_withCustomerName_returnsOk() throws Exception {
        // given
        AccountResponse item = AccountResponse.builder()
                .id(UUID.randomUUID())
                .accountNumber("06-1234-1234567-50")
                .customerName("John Smith")
                .createdAt(LocalDateTime.now())
                .build();
        when(accountService.getAccounts(isNull(), eq("John"), isNull(), eq(10))).thenReturn(List.of(item));

        // when
        // then
        mockMvc.perform(get("/api/v1/accounts").param("customerName", "John"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerName").value("John Smith"));
    }

    @Test
    void getAccounts_withAllFilters_returnsOk() throws Exception {
        // given
        when(accountService.getAccounts(eq("06-1234-1234567-50"), eq("John"), eq("JD"), eq(10))).thenReturn(List.of());

        // when
        // then
        mockMvc.perform(get("/api/v1/accounts")
                        .param("accountNumber", "06-1234-1234567-50")
                        .param("customerName", "John")
                        .param("accountNickname", "JD"))
                .andExpect(status().isOk());
    }

    @Test
    void getAccounts_withNickname_returnsOk() throws Exception {
        // given
        when(accountService.getAccounts(isNull(), isNull(), eq("JaneyD"), eq(10))).thenReturn(List.of());

        // when
        // then
        mockMvc.perform(get("/api/v1/accounts").param("accountNickname", "JaneyD"))
                .andExpect(status().isOk());
    }

    @Test
    void getAccounts_withCustomLimit_returnsOk() throws Exception {
        // given
        when(accountService.getAccounts(isNull(), eq("John"), isNull(), eq(50))).thenReturn(List.of());

        // when
        // then
        mockMvc.perform(get("/api/v1/accounts")
                        .param("customerName", "John")
                        .param("limit", "50"))
                .andExpect(status().isOk());
    }

    @Test
    void getAccounts_limitBelowMin_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/accounts")
                        .param("customerName", "John")
                        .param("limit", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void getAccounts_limitAboveMax_returnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/accounts")
                        .param("customerName", "John")
                        .param("limit", "101"))
                .andExpect(status().isBadRequest());
    }
}
