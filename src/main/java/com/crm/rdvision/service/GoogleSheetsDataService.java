package com.crm.rdvision.service;

import com.crm.rdvision.entity.TicketEntity;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.InsertDimensionRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class GoogleSheetsDataService {

    @Autowired
    private GoogleSheetsService googleSheetsService;

    private static final String SPREADSHEET_ID = "1n1DlFBtmx7dXtUhRyv70BhVGz8wCjIpMoB2VgGHCWt0"; // Update with your actual Spreadsheet ID
    private static final String RANGE = "Sheet1!A1"; // Target the top of the sheet

    public void writeDataToSheet(TicketEntity values) throws GeneralSecurityException, IOException {
        Sheets sheetsService = googleSheetsService.getSheetsService();

        // Step 1: Retrieve all existing rows in the sheet
        ValueRange existingData = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, "Sheet1")
                .execute();

        // Step 2: Determine the last row with data
        int lastRow = existingData.getValues() != null ? existingData.getValues().size() : 0;

        // Step 3: Prepare data for the new row
        List<Object> rowData = Arrays.asList(
                values.getUniqueQueryId(),
                values.getQueryType(),
                values.getQueryTime(),
                values.getSenderName(),
                values.getSenderMobile(),
                values.getSenderEmail(),
                values.getSubject(),
                values.getSenderCompany(),
                values.getSenderAddress(),
                values.getSenderCity(),
                values.getSenderState(),
                values.getSenderPincode(),
                values.getSenderCountryIso(),
                values.getSenderMobileAlt(),
                values.getSenderPhone(),
                values.getSenderPhoneAlt(),
                values.getSenderEmailAlt(),
                values.getQueryProductName(),
                values.getQueryMessage(),
                values.getQueryMcatName(),
                values.getReceiverMobile()
        );

        // Replace null values with "N/A"
        List<Object> formattedRowData = rowData.stream()
                .map(value -> value == null ? "N/A" : value)
                .collect(Collectors.toList());

        // Step 4: Set up the ValueRange for the next row
        ValueRange body = new ValueRange().setValues(Collections.singletonList(formattedRowData));

        // Calculate the target range (e.g., "Sheet1!A<lastRow + 1>")
        String targetRange = "Sheet1!A" + (lastRow + 1);

        // Step 5: Append the new data
        sheetsService.spreadsheets().values()
                .update(SPREADSHEET_ID, targetRange, body)
                .setValueInputOption("RAW")
                .execute();
    }


}
