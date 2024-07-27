package org.example;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class TransactionProcessor implements AutoCloseable {
    private static final Logger LOGGER = Logger.getLogger(TransactionProcessor.class.getName());
    private static final int CHUNK_SIZE = 1000;

    private final BankImpl bank;
    private final LegacyServer legacyServer;
    private final ExecutorService executorService;
    private final BlockingQueue<Runnable> reportingQueue;

    public TransactionProcessor(BankImpl bank, LegacyServer legacyServer) {
        this.bank = bank;
        this.legacyServer = legacyServer;
        this.executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.reportingQueue = new LinkedBlockingQueue<>();
        startReportingThread();
    }

    private void startReportingThread() {
        Thread reportingThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Runnable reportTask = reportingQueue.take();
                    reportTask.run();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });
        reportingThread.setDaemon(true);
        reportingThread.start();
    }

    public void processTransactionFile(Path filePath) throws IOException {
        try (BufferedReader reader = Files.newBufferedReader(filePath)) {
            reader.readLine(); // Skip header
            List<TransactionData> chunk = new ArrayList<>(CHUNK_SIZE);
            String line;

            while ((line = reader.readLine()) != null) {
                TransactionData transaction = parseTransaction(line);
                if (transaction != null) {
                    chunk.add(transaction);

                    if (chunk.size() >= CHUNK_SIZE) {
                        processChunk(chunk);
                        chunk.clear();
                    }
                }
            }

            // Process any remaining transactions
            if (!chunk.isEmpty()) {
                processChunk(chunk);
            }
        }
    }

    private TransactionData parseTransaction(String line) {
        String[] parts = line.split(",");
        if (parts.length != 3) {
            LOGGER.warning("Invalid line format: " + line);
            return null;
        }
        try {
            int accountId = Integer.parseInt(parts[0]);
            String action = parts[1].toLowerCase();

            if (!"deposit".equals(action) && !"withdraw".equals(action)) {
                LOGGER.warning("Invalid action in line: " + line);
                throw new IllegalArgumentException("Invalid action: " + action);
            }

            double amount = Double.parseDouble(parts[2]);
            return new TransactionData(accountId, action, amount);
        } catch (NumberFormatException e) {
            LOGGER.warning("Invalid number format in line: " + line);
        } catch (IllegalArgumentException e) {
            LOGGER.warning("Invalid action in line: " + line);
        }
        return null;
    }

    private void processChunk(List<TransactionData> chunk) {
        for (TransactionData data : chunk) {
            processTransaction(data);
            reportToLegacyServerAsync(data);
        }
    }

    private void processTransaction(TransactionData data) {
        try {
            bank.processTransaction(data.accountId, data.action, data.amount);
            LOGGER.info(String.format("Processed transaction: Account %d, Action: %s, Amount: %.2f",
                    data.accountId, data.action, data.amount));
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error processing transaction", e);
        }
    }

    private void reportToLegacyServerAsync(TransactionData data) {
        reportingQueue.offer(() -> {
            try {
                AccountImpl account = (AccountImpl) bank.getAccountByID(data.accountId);
                double balance = account.getBalance();
                legacyServer.reportActivity(data.accountId, Instant.now(), data.amount, balance);
                LOGGER.info(String.format("Reported to legacy server: Account %d, Action: %s, Amount: %.2f, Balance: %.2f",
                        data.accountId, data.action, data.amount, balance));
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

    static class TransactionData {
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