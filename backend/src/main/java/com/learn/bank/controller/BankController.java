package com.learn.bank.controller;

import com.learn.bank.model.Dtos.*;
import com.learn.bank.service.BankService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * The banking REST API. Five endpoints the UI depends on:
 *
 *   POST /api/login                          -> authenticate
 *   GET  /api/customer                       -> profile
 *   GET  /api/accounts                       -> account list + balances
 *   GET  /api/accounts/{id}/transactions     -> statement
 *   POST /api/transfer                       -> move money
 *
 * These are exactly the contracts WireMock will later reproduce as JSON stubs.
 */
@RestController
@RequestMapping("/api")
public class BankController {

    private final BankService service;

    public BankController(BankService service) {
        this.service = service;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        return service.login(req.username(), req.password())
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponse("INVALID_CREDENTIALS", "Wrong username or password")));
    }

    @GetMapping("/customer")
    public ResponseEntity<?> customer(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (unauthorized(auth)) return unauthorizedResponse();
        return ResponseEntity.ok(service.customer());
    }

    @GetMapping("/accounts")
    public ResponseEntity<?> accounts(@RequestHeader(value = "Authorization", required = false) String auth) {
        if (unauthorized(auth)) return unauthorizedResponse();
        return ResponseEntity.ok(new AccountsResponse(service.accounts()));
    }

    @GetMapping("/accounts/{id}/transactions")
    public ResponseEntity<?> transactions(@PathVariable String id,
                                          @RequestHeader(value = "Authorization", required = false) String auth) {
        if (unauthorized(auth)) return unauthorizedResponse();
        if (!service.accountExists(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("NOT_FOUND", "No account " + id));
        }
        return ResponseEntity.ok(new TransactionsResponse(service.transactions(id)));
    }

    @PostMapping("/transfer")
    public ResponseEntity<?> transfer(@RequestBody TransferRequest req,
                                      @RequestHeader(value = "Authorization", required = false) String auth) {
        if (unauthorized(auth)) return unauthorizedResponse();
        try {
            return ResponseEntity.ok(service.transfer(req));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body(new ErrorResponse("INSUFFICIENT_FUNDS", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse("BAD_REQUEST", e.getMessage()));
        }
    }

    private boolean unauthorized(String auth) {
        return auth == null || !auth.startsWith("Bearer ");
    }

    private ResponseEntity<?> unauthorizedResponse() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("UNAUTHORIZED", "Missing or invalid token"));
    }
}
