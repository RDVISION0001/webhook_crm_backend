package com.crm.rdvision.service;

import com.crm.rdvision.Exception.BussinessException;
import com.crm.rdvision.dto.OrderDto;
import com.crm.rdvision.dto.ProductOrderDto;
import com.crm.rdvision.entity.Address;
import com.crm.rdvision.entity.Invoice;
import com.crm.rdvision.entity.ProductOrder;
import com.crm.rdvision.entity.TicketEntity;
import com.crm.rdvision.repository.*;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class InventorySheetService {

    @Autowired
    private GoogleSheetsService googleSheetsService;
    @Autowired
    InvoiceRepo invoiceRepo;
    @Autowired
    TicketRepo ticketRepo;
    @Autowired
    UploadTicketRepo uploadTicketRepo;
    @Autowired
    OrderRepo orderRepo;
    @Autowired
    EmailService emailService;
    @Autowired
    OrderService orderService;
    @Autowired
    AddressRepo addressRepo;

    private static final String SPREADSHEET_ID = "1wj66258vFNvCR9nxBkUYnQ92wZfbj6o6zI3hx3OTjoU"; // Update with your actual Spreadsheet ID
    private static final String RANGE = "Sheet1!A1"; // Target the top of the sheet

    public void writeDataToSheet(String uniqueQueryId) throws GeneralSecurityException, IOException, BussinessException, com.avanse.core.exception.TechnicalException {
        Sheets sheetsService = googleSheetsService.getSheetsService();

        Invoice invoice = invoiceRepo.findByTicketId(uniqueQueryId);
        TicketEntity ticketEntity = ticketRepo.findByUniqueQueryId(uniqueQueryId);
        OrderDto orderDto = orderService.getOrder(uniqueQueryId);
        Optional<Address> address = addressRepo.findByTicketId(uniqueQueryId);

        ValueRange existingData = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, "Sheet1")
                .execute();

        int lastRow = existingData.getValues() != null ? existingData.getValues().size() : 0;

        // Loop through product orders
        for (var productOrder : orderDto.getProductOrders()) {
            // Ensure productOrder and products are valid
            if (productOrder.getProduct() == null || productOrder.getProduct().isEmpty()) {
                continue;
            }

            // Use the first product from the list
            var product = productOrder.getProduct().get(0);

            // Prepare row data
            List<Object> rowData = Arrays.asList(
                    LocalDate.now().toString(),
                    ticketEntity != null ? ticketEntity.getSenderName() : "N/A",
                    address.isPresent() ? address.get().getLandmark() : "N/A",
                    address.isPresent() ? address.get().getCity() : "N/A",
                    address.isPresent() ? address.get().getState() : "N/A",
                    address.isPresent() ? address.get().getZipCode() : "N/A",
                    address.isPresent() ? address.get().getCountry() : "N/A",
                    product != null ? product.getName() : "N/A",
                    product != null ? product.getStrength() : "N/A",
                    productOrder.getQuantity() != null ? productOrder.getQuantity() : 0,
                    productOrder.getCurrency() + " " +
                            (productOrder.getTotalAmount() != null && productOrder.getQuantity() != null
                                    ? productOrder.getTotalAmount() / productOrder.getQuantity()
                                    : 0),
                    productOrder.getCurrency() + " " + (productOrder.getTotalAmount() != null
                            ? productOrder.getTotalAmount()
                            : 0),
                    40,
                    (productOrder.getTotalAmount() != null ? productOrder.getTotalAmount() : 0) + 40,
                    invoice != null ? invoice.getInviceId() : "N/A"
            );


            // Replace null values with "N/A"
            List<Object> formattedRowData = rowData.stream()
                    .map(value -> value == null ? "N/A" : value)
                    .collect(Collectors.toList());

            // Calculate the target range for the current product order
            String targetRange = "Sheet1!A" + (lastRow + 1);

            // Prepare the ValueRange object
            ValueRange body = new ValueRange().setValues(Collections.singletonList(formattedRowData));

            // Append the new row to the sheet
            sheetsService.spreadsheets().values()
                    .update(SPREADSHEET_ID, targetRange, body)
                    .setValueInputOption("RAW")
                    .execute();

            // Increment the row counter
            lastRow++;
        }
    }

    public void writeCustomerOrdersToSheet(String uniqueQueryId) throws GeneralSecurityException, IOException, com.avanse.core.exception.TechnicalException, BussinessException {
        Sheets sheetsService = googleSheetsService.getSheetsService();

        // Retrieve the order and related data
        Invoice invoice = invoiceRepo.findByTicketId(uniqueQueryId);
        TicketEntity ticketEntity = ticketRepo.findByUniqueQueryId(uniqueQueryId);
        OrderDto orderDto = orderService.getOrder(uniqueQueryId);
        Optional<Address> address = addressRepo.findByTicketId(uniqueQueryId);

        // Get product orders
        List<ProductOrderDto> productOrders = orderDto.getProductOrders();
        int orderCount = productOrders.size();

        // Retrieve existing sheet data to find the last row
        ValueRange existingData = sheetsService.spreadsheets().values()
                .get(SPREADSHEET_ID, "Sheet1")
                .execute();

        int startRow = existingData.getValues() != null ? existingData.getValues().size() + 1 : 1;

        // Step 1: Insert product order data
        List<List<Object>> rows = new ArrayList<>();
        for (ProductOrderDto productOrder : productOrders) {
            // Populate each row with the correct data corresponding to the header sequence
            rows.add(Arrays.asList(
                    LocalDateTime.now().toString(),
                    invoice != null && invoice.getInviceId() != null ? invoice.getInviceId() : "", // Date - Assuming current date
                    ticketEntity.getSenderName() != null ? ticketEntity.getSenderName() : "", // Sender Name (check null)
                    address.map(Address::getLandmark).orElse(""), // Landmark
                    address.map(Address::getCity).orElse(""), // City
                    address.map(Address::getState).orElse(""), // State
                    address.map(Address::getZipCode).orElse(""), // Zip
                    address.map(Address::getCountry).orElse(""), // Country
                    productOrder.getProduct() != null && !productOrder.getProduct().isEmpty() ? productOrder.getProduct().get(0).getName() : "", // Product Name
                    productOrder.getProduct() != null && !productOrder.getProduct().isEmpty() ? productOrder.getProduct().get(0).getStrength()+"mg" : "", // Strength
                    productOrder.getQuantity() != null ? productOrder.getQuantity() : 0, // Quantity
                    productOrder.getTotalAmount() != null && productOrder.getQuantity() != 0
                            ? productOrder.getCurrency() + " " + String.format("%.2f", productOrder.getTotalAmount() / (double) productOrder.getQuantity())
                            : 0, // Price/Unit
                    productOrder.getTotalAmount() != null ? productOrder.getCurrency()+" "+ productOrder.getTotalAmount() : 0, // Total
                    "USD 40",
                    productOrder.getCurrency()+" "+ (orderDto.getTotalPayableAmount()+40)

            ));
        }

        // Insert values into Google Sheets
        sheetsService.spreadsheets().values()
                .update(SPREADSHEET_ID, "Sheet1!A" + startRow, new ValueRange().setValues(rows))
                .setValueInputOption("RAW")
                .execute();

        // Step 2: Merge cells for the required columns (Date, Landmark, City, State, ZipCode, Country, Order ID)
        List<Request> requests = new ArrayList<>();

        // Merging 'Date' column
        requests.add(new Request().setMergeCells(new MergeCellsRequest()
                .setRange(new GridRange()
                        .setSheetId(0)
                        .setStartRowIndex(startRow - 1)
                        .setEndRowIndex(startRow - 1 + orderCount)
                        .setStartColumnIndex(0) // Date column (index 0)
                        .setEndColumnIndex(1))
                .setMergeType("MERGE_ALL")));

        // Merging 'Sender Name' column
        requests.add(new Request().setMergeCells(new MergeCellsRequest()
                .setRange(new GridRange()
                        .setSheetId(0)
                        .setStartRowIndex(startRow - 1)
                        .setEndRowIndex(startRow - 1 + orderCount)
                        .setStartColumnIndex(1) // Sender Name column (index 1)
                        .setEndColumnIndex(2))
                .setMergeType("MERGE_ALL")));

        // Merging 'Landmark' column
        requests.add(new Request().setMergeCells(new MergeCellsRequest()
                .setRange(new GridRange()
                        .setSheetId(0)
                        .setStartRowIndex(startRow - 1)
                        .setEndRowIndex(startRow - 1 + orderCount)
                        .setStartColumnIndex(2) // Landmark column (index 2)
                        .setEndColumnIndex(3))
                .setMergeType("MERGE_ALL")));

        // Merging 'City' column
        requests.add(new Request().setMergeCells(new MergeCellsRequest()
                .setRange(new GridRange()
                        .setSheetId(0)
                        .setStartRowIndex(startRow - 1)
                        .setEndRowIndex(startRow - 1 + orderCount)
                        .setStartColumnIndex(3) // City column (index 3)
                        .setEndColumnIndex(4))
                .setMergeType("MERGE_ALL")));

        // Merging 'State' column
        requests.add(new Request().setMergeCells(new MergeCellsRequest()
                .setRange(new GridRange()
                        .setSheetId(0)
                        .setStartRowIndex(startRow - 1)
                        .setEndRowIndex(startRow - 1 + orderCount)
                        .setStartColumnIndex(4) // State column (index 4)
                        .setEndColumnIndex(5))
                .setMergeType("MERGE_ALL")));

        // Merging 'ZipCode' column
        requests.add(new Request().setMergeCells(new MergeCellsRequest()
                .setRange(new GridRange()
                        .setSheetId(0)
                        .setStartRowIndex(startRow - 1)
                        .setEndRowIndex(startRow - 1 + orderCount)
                        .setStartColumnIndex(5) // ZipCode column (index 5)
                        .setEndColumnIndex(6))
                .setMergeType("MERGE_ALL")));

        // Merging 'Country' column
        requests.add(new Request().setMergeCells(new MergeCellsRequest()
                .setRange(new GridRange()
                        .setSheetId(0)
                        .setStartRowIndex(startRow - 1)
                        .setEndRowIndex(startRow - 1 + orderCount)
                        .setStartColumnIndex(6) // Country column (index 6)
                        .setEndColumnIndex(7))
                .setMergeType("MERGE_ALL")));

        // Merging 'Invoice ID' column
        requests.add(new Request().setMergeCells(new MergeCellsRequest()
                .setRange(new GridRange()
                        .setSheetId(0)
                        .setStartRowIndex(startRow - 1)
                        .setEndRowIndex(startRow - 1 + orderCount)
                        .setStartColumnIndex(7) // Invoice ID column (index 12)
                        .setEndColumnIndex(8))
                .setMergeType("MERGE_ALL")));
        // Merging 'Invoice ID' column
        requests.add(new Request().setMergeCells(new MergeCellsRequest()
                .setRange(new GridRange()
                        .setSheetId(0)
                        .setStartRowIndex(startRow - 1)
                        .setEndRowIndex(startRow - 1 + orderCount)
                        .setStartColumnIndex(13) // Invoice ID column (index 12)
                        .setEndColumnIndex(14))
                .setMergeType("MERGE_ALL")));
        requests.add(new Request().setMergeCells(new MergeCellsRequest()
                .setRange(new GridRange()
                        .setSheetId(0)
                        .setStartRowIndex(startRow - 1)
                        .setEndRowIndex(startRow - 1 + orderCount)
                        .setStartColumnIndex(14) // Invoice ID column (index 12)
                        .setEndColumnIndex(15))
                .setMergeType("MERGE_ALL")));

        // Apply the merge requests (if needed)
        BatchUpdateSpreadsheetRequest batchUpdateRequest = new BatchUpdateSpreadsheetRequest().setRequests(requests);
        sheetsService.spreadsheets().batchUpdate(SPREADSHEET_ID, batchUpdateRequest).execute();
    }

}


