package com.project.moneymanager.service;

import com.project.moneymanager.dto.ExpenseDTO;
import com.project.moneymanager.entity.CategoryEntity;
import com.project.moneymanager.entity.ExpenseEntity;
import com.project.moneymanager.entity.ProfileEntity;
import com.project.moneymanager.repository.CategoryRepository;
import com.project.moneymanager.repository.ExpenseRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExpenseService {

    private final CategoryRepository categoryRepository;
    private final ExpenseRepository expenseRepository;
    private final ProfileService profileService;
    private final EmailService emailService;


    public ExpenseDTO addExpense(ExpenseDTO dto) {
        ProfileEntity profile = profileService.getCurrentProfile();
        CategoryEntity category = categoryRepository.findByIdAndProfileId(dto.getCategoryId(), profile.getId())
                .orElseThrow(() -> new RuntimeException("Category not found or accessible"));
        ExpenseEntity newExpense = toEntity(dto, profile, category);
        newExpense = expenseRepository.save(newExpense);
        return toDTO(newExpense);
    }

    public List<ExpenseDTO> getCurrentMonthExpensesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.withDayOfMonth(1);
        LocalDate endDate = now.withDayOfMonth(now.lengthOfMonth());
        List<ExpenseEntity> list = expenseRepository.findByProfileIdAndDateBetween(profile.getId(), startDate, endDate);
        return list.stream().map(this::toDTO).toList();
    }

    public void deleteExpense(Long expenseId) {
        ProfileEntity profile = profileService.getCurrentProfile();
        ExpenseEntity entity = expenseRepository.findById(expenseId)
                .orElseThrow(() -> new RuntimeException("Expense not found"));
        if(!entity.getProfile().getId().equals(profile.getId())){
            throw new RuntimeException("Unauthorized to delete this expense");
        }
        expenseRepository.delete(entity);
    }

    public List<ExpenseDTO> getLatest5ExpensesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<ExpenseEntity> list = expenseRepository.findTop5ByProfileIdOrderByDateDesc(profile.getId());
        return list.stream().map(this::toDTO).toList();
    }

    public BigDecimal getTotalExpenseForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        BigDecimal total = expenseRepository.findTotalExpenseByProfileId(profile.getId());
        return total != null ? total : BigDecimal.ZERO;
    }

    public List<ExpenseDTO> filterExpenses(LocalDate startDate, LocalDate endDate, String keyword, Sort sort) {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<ExpenseEntity> list = expenseRepository.findByProfileIdAndDateBetweenAndNameContainingIgnoreCase(profile.getId(), startDate, endDate, keyword, sort);
        return list.stream().map(this::toDTO).toList();
    }

    public List<ExpenseDTO> getExpensesForProfileOnDate(Long profileId, LocalDate date) {
        List<ExpenseEntity> list = expenseRepository.findByProfileIdAndDate(profileId, date);
        return list.stream().map(this::toDTO).toList();
    }

    public List<ExpenseDTO> getAllExpensesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<ExpenseEntity> list = expenseRepository.findByProfileIdOrderByIdAsc(profile.getId());
        return list.stream().map(this::toDTO).toList();
    }

    public ByteArrayInputStream generateExpenseExcel(List<ExpenseDTO> expenses) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Expense");
            Row headerRow = sheet.createRow(0);
            String[] headers = {"S.No", "Name", "Category", "Amount", "Date"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            int rowIdx = 1;
            int serialNo = 1;
            for (ExpenseDTO expense : expenses) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(serialNo++);
                row.createCell(1).setCellValue(expense.getName());
                row.createCell(2).setCellValue(expense.getCategoryName() != null ? expense.getCategoryName() : "");
                row.createCell(3).setCellValue(expense.getAmount().doubleValue());
                row.createCell(4).setCellValue(expense.getDate().toString());
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    public void emailExpenseExcelToCurrentUser() throws IOException {
        ProfileEntity profile = profileService.getCurrentProfile();
        ByteArrayInputStream excelFile = generateExpenseExcel(getAllExpensesForCurrentUser());

        emailService.sendEmailWithAttachment(
                profile.getEmail(),
                "Your Expense Details",
                "Hi " + profile.getFullName() + ",\n\nPlease find attached your expense details.\n\nRegards,\nMoney Manager",
                excelFile,
                "expense_details.xlsx"
        );
    }

    private ExpenseEntity toEntity(ExpenseDTO dto, ProfileEntity profile, CategoryEntity category){
        return ExpenseEntity.builder()
                .name(dto.getName())
                .icon(dto.getIcon())
                .amount(dto.getAmount())
                .date(dto.getDate())
                .profile(profile)
                .category(category)
                .build();
    }

    private ExpenseDTO toDTO(ExpenseEntity entity){
        return ExpenseDTO.builder()
                .id(entity.getId())
                .name(entity.getName())
                .icon(entity.getIcon())
                .categoryId(entity.getCategory() != null ? entity.getCategory().getId() : null)
                .categoryName(entity.getCategory() != null ? entity.getCategory().getName() : null)
                .amount(entity.getAmount())
                .date(entity.getDate())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
