package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.dto.BalanceTransfer;
import com.db.awmd.challenge.exception.*;
import com.db.awmd.challenge.repository.AccountsRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

import static com.db.awmd.challenge.constants.ExceptionConstants.*;
import static com.db.awmd.challenge.constants.NotificationConstants.*;

@Service
@Slf4j
public class AccountsService {

  @Getter
  private final AccountsRepository accountsRepository;

  private final NotificationService notificationService;

  @Autowired
  public AccountsService(AccountsRepository accountsRepository,NotificationService notificationService) {
    this.accountsRepository = accountsRepository;
    this.notificationService = notificationService;
  }

  public void createAccount(Account account) {
    this.accountsRepository.createAccount(account);
  }

  public Account getAccount(String accountId) throws AccountNotFoundException {
    final Account filteredAccount = this.accountsRepository.getAccount(accountId);
    return Optional.ofNullable(filteredAccount).orElseThrow(() -> new AccountNotFoundException(ACCOUNT_NOT_FOUND_EXCEPTION+accountId));
  }

  public void balanceTransfer(BalanceTransfer balanceTransfer) throws InsufficientBalanceException, IdenticalAccountTransferDenialException {

    Account sender = null;
    Account receiver = null;

    try {
      sender = this.getAccount(balanceTransfer.getAccountFrom());
      receiver = this.getAccount(balanceTransfer.getAccountTo());
    }catch (AccountNotFoundException e){
      throw new InsufficientDataException(INSUFFICIENT_DATA_EXCEPTION);
    }

    /* If sender and receiver are same, throw exception */
    if(sender.equals(receiver)){
      throw new IdenticalAccountTransferDenialException(IDENTICAL_ACCOUNT_TRANSFER_DENIAL_EXCEPTION);
    }

    /* If amount to transfer should not be negative, else throw exception */
    if(balanceTransfer.getAmount().compareTo(BigDecimal.ZERO)<0){
      throw new BadAmountFormatException(BAD_AMOUNT_TRANSFER_DENIAL_EXCEPTION);
    }

    /* To avoid deadlock, always acquire and release locks in same order for different threads */
    Object lock1 = sender.getAccountId().compareTo(receiver.getAccountId()) > 0 ? sender : receiver;
    Object lock2 = lock1.equals(sender)?receiver:sender;

    synchronized (lock1) {
      log.info("applying lock1:"+((Account)lock1).getAccountId());
      synchronized (lock2) {
        log.info("applying lock2:"+((Account)lock2).getAccountId());

        updateBalance(sender,balanceTransfer.getAmount().negate());
        updateBalance(receiver,balanceTransfer.getAmount());

        /* On Success, Trigger notification */
        notificationService.notifyAboutTransfer(sender, IN_CURRENCY+balanceTransfer.getAmount()+AMOUNT_DEBITED+sender.getBalance());
        notificationService.notifyAboutTransfer(receiver, IN_CURRENCY+balanceTransfer.getAmount()+AMOUNT_CREDITED+receiver.getBalance());

      }
    }
  }

  private void updateBalance(Account account,BigDecimal amount){
    BigDecimal updatedAmount = account.getBalance().add(amount);
    /* If sender's balance is less than the transfer amount, throw exception */
    if(updatedAmount.compareTo(BigDecimal.ZERO)<0){
      throw new InsufficientBalanceException(INSUFFICIENT_BALANCE_EXCEPTION);
    }
    account.setBalance(updatedAmount);
  }

}
