package com.financialcorp.cachepoc.service;

import com.financialcorp.cachepoc.dto.AccountRequest;
import com.financialcorp.cachepoc.dto.BalanceUpdateRequest;
import com.financialcorp.cachepoc.entity.Account;
import com.financialcorp.cachepoc.repository.AccountRepository;
import java.util.List;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccountService {

    public static final String ACCOUNTS_CACHE = CacheSyncService.ACCOUNTS_CACHE;

    private final AccountRepository accountRepository;
    private final CacheSyncService cacheSyncService;

    public AccountService(AccountRepository accountRepository, CacheSyncService cacheSyncService) {
        this.accountRepository = accountRepository;
        this.cacheSyncService = cacheSyncService;
    }

    @Cacheable(cacheNames = ACCOUNTS_CACHE, key = "#id")
    @Transactional(readOnly = true)
    public Account getAccount(Long id) {
        return accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
    }

    @Transactional(readOnly = true)
    public List<Account> listAccounts() {
        return accountRepository.findAll();
    }

    @CachePut(cacheNames = ACCOUNTS_CACHE, key = "#result.id")
    @Transactional
    public Account createAccount(AccountRequest request) {
        Account account = new Account(
                request.getAccountNumber(),
                request.getOwnerName(),
                request.getOrganizationName(),
                request.getBalance());
        return accountRepository.save(account);
    }

    @CachePut(cacheNames = ACCOUNTS_CACHE, key = "#id")
    @Transactional
    public Account updateAccount(Long id, AccountRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        account.setAccountNumber(request.getAccountNumber());
        account.setOwnerName(request.getOwnerName());
        account.setOrganizationName(request.getOrganizationName());
        account.setBalance(request.getBalance());
        return accountRepository.save(account);
    }

    @CachePut(cacheNames = ACCOUNTS_CACHE, key = "#id")
    @Transactional
    public Account updateBalanceInDatabase(Long id, BalanceUpdateRequest request) {
        int updated = accountRepository.updateBalance(id, request.getBalance());
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
        }
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found"));
        cacheSyncService.putAccount(account);
        return account;
    }

    @CacheEvict(cacheNames = ACCOUNTS_CACHE, key = "#id")
    @Transactional
    public void deleteAccount(Long id) {
        if (!accountRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Account not found");
        }
        accountRepository.deleteById(id);
    }
}
