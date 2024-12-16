package com.crm.rdvision.service;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Collections;

@Service
public class GoogleSheetsService {

    private static final String APPLICATION_NAME = "Spring Boot Google Sheets Integration";
    private static final String CREDENTIALS_FILE_PATH = "/springboot-to-excel-8407e49da553.json"; // Ensure the path is correct for your resources

    public Sheets getSheetsService() throws GeneralSecurityException, IOException {
        // Load the credentials file from the resources folder
        try (InputStream credentialStream = getClass().getResourceAsStream(CREDENTIALS_FILE_PATH)) {
            if (credentialStream == null) {
                throw new IOException("Credential file not found: " + CREDENTIALS_FILE_PATH);
            }
            GoogleCredentials credentials = GoogleCredentials.fromStream(credentialStream)
                    .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS));
            return new Sheets.Builder(GoogleNetHttpTransport.newTrustedTransport(), JacksonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credentials))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        }
    }
}
