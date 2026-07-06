package com.learn.bank.model;

import java.util.List;

/**
 * All request/response shapes for the banking API, kept together so the
 * contract is easy to read. WireMock stubs must produce these same JSON shapes.
 */
public final class Dtos {

    public record LoginRequest(String username, String password) {}

    public record LoginResponse(String token, String customerId, String name) {}

    public record Customer(String customerId, String name, String email,
                           String phone, String memberSince) {}

    public record Account(String id, String type, String number,
                          double balance, String currency) {}

    public record Transaction(String id, String date, String description,
                              double amount, String type) {}

    public record TransferRequest(String fromAccountId, String toAccountId, double amount) {}

    public record TransferResponse(String status, String reference, double newBalance) {}

    public record AccountsResponse(List<Account> accounts) {}

    public record TransactionsResponse(List<Transaction> transactions) {}

    public record ErrorResponse(String error, String message) {}

    private Dtos() {}
}
