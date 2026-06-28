package com.bank.accountservice.service;

import com.bank.accountservice.dto.AccountResponse;
import com.bank.accountservice.dto.CreateAccountRequest;
import com.bank.accountservice.repository.AccountRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Testcontainers
class AccountServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @Container
    @SuppressWarnings("resource")
    static GenericContainer<?> redis = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
    }

    @Autowired
    private AccountService accountService;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private CircuitBreakerRegistry circuitBreakerRegistry;

    @BeforeEach
    void setUp() {
        accountRepository.deleteAll();
        circuitBreakerRegistry.circuitBreaker("accountService").reset();
        redisTemplate.execute((RedisCallback<Void>) conn -> {
            conn.serverCommands().flushAll();
            return null;
        });
    }

    private CreateAccountRequest request(String customerName, String nickname) {
        CreateAccountRequest req = new CreateAccountRequest();
        req.setCustomerName(customerName);
        req.setAccountNickname(nickname);
        return req;
    }

    private String cacheKey(String accountNumber, String customerName, String nickname) {
        return "accounts::getAccounts::an=" + (accountNumber != null ? accountNumber : "_")
                + "|cn=" + (customerName != null ? customerName : "_")
                + "|nn=" + (nickname != null ? nickname : "_");
    }

    @Test
    void createAccount_persistsToDatabase() {
        // given
        AccountResponse response = accountService.createAccount(request("Alice Smith", "AliceS"));

        // when
        // then
        assertThat(accountRepository.findById(response.getId())).isPresent().hasValueSatisfying(account -> {
            assertThat(account.getCustomerName()).isEqualTo("Alice Smith");
            assertThat(account.getAccountNickname()).isEqualTo("AliceS");
            assertThat(account.getAccountNumber()).isNotBlank();
        });
    }

    @Test
    void getAccounts_byVariousFilters_returnsMatchAndPopulatesCache() {
        // given
        AccountResponse created = accountService.createAccount(request("Bob Jones", "BobbyJ"));

        // when search by customer name (exact, case-insensitive)
        List<AccountResponse> byName = accountService.getAccounts(null, "bob jones", null);
        assertThat(byName).hasSize(1).first().extracting(AccountResponse::getCustomerName).isEqualTo("Bob Jones");
        assertThat(redisTemplate.hasKey(cacheKey(null, "bob jones", null))).isTrue();

        // when search by account number (exact)
        List<AccountResponse> byNumber = accountService.getAccounts(created.getAccountNumber(), null, null);
        assertThat(byNumber).hasSize(1).first().extracting(AccountResponse::getAccountNumber).isEqualTo(created.getAccountNumber());
        assertThat(redisTemplate.hasKey(cacheKey(created.getAccountNumber(), null, null))).isTrue();

        // when search by nickname (prefix)
        List<AccountResponse> byNickname = accountService.getAccounts(null, null, "Bobby");
        assertThat(byNickname).hasSize(1).first().extracting(AccountResponse::getAccountNickname).isEqualTo("BobbyJ");
        assertThat(redisTemplate.hasKey(cacheKey(null, null, "Bobby"))).isTrue();

        // when search combined filters
        List<AccountResponse> combined = accountService.getAccounts(created.getAccountNumber(), "Bob Jones", "BobbyJ");
        assertThat(combined).hasSize(1);
        assertThat(redisTemplate.hasKey(cacheKey(created.getAccountNumber(), "Bob Jones", "BobbyJ"))).isTrue();
    }

    @Test
    void createAccount_failsWhenCustomerExceeds5Accounts() {
        // given we already have 5 accounts from this customer
        for (int i = 0; i < 5; i++) {
            accountService.createAccount(request("Carol White", null));
        }

        // when creating one more
        // then it will trigger an exception
        assertThatThrownBy(() -> accountService.createAccount(request("Carol White", null)))
                .satisfies(ex -> {
                    Throwable cause = ex;
                    while (cause.getCause() != null) cause = cause.getCause();
                    assertThat(cause.getMessage()).contains("maximum of 5 accounts");
                });
    }
}
