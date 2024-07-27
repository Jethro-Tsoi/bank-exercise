package org.example;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

public class BankImpl implements Bank {
    private static final Logger LOGGER = Logger.getLogger(BankImpl.class.getName());

    private final ConcurrentHashMap<Integer, AccountImpl> accounts;

    public BankImpl() {
        this.accounts = new ConcurrentHashMap<>();
    }

    @Override
    public Account getAccountByID(int accountId) {
        return accounts.computeIfAbsent(accountId, id -> {
            LOGGER.info("Creating new account with ID: " + id);
            return new AccountImpl(id);
        });
    }

    public void processTransaction(int accountId, String action, double amount) {
        AccountImpl account = (AccountImpl) getAccountByID(accountId);
        double actualAmount = "withdraw".equalsIgnoreCase(action) ? -amount : amount;
        AccountEntry entry = new AccountEntry(Instant.now(), actualAmount, action);
        account.addEntry(entry);
        LOGGER.info(String.format("Processed transaction for account %d: %s %.2f", accountId, action, amount));
    }
}