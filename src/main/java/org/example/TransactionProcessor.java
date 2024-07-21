package org.example;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransactionProcessor implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(TransactionProcessor.class.getName());
    private static final int BATCH_SIZE = 1000; // Process transactions in batches of 1000

    private final BankImpl bank;
    private final LegacyServer legacyServer;
    private final ExecutorService executorService;

    public TransactionProcessor(BankImpl bank, LegacyServer legacyServer) {
        this.bank = bank;
        this.legacyServer = legacyServer;
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    }

    public void processTransactionFile(Path filePath) {
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            String line;
            reader.readLine(); // Skip header
            List<TransactionData> batch = new ArrayList<>(BATCH_SIZE);
            
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length != 3) {
                    LOGGER.warning("Invalid line format: " + line);
                    continue;
                }
                try {
                    int accountId = Integer.parseInt(parts[0]);
                    String action = parts[1];
                    double amount = Double.parseDouble(parts[2]);

                    batch.add(new TransactionData(accountId, action, amount));
                    
                    if (batch.size() >= BATCH_SIZE) {
                        processBatch(batch);
                        batch.clear();
                    }
                } catch (NumberFormatException e) {
                    LOGGER.warning("Invalid number format in line: " + line);
                }
            }
            
            // Process any remaining transactions
            if (!batch.isEmpty()) {
                processBatch(batch);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error processing transaction file", e);
        }
    }

    private void processBatch(List<TransactionData> batch) {
        for (TransactionData data : batch) {
            bank.processTransaction(data.accountId, data.action, data.amount);
            reportToLegacyServer(data.accountId, data.action, data.amount);
        }
    }

    private void reportToLegacyServer(int accountId, String action, double amount) {
        executorService.submit(() -> {
            try {
                AccountImpl account = (AccountImpl) bank.getAccountByID(accountId);
                double balance = account.getBalance();
                legacyServer.reportActivity(accountId, Instant.now(), amount, balance);
                LOGGER.info(String.format("Reported to legacy server: Account %d, Action: %s, Amount: %.2f, Balance: %.2f", 
                                          accountId, action, amount, balance));
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Error reporting to legacy server", e);
            }
        });
    }

    @Override
    public void close() throws Exception {
        executorService.shutdown();
        if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
            LOGGER.warning("ExecutorService did not terminate in the specified time.");
            executorService.shutdownNow();
        }
    }

    private static class TransactionData {
        final int accountId;
        final String action;
        final double amount;

        TransactionData(int accountId, String action, double amount) {
            this.accountId = accountId;
            this.action = action;
            this.amount = amount;
        }
    }
}