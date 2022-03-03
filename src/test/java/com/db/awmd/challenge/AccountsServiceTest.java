package com.db.awmd.challenge;

import static com.db.awmd.challenge.constants.ExceptionConstants.*;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.*;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.dto.BalanceTransfer;
import com.db.awmd.challenge.exception.*;
import com.db.awmd.challenge.service.AccountsService;
import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
public class AccountsServiceTest {

  @Autowired
  private AccountsService accountsService;

  @Test
  public void addAccount() throws Exception {
    Account account = new Account("Id-123");
    account.setBalance(new BigDecimal(1000));
    this.accountsService.createAccount(account);

    assertThat(this.accountsService.getAccount("Id-123")).isEqualTo(account);
  }

  @Test
  public void addAccount_failsOnDuplicateId() throws Exception {
    String uniqueId = "Id-" + System.currentTimeMillis();
    Account account = new Account(uniqueId);
    this.accountsService.createAccount(account);

    try {
      this.accountsService.createAccount(account);
      fail("Should have failed when adding duplicate account");
    } catch (DuplicateAccountIdException ex) {
      assertThat(ex.getMessage()).isEqualTo("Account id " + uniqueId + " already exists!");
    }

  }

  @Test
  public void transfer() throws InterruptedException {
    String accountId1 = "ABCD" + System.currentTimeMillis();
    Thread.sleep(1000);
    String accountId2 = "ABCD" + System.currentTimeMillis();

    Account account1 = new Account(accountId1);
    account1.setBalance(BigDecimal.valueOf(1000));
    Account account2 = new Account(accountId2);

    this.accountsService.createAccount(account1);
    this.accountsService.createAccount(account2);

    BalanceTransfer balanceTransfer = new BalanceTransfer();
    balanceTransfer.setAccountFrom(account1.getAccountId());
    balanceTransfer.setAccountTo(account2.getAccountId());
    balanceTransfer.setAmount(BigDecimal.valueOf(500));

    this.accountsService.balanceTransfer(balanceTransfer);
    assertThat(account2.getBalance()).isEqualTo(BigDecimal.valueOf(500));
  }

  @Test
  public void insufficientBalanceTransfer() throws InterruptedException {
    String accountId1 = "ABCD" + System.currentTimeMillis();
    Thread.sleep(1000);
    String accountId2 = "ABCD" + System.currentTimeMillis();

    Account account1 = new Account(accountId1);
    Account account2 = new Account(accountId2);

    this.accountsService.createAccount(account1);
    this.accountsService.createAccount(account2);

    BalanceTransfer balanceTransfer = new BalanceTransfer();
    balanceTransfer.setAccountFrom(account1.getAccountId());
    balanceTransfer.setAccountTo(account2.getAccountId());
    balanceTransfer.setAmount(BigDecimal.valueOf(500));
    try {
      this.accountsService.balanceTransfer(balanceTransfer);
    }catch (InsufficientBalanceException e){
      assertThat(e.getMessage()).isEqualTo(INSUFFICIENT_BALANCE_EXCEPTION);
    }
  }

  @Test
  public void negativeBalanceTransfer() throws InterruptedException {
    String accountId1 = "ABCD" + System.currentTimeMillis();
    Thread.sleep(1000);
    String accountId2 = "ABCD" + System.currentTimeMillis();

    Account account1 = new Account(accountId1);
    Account account2 = new Account(accountId2);

    this.accountsService.createAccount(account1);
    this.accountsService.createAccount(account2);

    BalanceTransfer balanceTransfer = new BalanceTransfer();
    balanceTransfer.setAccountFrom(account1.getAccountId());
    balanceTransfer.setAccountTo(account2.getAccountId());
    balanceTransfer.setAmount(BigDecimal.valueOf(-500));
    try {
      this.accountsService.balanceTransfer(balanceTransfer);
    }catch (BadAmountFormatException e){
      assertThat(e.getMessage()).isEqualTo(BAD_AMOUNT_TRANSFER_DENIAL_EXCEPTION);
    }
  }

  @Test
  public void senderAndReceiverSameTransfer() {
    String accountId1 = "ABCD" + System.currentTimeMillis();

    Account account1 = new Account(accountId1);

    this.accountsService.createAccount(account1);

    BalanceTransfer balanceTransfer = new BalanceTransfer();
    balanceTransfer.setAccountFrom(account1.getAccountId());
    balanceTransfer.setAccountTo(account1.getAccountId());
    balanceTransfer.setAmount(BigDecimal.valueOf(500));
    try {
      this.accountsService.balanceTransfer(balanceTransfer);
    }catch (IdenticalAccountTransferDenialException e){
      assertThat(e.getMessage()).isEqualTo(IDENTICAL_ACCOUNT_TRANSFER_DENIAL_EXCEPTION);
    }
  }

  @Test
  public void insufficientDataTransfer() throws InterruptedException {
    String accountId1 = "ABCD" + System.currentTimeMillis();
    Thread.sleep(1000);
    String accountId2 = "ABCD" + System.currentTimeMillis();

    Account account1 = new Account(accountId1);
    Account account2 = new Account(accountId2);

    this.accountsService.createAccount(account1);
    this.accountsService.createAccount(account2);

    BalanceTransfer balanceTransfer = new BalanceTransfer();
    balanceTransfer.setAccountFrom("");
    balanceTransfer.setAccountTo(account2.getAccountId());
    balanceTransfer.setAmount(BigDecimal.valueOf(500));
    try {
      this.accountsService.balanceTransfer(balanceTransfer);
    }catch (InsufficientDataException e){
      assertThat(e.getMessage()).isEqualTo(INSUFFICIENT_DATA_EXCEPTION);
    }
  }

  @Test
  public void concurrentAccountTransfers() throws InterruptedException {
    /* we will try testing concurrency as much as possible even though we cannot guarantee it through a unit test */
    String accountId1 = "ABCD" + System.currentTimeMillis();
    Thread.sleep(1000);
    String accountId2 = "ABCD" + System.currentTimeMillis();

    Account account1 = new Account(accountId1);
    Account account2 = new Account(accountId2);
    account1.setBalance(BigDecimal.valueOf(1000));

    this.accountsService.createAccount(account1);
    this.accountsService.createAccount(account2);

    Runnable run=()->{
      BalanceTransfer balanceTransfer = new BalanceTransfer();
      balanceTransfer.setAccountFrom(account1.getAccountId());
      balanceTransfer.setAccountTo(account2.getAccountId());
      balanceTransfer.setAmount(BigDecimal.valueOf(1000));

      this.accountsService.balanceTransfer(balanceTransfer);

      System.out.println(account1.getBalance());
      System.out.println(account2.getBalance());
    };

    Thread t1 = new Thread(run);
    t1.setName("thread-1");
    Thread t2 = new Thread(run);
    t2.setName("thread-2");

    t1.start();
    t2.start();

    t1.join();
    t2.join();

    assertThat(account2.getBalance()).isEqualTo(BigDecimal.valueOf(1000));
    assertThat(account1.getBalance()).isEqualTo(BigDecimal.ZERO);
  }
}
