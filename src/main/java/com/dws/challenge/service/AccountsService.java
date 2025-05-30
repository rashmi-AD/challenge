package com.dws.challenge.service;

import com.dws.challenge.domain.Account;
import com.dws.challenge.exception.AccountException;
import com.dws.challenge.repository.AccountsRepository;
import com.dws.challenge.request.TransferAmountRequest;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;

import java.math.BigDecimal;

/**
 * AccountService class responsible to perform operation related to Account.
 *
 * @author
 * @since 23 May 2025
 */
@Service
@Slf4j
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;

  @Getter
  private final NotificationService notificationService;
  @Autowired
  public AccountsService(AccountsRepository accountsRepository, NotificationService notificationService) {
    this.accountsRepository = accountsRepository;
    this.notificationService = notificationService;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) {
    return this.accountsRepository.getAccount(accountId);
  }

  /**
   * Updates the From account by subtracting the amount and updates the ToAccount by adding the amount .
   *
   * @param transferAmountReq - consists of From and To account Id and amount to be transferred.
   * @return List<OrderBookResponse>
   */
  @Transactional
  public String transferAmount(TransferAmountRequest transferAmountReq) {
    validateRequest(transferAmountReq);

    try {
      // lock in consistent order to avoid deadlocks
      //locking in a sorted order (e.g., always lock the "smaller" account number first),
      // you avoid circular wait conditions — which are the main cause of deadlocks in concurrent systems.
      String firstLock;
      String secondLock;

      if (transferAmountReq.getAccountFrom().compareTo(transferAmountReq.getAccountTo()) < 0) {
        firstLock = transferAmountReq.getAccountFrom();
        secondLock = transferAmountReq.getAccountTo();
      } else {
        firstLock = transferAmountReq.getAccountTo();
        secondLock = transferAmountReq.getAccountFrom();
      }

      Account first = (Account) this.accountsRepository.getAccount(firstLock);
      if(ObjectUtils.isEmpty(first))
              throw new AccountException("Account not found: " + firstLock);

      Account second = (Account) this.accountsRepository.getAccount(secondLock);
      if(ObjectUtils.isEmpty(second))
              throw new AccountException("Account not found: " + secondLock);

      // Identify which is from and which is to
      Account fromAccount = transferAmountReq.getAccountFrom().equals(first.getAccountId()) ? first : second;
      Account toAccount = transferAmountReq.getAccountTo().equals(first.getAccountId()) ? first : second;

      if (fromAccount.getBalance().compareTo(transferAmountReq.getAmount()) < 0) {
        throw new IllegalStateException("Insufficient Funds");
      }

      fromAccount.setBalance(fromAccount.getBalance().subtract(transferAmountReq.getAmount()));
      toAccount.setBalance(toAccount.getBalance().add(transferAmountReq.getAmount()));

      this.accountsRepository.save(fromAccount);
      this.accountsRepository.save(toAccount);

      this.notificationService.notifyAboutTransfer(fromAccount,transferAmountReq.getAmount()+" transferred to account "+transferAmountReq.getAccountTo());
      this.notificationService.notifyAboutTransfer(toAccount,transferAmountReq.getAmount()+" debited from account "+transferAmountReq.getAccountFrom());

      return "Amount transferred Successfully.";
    }catch(Exception exp){
      log.error("Exception occured while updating the amount to account, msg={}",exp.getMessage());
      return "Amount Transfer failed";
    }
  }


  private void validateRequest(TransferAmountRequest transferAmountReq) {
    if (transferAmountReq.getAccountFrom().equals(transferAmountReq.getAccountTo())){
      log.error("Account: AccountFrom and AccountTo is same");
      throw new AccountException("AccountFrom and AccountTo are same.");
    }
    if(transferAmountReq.getAmount().compareTo(new BigDecimal(0)) < 0){
      log.error("Account: Amount is less than 0");
      throw new AccountException("Amount is less than 0.");
    }
  }


}
