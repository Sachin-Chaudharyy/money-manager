package com.project.moneymanager.service;

import com.project.moneymanager.dto.ExpenseDTO;
import com.project.moneymanager.dto.IncomeDTO;
import com.project.moneymanager.entity.CategoryEntity;
import com.project.moneymanager.entity.ExpenseEntity;
import com.project.moneymanager.entity.IncomeEntity;
import com.project.moneymanager.entity.ProfileEntity;
import com.project.moneymanager.repository.CategoryRepository;
import com.project.moneymanager.repository.IncomeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IncomeService {

    private final CategoryRepository categoryRepository;
    private final IncomeRepository incomeRepository;
    private final ProfileService  profileService;
    private final EmailService emailService;

    public IncomeDTO addIncome(IncomeDTO dto) {
        ProfileEntity profile = profileService.getCurrentProfile();
        CategoryEntity category = categoryRepository.findByIdAndProfileId(dto.getCategoryId(), profile.getId())
                .orElseThrow(() -> new RuntimeException("Category not found"));
        IncomeEntity newIncome = toEntity(dto, profile, category);
        newIncome = incomeRepository.save(newIncome);
        return toDTO(newIncome);
    }

    public List<IncomeDTO> getCurrentMonthIncomesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        LocalDate now = LocalDate.now();
        LocalDate startDate = now.withDayOfMonth(1);
        LocalDate endDate = now.withDayOfMonth(now.lengthOfMonth());
        List<IncomeEntity> list = incomeRepository.findByProfileIdAndDateBetween(profile.getId(), startDate, endDate);
        return list.stream().map(this::toDTO).toList();
    }

    public void deleteIncome(Long incomeId) {
        ProfileEntity profile = profileService.getCurrentProfile();
        IncomeEntity entity = incomeRepository.findById(incomeId)
                .orElseThrow(() -> new RuntimeException("Income not found"));
        if(!entity.getProfile().getId().equals(profile.getId())){
            throw new RuntimeException("Unauthorized to delete this income");
        }
        incomeRepository.delete(entity);
    }

    public List<IncomeDTO> getLatest5IncomesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<IncomeEntity> list = incomeRepository.findTop5ByProfileIdOrderByDateDesc(profile.getId());
        return list.stream().map(this::toDTO).toList();
    }

    public BigDecimal getTotalIncomeForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        BigDecimal total = incomeRepository.findTotalIncomeByProfileId(profile.getId());
        return total != null ? total : BigDecimal.ZERO;
    }

    public List<IncomeDTO> filterIncomes(LocalDate startDate, LocalDate endDate, String keyword, Sort sort) {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<IncomeEntity> list = incomeRepository.findByProfileIdAndDateBetweenAndNameContainingIgnoreCase(profile.getId(), startDate, endDate, keyword, sort);
        return list.stream().map(this::toDTO).toList();
    }

    public List<IncomeDTO> getAllIncomesForCurrentUser() {
        ProfileEntity profile = profileService.getCurrentProfile();
        List<IncomeEntity> list = incomeRepository.findByProfileIdOrderByIdAsc(profile.getId());
        return list.stream().map(this::toDTO).toList();
    }

    public ByteArrayInputStream generateIncomeExcel(List<IncomeDTO> incomes) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Income");
            Row headerRow = sheet.createRow(0);
            String[] headers = {"S.No", "Name", "Category", "Amount", "Date"};
            for (int i = 0; i < headers.length; i++) {
                headerRow.createCell(i).setCellValue(headers[i]);
            }
            int rowIdx = 1;
            int serialNo = 1;
            for (IncomeDTO income : incomes) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(serialNo++);
                row.createCell(1).setCellValue(income.getName());
                row.createCell(2).setCellValue(income.getCategoryName() != null ? income.getCategoryName() : "");
                row.createCell(3).setCellValue(income.getAmount().doubleValue());
                row.createCell(4).setCellValue(income.getDate().toString());
            }
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    public void emailIncomeExcelToCurrentUser() throws IOException {
        ProfileEntity profile = profileService.getCurrentProfile();
        ByteArrayInputStream excelFile = generateIncomeExcel(getAllIncomesForCurrentUser());

        emailService.sendEmailWithAttachment(
                profile.getEmail(),
                "Your Income Details",
                "Hi " + profile.getFullName() + ",\n\nPlease find attached your income details.\n\nRegards,\nMoney Manager",
                excelFile,
                "income_details.xlsx"
        );
    }

    private IncomeEntity toEntity(IncomeDTO dto, ProfileEntity profile, CategoryEntity category){
        return IncomeEntity.builder()
                .name(dto.getName())
                .icon(dto.getIcon())
                .amount(dto.getAmount())
                .date(dto.getDate())
                .profile(profile)
                .category(category)
                .build();
    }

    private IncomeDTO toDTO(IncomeEntity entity){
        return IncomeDTO.builder()
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
