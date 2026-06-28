package com.bank.accountservice.service;

import com.bank.accountservice.dto.AccountResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccountCacheEvictor {

    private final StringRedisTemplate redisTemplate;
    private static final String CACHE_PREFIX = "accounts::";

    void evictRelatedCaches(AccountResponse account) {
        try {
            List<String> patterns = new ArrayList<>();
            // The cache for the account number
            patterns.add(CACHE_PREFIX + "getAccounts::an=" + account.getAccountNumber() + "|*");

            // the cache for the customer name
            patterns.add(CACHE_PREFIX + "getAccounts::*|cn=" + account.getCustomerName() + "|*");

            // the cache for the nickname.
            if (StringUtils.hasText(account.getAccountNickname())) {
                patterns.add(CACHE_PREFIX + "getAccounts::*|nn=" + account.getAccountNickname());
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
}
