package com.crm.rdvision.service;
import com.crm.rdvision.entity.UploadTicket;
import com.crm.rdvision.repository.UploadTicketRepo;
import org.apache.poi.ss.usermodel.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class ExcelService {

    @Autowired
    private UploadTicketRepo uploadTicketRepo;

    public List<String> saveProductsFromExcel(MultipartFile file) {
        List<String> errors = new ArrayList<>();
        List<UploadTicket> tickets = new ArrayList<>();

        try (InputStream inputStream = file.getInputStream()) {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) { // Skip header row
                Row row = sheet.getRow(i);
                try {
                    UploadTicket uploadTicket = new UploadTicket();

                    uploadTicket.setFirstName(getCellValue(row, 0));
                    uploadTicket.setEmail(getCellValue(row, 1));
                    uploadTicket.setMobileNumber(getCellValue(row, 2));
                    uploadTicket.setProductEnquiry(getCellValue(row, 3));
                    uploadTicket.setSenderCountryIso(getCellValue(row, 4));
                    uploadTicket.setSubject(getCellValue(row, 5));
                    uploadTicket.setUploadDate(LocalDate.now());
                    uploadTicket.setQueryTime(LocalTime.now().toString());
                    uploadTicket.setTicketstatus("New");
                    UUID uuid = UUID.randomUUID();
                    String uniqueId = uuid.toString().replace("-", "");
                    uploadTicket.setUniqueQueryId(uniqueId);

                    tickets.add(uploadTicket);

                } catch (Exception e) {
                    // Log and collect error for this row
                    errors.add("Row " + (i + 1) + ": " + e.getMessage());
                }
            }

            // Only save data if there are no errors
            if (errors.isEmpty() && !tickets.isEmpty()) {
                uploadTicketRepo.saveAll(tickets);
            } else {
                errors.add("No data saved due to validation errors.");
            }

        } catch (Exception e) {
            errors.add("Failed to process Excel file: " + e.getMessage());
        }

        return errors;
    }

    // Helper method to get cell value safely
    private String getCellValue(Row row, int columnIndex) {
        Cell cell = row.getCell(columnIndex);
        if (cell == null) {
            throw new IllegalArgumentException("Missing value in column " + (columnIndex + 1));
        }
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                return String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            default:
                throw new IllegalArgumentException("Unsupported cell type in column " + (columnIndex + 1));
        }
    }

}
