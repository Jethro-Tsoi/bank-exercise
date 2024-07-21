package org.example;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

public class AccountImpl implements Account {
    private static final Logger LOGGER = Logger.getLogger(AccountImpl.class.getName());

    private final int accountId;
    private double balance;
    private final List<AccountEntry> entries;
    private final ReadWriteLock lock;

    public AccountImpl(int accountId) {
        this.accountId = accountId;
        this.balance = 0;
        this.entries = new ArrayList<>();
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public double getBalance() {
        lock.readLock().lock();
        try {
            return balance;
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public AccountEntry[] entries() {
        lock.readLock().lock();
        try {
            return entries.toArray(new AccountEntry[0]);
        } finally {
            lock.readLock().unlock();
        }
    }

    public void addEntry(AccountEntry entry) {
        lock.writeLock().lock();
        try {
            entries.add(entry);
            balance += entry.getAmount();
            LOGGER.info(String.format("Account %d: Added entry %s, new balance: %.2f", accountId, entry, balance));
        } finally {
            lock.writeLock().unlock();
        }
    }

    public int getAccountId() {
        return accountId;
    }
}