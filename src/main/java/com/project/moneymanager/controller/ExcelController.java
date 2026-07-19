package com.project.moneymanager.controller;

import com.project.moneymanager.service.ExpenseService;
import com.project.moneymanager.service.IncomeService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayInputStream;
import java.io.IOException;

@RestController
@RequestMapping("/excel")
@RequiredArgsConstructor
public class ExcelController {

    private final IncomeService incomeService;
    private final ExpenseService expenseService;

    @GetMapping("/download/income")
    public ResponseEntity<InputStreamResource> downloadIncomeExcel() throws IOException {
        ByteArrayInputStream in = incomeService.generateIncomeExcel(incomeService.getAllIncomesForCurrentUser());

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=income_details.xlsx");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }

    @GetMapping("/download/expense")
    public ResponseEntity<InputStreamResource> downloadExpenseExcel() throws IOException {
        ByteArrayInputStream in = expenseService.generateExpenseExcel(expenseService.getAllExpensesForCurrentUser());

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=expense_details.xlsx");

        return ResponseEntity
                .ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }
}