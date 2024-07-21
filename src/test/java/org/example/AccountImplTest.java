package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class AccountImplTest {

    private AccountImpl account;

    @BeforeEach
    void setUp() {
        account = new AccountImpl(1);
    }

    @Test
    void testInitialBalance() {
        assertEquals(0, account.getBalance());
    }

    @Test
    void testAddEntry() {
        AccountEntry entry = new AccountEntry(Instant.now(), 100, "Deposit");
        account.addEntry(entry);
        assertEquals(100, account.getBalance());
        assertEquals(1, account.entries().length);
    }

    @Test
    void testMultipleEntries() {
        account.addEntry(new AccountEntry(Instant.now(), 100, "Deposit"));
        account.addEntry(new AccountEntry(Instant.now(), -50, "Withdraw"));
        assertEquals(50, account.getBalance());
        assertEquals(2, account.entries().length);
    }

}