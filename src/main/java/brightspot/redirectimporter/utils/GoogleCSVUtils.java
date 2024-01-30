package brightspot.redirectimporter.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.psddev.cms.tool.CmsTool;
import com.psddev.dari.db.Singleton;
import com.psddev.dari.util.IoUtils;
import com.psddev.google.GoogleAccountAccess;
import com.psddev.google.GoogleAccountAccessDatastoreFactory;
import com.psddev.google.drive.GoogleDriveFile;
import com.psddev.google.drive.GoogleDriveSettings;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.lang3.StringUtils;

public class GoogleCSVUtils {

    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String APPLICATION_NAME = "brightspot/googledrive";

    /**
     * @param file Google drive file.
     * Reads in the given <code>file</code> as {@link CSVParser}, either with the first record as the header row or with a
     * {@link #generateDefaultHeaderRow} based on whether <code>hasHeader</code> exists.
     *
     * @param file Can't be null.
     */
    public static CSVParser getCsv(GoogleDriveFile file) {
        try (InputStream input = getFiles(file.getAccountAccess())
                .export(file.getFileId(), "text/csv")
                .executeMediaAsInputStream()) {

            return CSVParser.parse(
                    IoUtils.toString(input, StandardCharsets.UTF_8),
                    CSVFormat
                            .DEFAULT
                            .withFirstRecordAsHeader()
                            .withIgnoreHeaderCase(true)
                            .withIgnoreSurroundingSpaces(true)
            );

        } catch (IOException error) {
            throw new RuntimeException(
                    "Can't export the Google Drive file into CSV!",
                    error);
        }
    }

    /**
     * Constructs the {@link Drive} object by building a {@link com.google.api.client.auth.oauth2.Credential} and an {@link HttpTransport}.
     *
     * @param accountAccess Account that API requests will be made to
     * @return {@link Drive} object from the Google SDK that will be used to make requests
     */
    public static Drive initializeApi(GoogleAccountAccess accountAccess) throws GeneralSecurityException, IOException {
        if (accountAccess == null) {
            throw new IOException("Cannot initialize API, Google account access cannot be retrieved");
        }

        GoogleAuthorizationCodeFlow flow = buildAuthFlow();
        Credential credential = flow.loadCredential(accountAccess.getId().toString());

        HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();

        return new Drive.Builder(transport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    /**
     * Returns all the files accessible by this {@link GoogleAccountAccess}.
     */
    public static Drive.Files getFiles(GoogleAccountAccess access) {
        try {
            return initializeApi(access).files();

        } catch (GeneralSecurityException | IOException error) {
            throw new IllegalStateException(
                    "Can't initialize the Google Drive API!",
                    error);
        }
    }

    public static int getNumberOfColumns(GoogleDriveFile file) {
        return Optional.of(getHeaderRow(file)).map(List::size).orElse(0);
    }

    /**
     * Generates column heading strings based on the number of columns there are by manipulating their ASCII codes.
     * Column headings follow the pattern A, B, C, …, Z, AA, AB, AC, …, AZ, BA, BB, …, ZZ, AAA, AAB, …, etc.
     */
    public static String generateColumnHeading(int i) {
        return i < 0 ? "" : generateColumnHeading((i / 26) - 1) + (char) (65 + i % 26);
    }

    public static List<String> getHeaderRow(GoogleDriveFile file) {
        return getCsv(file).getHeaderNames();
    }

    private static List<String> generateDefaultHeaderRow(GoogleDriveFile file) {
        List<String> headers = new ArrayList<>();
        for (int i = 0; i < getNumberOfColumns(file); i++) {
            headers.add(generateColumnHeading(i));
        }
        return headers;
    }

    /**
     * Builds the authorization flow for Google API Requests.
     *
     * @return Authorization flow for Google API Requests
     */
    static GoogleAuthorizationCodeFlow buildAuthFlow() throws IOException, GeneralSecurityException {
        GoogleDriveSettings settings = Singleton.getInstance(CmsTool.class).as(GoogleDriveSettings.class);
        String clientId = settings.getClientId();
        String clientSecret = settings.getClientSecret();
        if (StringUtils.isBlank(clientId) || StringUtils.isBlank(
                clientSecret)) {
            throw new IOException("Google Federated Search Client ID and Client Secret are not set in Sites & Settings");
        }
        HttpTransport transport = GoogleNetHttpTransport.newTrustedTransport();
        return new GoogleAuthorizationCodeFlow.Builder(
                transport,
                JSON_FACTORY,
                clientId,
                clientSecret,
                DriveScopes.all())
                .setAccessType("offline")
                .setApprovalPrompt("force")
                .setDataStoreFactory(new GoogleAccountAccessDatastoreFactory())
                .build();
    }
}
