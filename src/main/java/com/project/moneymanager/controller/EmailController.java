package com.project.moneymanager.controller;

import com.project.moneymanager.service.ExpenseService;
import com.project.moneymanager.service.IncomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/email")
@RequiredArgsConstructor
public class EmailController {

    private final IncomeService incomeService;
    private final ExpenseService expenseService;

    @GetMapping("/income-excel")
    public ResponseEntity<?> emailIncomeExcel() throws IOException {
        incomeService.emailIncomeExcelToCurrentUser();
        return ResponseEntity.ok(Map.of("message", "Income details emailed successfully"));
    }

    @GetMapping("/expense-excel")
    public ResponseEntity<?> emailExpenseExcel() throws IOException {
        expenseService.emailExpenseExcelToCurrentUser();
        return ResponseEntity.ok(Map.of("message", "Expense details emailed successfully"));
    }
}