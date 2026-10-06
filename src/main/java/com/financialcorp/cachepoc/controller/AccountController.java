package com.financialcorp.cachepoc.controller;

import com.financialcorp.cachepoc.dto.AccountRequest;
import com.financialcorp.cachepoc.dto.BalanceUpdateRequest;
import com.financialcorp.cachepoc.entity.Account;
import com.financialcorp.cachepoc.service.AccountService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/accounts")
@Tag(name = "Accounts", description = "CRUD with Redis write-through cache")
public class AccountController {

    private final AccountService accountService;

    public AccountController(AccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping
    @Operation(summary = "List accounts (database, not cache)")
    public List<Account> list() {
        return accountService.listAccounts();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get account by id (Redis first, then DB)")
    public Account get(@PathVariable Long id) {
        return accountService.getAccount(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create account and put into Redis")
    public Account create(@Valid @RequestBody AccountRequest request) {
        return accountService.createAccount(request);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update account and refresh Redis")
    public Account update(@PathVariable Long id, @Valid @RequestBody AccountRequest request) {
        return accountService.updateAccount(id, request);
    }

    @PatchMapping("/{id}/balance")
    @Operation(summary = "SQL balance update; cache is refreshed from DB")
    public Account updateBalanceFromDb(@PathVariable Long id, @Valid @RequestBody BalanceUpdateRequest request) {
        return accountService.updateBalanceInDatabase(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete account and evict Redis key")
    public void delete(@PathVariable Long id) {
        accountService.deleteAccount(id);
    }
}
