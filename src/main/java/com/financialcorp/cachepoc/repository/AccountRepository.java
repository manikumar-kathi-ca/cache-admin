package com.financialcorp.cachepoc.repository;

import com.financialcorp.cachepoc.entity.Account;
import java.math.BigDecimal;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AccountRepository extends JpaRepository<Account, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Account a set a.balance = :balance where a.id = :id")
    int updateBalance(@Param("id") Long id, @Param("balance") BigDecimal balance);
}
