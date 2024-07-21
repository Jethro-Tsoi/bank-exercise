package org.example;

import java.time.Instant;

public class LegacyServer {

    public void reportActivity(int accountId, Instant instant, double amount, double balance) {
        try {
            Thread.sleep(250);
        } catch (InterruptedException e) {
        }

    }
}
