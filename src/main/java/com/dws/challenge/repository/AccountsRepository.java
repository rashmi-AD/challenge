package com.dws.challenge.repository;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.DuplicateAccountIdException;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface AccountsRepository {

  void createAccount(Account account) throws DuplicateAccountIdException;

  Account getAccount(String accountId);

  void clearAccounts();

  void save(Account fromAccount);
}
