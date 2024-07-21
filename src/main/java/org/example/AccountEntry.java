package org.example;

import java.time.Instant;

//TODO
public class AccountEntry {
    private final Instant timestamp;
    private final double amount;
    private final String action;

    public AccountEntry(Instant timestamp, double amount, String action) {
        this.timestamp = timestamp;
        this.amount = amount;
        this.action = action;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public double getAmount() {
        return amount;
    }

    public String getAction() {
        return action;
    }
}