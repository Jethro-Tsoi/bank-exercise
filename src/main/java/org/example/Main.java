package org.example;

import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Logger;

public class Main {
    private static final Logger LOGGER = Logger.getLogger(Main.class.getName());

    public static void main(String[] args) {
//        if (args.length != 1) {
//            LOGGER.severe("Usage: java Main <path_to_transaction_file>");
//            System.exit(1);
//        }

        Path filePath = Paths.get("src/main/resources/transaction-01.csv");

//        String filePath = args[0];
        BankImpl bank = new BankImpl();

        LegacyServer legacyServer = new LegacyServer();

        try (TransactionProcessor processor = new TransactionProcessor(bank, legacyServer)) {
            processor.processTransactionFile(filePath);

        } catch (Exception e) {
            LOGGER.severe("An error occurred: " + e.getMessage());
        }
    }
}