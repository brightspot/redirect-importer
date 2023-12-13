package brightspot.redirectimporter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import brightspot.redirect.QueryStringOption;
import brightspot.redirect.QueryStringOptionModify;
import brightspot.redirect.QueryStringOptionPreserve;
import brightspot.redirect.VanityRedirect;
import brightspot.redirectimporter.utils.DefaultImplementationSupplier;
import brightspot.redirectimporter.utils.GoogleCSVUtils;
import com.google.common.base.Throwables;
import com.psddev.cms.db.ExternalItemConverter;
import com.psddev.google.drive.GoogleDriveFile;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VanityRedirectGoogleDriveFileConverter extends ExternalItemConverter<GoogleDriveFile, VanityRedirect> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VanityRedirectGoogleDriveFileConverter.class);

    @Override
    public Collection<? extends VanityRedirect> convert(GoogleDriveFile source) {
        try {
            String googleMimeType = source.getMimeType();
            if (googleMimeType.toLowerCase(Locale.ROOT).contains("spreadsheet")) {
                return extractRedirects(source);
            }
        } catch (IOException exception) {
            LOGGER.error("Could not download file: " + exception.getMessage());
            Throwables.propagate(exception);
        }
        return Collections.emptyList();
    }

    private static Collection<VanityRedirect> extractRedirects(GoogleDriveFile source) throws IOException {

        Collection<VanityRedirect> result = new ArrayList<>();

        // Extract redirects from the spreadsheet using csv
        CSVParser csvParser = GoogleCSVUtils.getCsv(source, true);

        List<CSVRecord> records = csvParser.getRecords();

        if (records == null || records.size() == 0) {
            return Collections.emptyList();
        }

        for (CSVRecord csvRecord : records) {

            String localPath = csvRecord.get(0);
            String newUrl = csvRecord.get(1);
            String status = csvRecord.get(2);
            String queryString = csvRecord.get(3);

            VanityRedirect vanityRedirect = new VanityRedirect();
            vanityRedirect.setLocalUrls(Collections.singleton(localPath));
            vanityRedirect.setDestination(newUrl);

            // If status equals 302, set to true, otherwise it is false
            boolean temporary = status.equals("302");
            vanityRedirect.setTemporary(temporary);

            if (queryString.equalsIgnoreCase("preserve")) {
                vanityRedirect.setQueryString(DefaultImplementationSupplier.createDefault(
                        QueryStringOption.class,
                        QueryStringOptionPreserve.class));
            } else if (queryString.equalsIgnoreCase("ignore")) {
                vanityRedirect.setQueryString(DefaultImplementationSupplier.createDefault(
                        QueryStringOption.class,
                        QueryStringOptionPreserve.class));
            } else if (queryString.equalsIgnoreCase("modify")) {
                vanityRedirect.setQueryString(DefaultImplementationSupplier.createDefault(
                        QueryStringOption.class,
                        QueryStringOptionModify.class));
            }
        }

        // Return redirects
        return result;
    }
}

