package com.learn.bank.service;

import com.learn.bank.model.Dtos.*;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory "database" for the demo bank. One customer, two accounts,
 * a handful of transactions. Real enough to drive a UI, simple enough to
 * mirror later with static WireMock JSON.
 */
@Service
public class BankService {

    private final Customer customer = new Customer(
            "CUST-1001", "Adesh Sharma", "adesh.sharma@example.com",
            "+91 98765 43210", "2019-03-14");

    // Mutable balances so transfers actually change state on the real backend.
    private final Map<String, Double> balances = new LinkedHashMap<>(Map.of(
            "AC-100", 84250.75,
            "AC-200", 15980.00));

    private final Map<String, Account> accounts = new LinkedHashMap<>(Map.of(
            "AC-100", new Account("AC-100", "SAVINGS", "XXXX-4021", 0, "INR"),
            "AC-200", new Account("AC-200", "CURRENT", "XXXX-7788", 0, "INR")));

    private final Map<String, List<Transaction>> transactions = Map.of(
            "AC-100", List.of(
                    new Transaction("T-9001", "2026-07-01", "Salary Credit - Acme Corp", 65000.00, "CREDIT"),
                    new Transaction("T-9002", "2026-07-02", "Amazon Purchase", -2499.00, "DEBIT"),
                    new Transaction("T-9003", "2026-07-03", "Electricity Bill", -1830.25, "DEBIT"),
                    new Transaction("T-9004", "2026-07-04", "UPI - Coffee Shop", -280.00, "DEBIT")),
            "AC-200", List.of(
                    new Transaction("T-8001", "2026-07-01", "Client Payment - Invoice #221", 40000.00, "CREDIT"),
                    new Transaction("T-8002", "2026-07-02", "Office Rent", -22000.00, "DEBIT"),
                    new Transaction("T-8003", "2026-07-03", "Software Subscription", -1500.00, "DEBIT")));

    private final AtomicLong reference = new AtomicLong(50000);

    public Optional<LoginResponse> login(String username, String password) {
        // Demo credentials. Anything else is rejected (so WireMock can mimic 401s too).
        if ("adesh".equalsIgnoreCase(username) && "password123".equals(password)) {
            return Optional.of(new LoginResponse(
                    "mock-jwt-token-abc123", customer.customerId(), customer.name()));
        }
        return Optional.empty();
    }

    public Customer customer() {
        return customer;
    }

    public List<Account> accounts() {
        List<Account> result = new ArrayList<>();
        accounts.forEach((id, a) ->
                result.add(new Account(a.id(), a.type(), a.number(), balances.get(id), a.currency())));
        return result;
    }

    public boolean accountExists(String id) {
        return accounts.containsKey(id);
    }

    public List<Transaction> transactions(String accountId) {
        return transactions.getOrDefault(accountId, List.of());
    }

    public synchronized TransferResponse transfer(TransferRequest req) {
        Double fromBalance = balances.get(req.fromAccountId());
        if (fromBalance == null || !balances.containsKey(req.toAccountId())) {
            throw new IllegalArgumentException("Unknown account");
        }
        if (req.amount() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (fromBalance < req.amount()) {
            throw new IllegalStateException("Insufficient funds");
        }
        balances.put(req.fromAccountId(), fromBalance - req.amount());
        balances.put(req.toAccountId(), balances.get(req.toAccountId()) + req.amount());
        String ref = "TXN-" + reference.incrementAndGet();
        return new TransferResponse("SUCCESS", ref, balances.get(req.fromAccountId()));
    }
}
