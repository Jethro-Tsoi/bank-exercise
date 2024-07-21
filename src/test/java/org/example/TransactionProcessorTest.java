package org.example;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mockito;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class TransactionProcessorTest {

    private BankImpl bank;
    private LegacyServer legacyServer;
    private TransactionProcessor processor;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        bank = new BankImpl();
        legacyServer = Mockito.mock(LegacyServer.class);
        processor = new TransactionProcessor(bank, legacyServer);
    }

    @Test
    void testProcessTransactionFile() throws IOException {
        Path file = tempDir.resolve("transactions.csv");
        Files.writeString(file,
                "Acc Id,Action,Amount\n" +
                        "1,Deposit,100\n" +
                        "1,Withdraw,50\n" +
                        "2,Deposit,200\n"
        );

        processor.processTransactionFile(file);

        Account account1 = bank.getAccountByID(1);
        assertEquals(50, account1.getBalance());
        assertEquals(2, account1.entries().length);

        Account account2 = bank.getAccountByID(2);
        assertEquals(200, account2.getBalance());
        assertEquals(1, account2.entries().length);

        verify(legacyServer, times(3)).reportActivity(anyInt(), any(Instant.class), anyDouble(), anyDouble());
    }

    @Test
    void testInvalidLineInFile() throws IOException {
        Path file = tempDir.resolve("transactions.csv");
        Files.writeString(file,
                "Acc Id,Action,Amount\n" +
                        "1,Deposit,100\n" +
                        "invalid,line,here\n" +
                        "2,Deposit,200\n"
        );

        processor.processTransactionFile(file);

        Account account1 = bank.getAccountByID(1);
        assertEquals(100, account1.getBalance());

        Account account2 = bank.getAccountByID(2);
        assertEquals(200, account2.getBalance());

        verify(legacyServer, times(2)).reportActivity(anyInt(), any(Instant.class), anyDouble(), anyDouble());
    }

    @Test
    void testFileNotFound() {
        Path file = tempDir.resolve("non_existent_file.csv");
        assertDoesNotThrow(() -> processor.processTransactionFile(file));
    }

    @Test
    void testBatchProcessing() throws IOException {
        Path file = tempDir.resolve("large_transactions.csv");

        // Create a large CSV file with 2500 transactions
        String header = "Acc Id,Action,Amount\n";
        String transactions = IntStream.range(0, 2500)
                .mapToObj(i -> String.format("%d,Deposit,100", i % 100))
                .collect(Collectors.joining("\n"));

        Files.writeString(file, header + transactions);

        processor.processTransactionFile(file);

        // Check that all accounts were created and have the correct balance
        for (int i = 0; i < 100; i++) {
            Account account = bank.getAccountByID(i);
            assertEquals(2500, account.getBalance());
            assertEquals(25, account.entries().length);
        }

        // Verify that the legacy server was called for each transaction
        verify(legacyServer, times(2500)).reportActivity(anyInt(), any(Instant.class), anyDouble(), anyDouble());
    }

    @Test
    void testPartialBatch() throws IOException {
        Path file = tempDir.resolve("partial_batch.csv");

        // Create a CSV file with 1500 transactions (1 full batch + 1 partial batch)
        String header = "Acc Id,Action,Amount\n";
        String transactions = IntStream.range(0, 1500)
                .mapToObj(i -> String.format("%d,Deposit,100", i % 50))
                .collect(Collectors.joining("\n"));

        Files.writeString(file, header + transactions);

        processor.processTransactionFile(file);

        // Check that all accounts were created and have the correct balance
        for (int i = 0; i < 50; i++) {
            Account account = bank.getAccountByID(i);
            assertEquals(3000, account.getBalance());
            assertEquals(30, account.entries().length);
        }

        // Verify that the legacy server was called for each transaction
        verify(legacyServer, times(1500)).reportActivity(anyInt(), any(Instant.class), anyDouble(), anyDouble());
    }
}