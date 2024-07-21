package org.example;

// this must be always be consistent
// the sum of entries must always match the balance
// overdraft/negative balance allowed
public interface Account {

    double getBalance();
    AccountEntry[] entries();
}
