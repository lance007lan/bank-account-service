package com.bank.accountservice.service;

import com.bank.accountservice.dto.AccountResponse;
import com.bank.accountservice.dto.CreateAccountRequest;
import com.bank.accountservice.entity.Account;
import com.bank.accountservice.exception.CircuitBreakerOpenException;
import com.bank.accountservice.exception.ValidationException;
import com.bank.accountservice.repository.AccountRepository;
import com.bank.accountservice.repository.OffensiveWordRepository;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final OffensiveWordRepository offensiveWordRepository;
    private final AccountCacheEvictor cacheEvictor;
    private static final Random RANDOM = new Random();
    private static final String CB = "accountService";

    @CircuitBreaker(name = CB, fallbackMethod = "createAccountFallback")
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (StringUtils.hasText(request.getAccountNickname())
                && offensiveWordRepository.containsOffensiveWord(request.getAccountNickname())) {
            throw new ValidationException("Customer nickname contains offensive language.");
        }

        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .customerName(request.getCustomerName())
                .accountNickname(request.getAccountNickname())
                .build();

        AccountResponse response = toResponse(accountRepository.save(account));

        // Whenever a new account is created successfully, the related caches should be invalided.
        cacheEvictor.evictRelatedCaches(response);
        return response;
    }

    @CircuitBreaker(name = CB, fallbackMethod = "getAccountsFallback")
    @Cacheable(value = "accounts", keyGenerator = "accountCacheKeyGenerator")
    public List<AccountResponse> getAccounts(String accountNumber, String customerName, String accountNickname) {
        List<Specification<Account>> specs = new ArrayList<>();

        if (StringUtils.hasText(accountNumber)) {
            specs.add((root, query, cb) -> cb.equal(root.get("accountNumber"), accountNumber));
        }

        if (StringUtils.hasText(customerName)) {
            specs.add((root, query, cb) -> cb.equal(cb.lower(root.get("customerName")), customerName.toLowerCase()));
        }

        if (StringUtils.hasText(accountNickname)) {
            specs.add((root, query, cb) -> cb.like(cb.lower(root.get("accountNickname")), accountNickname.toLowerCase() + "%"));
        }

        return accountRepository.findAll(Specification.allOf(specs)).stream().map(this::toResponse).collect(Collectors.toList());
    }

    private AccountResponse createAccountFallback(CreateAccountRequest request, Exception ex) {
        throw handleFallback(ex);
    }

    private List<AccountResponse> getAccountsFallback(String accountNumber, String customerName, String accountNickname, Exception ex) {
        throw handleFallback(ex);
    }

    private RuntimeException handleFallback(Exception ex) {
        if (ex instanceof CallNotPermittedException) {
            throw new CircuitBreakerOpenException(CB);
        }
        if (ex instanceof RuntimeException re) return re;
        return new RuntimeException(ex);
    }

    private AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .customerName(account.getCustomerName())
                .accountNickname(account.getAccountNickname())
                .createdAt(account.getCreatedAt())
                .build();
    }

    private String generateAccountNumber() {
        // NZ bank account format: BB-BBBB-AAAAAAA-SS
        int bank    = RANDOM.nextInt(30) + 1;
        int branch  = RANDOM.nextInt(9000) + 1000;
        int account = RANDOM.nextInt(9_000_000) + 1_000_000;
        int suffix  = RANDOM.nextInt(90) + 10;
        return String.format("%02d-%04d-%07d-%02d", bank, branch, account, suffix);
    }
}
