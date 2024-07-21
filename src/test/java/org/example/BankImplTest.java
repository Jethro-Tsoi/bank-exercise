package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BankImplTest {

    private BankImpl bank;

    @BeforeEach
    void setUp() {
        bank = new BankImpl();
    }

    @Test
    void testGetAccountByID() {
        Account account = bank.getAccountByID(1);
        assertNotNull(account);
        assertEquals(0, account.getBalance());
    }

    @Test
    void testGetSameAccountTwice() {
        Account account1 = bank.getAccountByID(1);
        Account account2 = bank.getAccountByID(1);
        assertSame(account1, account2);
    }

    @Test
    void testProcessTransaction() {
        bank.processTransaction(1, "Deposit", 100);
        Account account = bank.getAccountByID(1);
        assertEquals(100, account.getBalance());
        assertEquals(1, account.entries().length);
    }

    @Test
    void testProcessMultipleTransactions() {
        bank.processTransaction(1, "Deposit", 100);
        bank.processTransaction(1, "Withdraw", 50);
        Account account = bank.getAccountByID(1);
        assertEquals(50, account.getBalance());
        assertEquals(2, account.entries().length);
    }
}