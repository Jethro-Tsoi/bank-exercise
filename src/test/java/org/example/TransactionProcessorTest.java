package org.example;

import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionProcessorTest {

    private TransactionProcessor processor;
    private BankImpl bank;
    private LegacyServer legacyServer;
    private ConcurrentHashMap<Integer, AccountImpl> accounts;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        bank = mock(BankImpl.class);
        legacyServer = mock(LegacyServer.class);
        processor = new TransactionProcessor(bank, legacyServer);
        accounts = new ConcurrentHashMap<>();

        // Set up mock behavior for bank
        when(bank.getAccountByID(anyInt())).thenAnswer(invocation -> {
            int accountId = invocation.getArgument(0);
            return accounts.get(accountId);
        });

        doAnswer(invocation -> {
            int accountId = invocation.getArgument(0);
            String action = invocation.getArgument(1);
            double amount = invocation.getArgument(2);
            AccountImpl account = accounts.computeIfAbsent(accountId, id -> new AccountImpl(accountId));
            double actualAmount = "withdraw".equals(action) ? -amount : amount;
            AccountEntry entry = new AccountEntry(Instant.now(), actualAmount, action);
            account.addEntry(entry);
            return null;
        }).when(bank).processTransaction(anyInt(), anyString(), anyDouble());
    }

    @AfterEach
    void tearDown() throws Exception {
        processor.close();
    }

    @Test
    void testBasicTransactionProcessing() throws IOException, InterruptedException {
        Path transactionFile = createTransactionFile(
                "AccountId,Action,Amount\n" +
                        "1,deposit,100.00\n" +
                        "2,withdraw,50.00\n" +
                        "1,withdraw,25.00\n");

        processor.processTransactionFile(transactionFile);
        Thread.sleep(1000); // Allow time for asynchronous operations

        verify(bank, times(3)).processTransaction(anyInt(), anyString(), anyDouble());
        verify(bank, times(3)).getAccountByID(anyInt());
        verify(legacyServer, times(3)).reportActivity(anyInt(), any(Instant.class), anyDouble(), anyDouble());

        assertEquals(75.00, accounts.get(1).getBalance(), 0.01);
        assertEquals(-50.00, accounts.get(2).getBalance(), 0.01);
    }

    @Test
    void testLargeTransactionFile() throws IOException, InterruptedException {
        StringBuilder fileContent = new StringBuilder("AccountId,Action,Amount\n");
        for (int i = 0; i < 10000; i++) {
            fileContent.append(i % 100).append(",deposit,").append(i).append(".00\n");
        }
        Path transactionFile = createTransactionFile(fileContent.toString());

        processor.processTransactionFile(transactionFile);
        Thread.sleep(2000); // Allow more time for processing large file

        verify(bank, times(10000)).processTransaction(anyInt(), anyString(), anyDouble());
        verify(legacyServer, times(10000)).reportActivity(anyInt(), any(Instant.class), anyDouble(), anyDouble());

        for (int i = 0; i < 100; i++) {
            assertTrue(accounts.get(i).getBalance() > 0);
        }
    }

    @Test
    void testInvalidTransactions() throws IOException, InterruptedException {
        Path transactionFile = createTransactionFile(
                "AccountId,Action,Amount\n" +
                        "1,deposit,100.00\n" +
                        "invalid,withdraw,50.00\n" +
                        "2,invalid,25.00\n" +
                        "3,withdraw,invalid\n");

        processor.processTransactionFile(transactionFile);
        Thread.sleep(1000);

        verify(bank, times(1)).processTransaction(anyInt(), anyString(), anyDouble());
        verify(legacyServer, times(1)).reportActivity(anyInt(), any(Instant.class), anyDouble(), anyDouble());

        assertEquals(100.00, accounts.get(1).getBalance(), 0.01);
        assertFalse(accounts.containsKey(2));
        assertFalse(accounts.containsKey(3));
    }

    @Test
    void testEmptyFile() throws IOException, InterruptedException {
        Path transactionFile = createTransactionFile("AccountId,Action,Amount\n");

        processor.processTransactionFile(transactionFile);
        Thread.sleep(1000);

        verify(bank, never()).processTransaction(anyInt(), anyString(), anyDouble());
        verify(legacyServer, never()).reportActivity(anyInt(), any(Instant.class), anyDouble(), anyDouble());

        assertTrue(accounts.isEmpty());
    }

    @Test
    void testMissingHeader() throws IOException, InterruptedException {
        Path transactionFile = createTransactionFile(
                "1,deposit,100.00\n" +
                        "2,withdraw,50.00\n");

        processor.processTransactionFile(transactionFile);
        Thread.sleep(1000);

        verify(bank, times(1)).processTransaction(anyInt(), anyString(), anyDouble());
        verify(legacyServer, times(1)).reportActivity(anyInt(), any(Instant.class), anyDouble(), anyDouble());

        assertNull(accounts.get(1));
        assertEquals(-50.00, accounts.get(2).getBalance(), 0.01);
    }

    @Test
    void testConcurrentProcessing() throws IOException, InterruptedException {
        StringBuilder fileContent = new StringBuilder("AccountId,Action,Amount\n");
        for (int i = 0; i < 1000; i++) {
            fileContent.append(1).append(",deposit,10.00\n");
        }
        Path transactionFile = createTransactionFile(fileContent.toString());

        processor.processTransactionFile(transactionFile);
        Thread.sleep(2000);

        verify(bank, times(1000)).processTransaction(anyInt(), anyString(), anyDouble());
        verify(legacyServer, times(1000)).reportActivity(anyInt(), any(Instant.class), anyDouble(), anyDouble());

        assertEquals(10000.00, accounts.get(1).getBalance(), 0.01);
    }

    private Path createTransactionFile(String content) throws IOException {
        Path transactionFile = tempDir.resolve("transactions.csv");
        Files.writeString(transactionFile, content);
        return transactionFile;
    }
}