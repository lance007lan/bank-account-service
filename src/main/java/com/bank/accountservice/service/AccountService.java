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
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final StringRedisTemplate redisTemplate;
    private static final String CACHE_PREFIX = "accounts::";
    private static final Random RANDOM = new Random();
    private static final String CB = "accountService";

    @CircuitBreaker(name = CB, fallbackMethod = "createAccountFallback")
    public AccountResponse createAccount(CreateAccountRequest request) {
        if (StringUtils.hasText(request.getCustomerNickname())
                && offensiveWordRepository.containsOffensiveWord(request.getCustomerNickname())) {
            throw new ValidationException("Customer nickname contains offensive language.");
        }

        Account account = Account.builder()
                .accountNumber(generateAccountNumber())
                .customerName(request.getCustomerName())
                .customerNickname(request.getCustomerNickname())
                .build();

        AccountResponse response = toResponse(accountRepository.save(account));

        // Whenever a new account is created successfully, the related caches should be invalided.
        evictRelatedCaches(response);
        return response;
    }

    private void evictRelatedCaches(AccountResponse account) {
        try {
            List<String> patterns = new ArrayList<>();
            // The cache for the account number
            patterns.add(CACHE_PREFIX + "getAccounts::an=" + account.getAccountNumber() + "|*");

            // the cache for the customer name
            patterns.add(CACHE_PREFIX + "getAccounts::*|cn=" + account.getCustomerName() + "|*");

            // the cache for the nickname.
            if (StringUtils.hasText(account.getCustomerNickname())) {
                patterns.add(CACHE_PREFIX + "getAccounts::*|nn=" + account.getCustomerNickname());
            }
            patterns.forEach(this::scanAndDelete);
        } catch (Exception e) {
            // Swallow all cache exceptions — a cache failure must not impact the application.
            log.warn("Cache eviction error: {}", e.getMessage());
        }
    }

    private void scanAndDelete(String pattern) {
        ScanOptions options = ScanOptions.scanOptions().match(pattern).count(5).build();
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                redisTemplate.delete(cursor.next());
            }
        }
    }

    @CircuitBreaker(name = CB, fallbackMethod = "getAccountsFallback")
    @Cacheable(value = "accounts", keyGenerator = "accountCacheKeyGenerator")
    public List<AccountResponse> getAccounts(String accountNumber, String customerName, String customerNickname) {
        List<Specification<Account>> specs = new ArrayList<>();

        if (StringUtils.hasText(accountNumber)) {
            specs.add((root, query, cb) -> cb.equal(root.get("accountNumber"), accountNumber));
        }

        if (StringUtils.hasText(customerName)) {
            specs.add((root, query, cb) -> cb.equal(cb.lower(root.get("customerName")), customerName.toLowerCase()));
        }

        if (StringUtils.hasText(customerNickname)) {
            specs.add((root, query, cb) -> cb.like(cb.lower(root.get("customerNickname")), customerNickname.toLowerCase() + "%"));
        }

        return accountRepository.findAll(Specification.allOf(specs)).stream().map(this::toResponse).collect(Collectors.toList());
    }

    private AccountResponse createAccountFallback(CreateAccountRequest request, Exception ex) {
        if (ex instanceof CallNotPermittedException) {
            throw new CircuitBreakerOpenException(CB);
        }
        if (ex instanceof RuntimeException re) throw re;
        throw new RuntimeException(ex);
    }

    private List<AccountResponse> getAccountsFallback(String accountNumber, String customerName, String customerNickname, Exception ex) {
        if (ex instanceof CallNotPermittedException) {
            throw new CircuitBreakerOpenException(CB);
        }
        if (ex instanceof RuntimeException re) throw re;
        throw new RuntimeException(ex);
    }

    private AccountResponse toResponse(Account account) {
        return AccountResponse.builder()
                .id(account.getId())
                .accountNumber(account.getAccountNumber())
                .customerName(account.getCustomerName())
                .customerNickname(account.getCustomerNickname())
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
