package brightspot.redirectimporter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import brightspot.redirect.QueryStringOption;
import brightspot.redirect.QueryStringOptionModify;
import brightspot.redirect.QueryStringOptionPreserve;
import brightspot.redirect.VanityRedirect;
import brightspot.redirectimporter.utils.DefaultImplementationSupplier;
import brightspot.redirectimporter.utils.GoogleCSVUtils;
import com.google.common.base.Throwables;
import com.psddev.cms.db.ExternalItemConverter;
import com.psddev.cms.db.ExternalItemImportException;
import com.psddev.cms.db.Site;
import com.psddev.cms.ui.ToolRequest;
import com.psddev.dari.db.Query;
import com.psddev.dari.web.WebRequest;
import com.psddev.google.drive.GoogleDriveFile;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VanityRedirectGoogleDriveFileConverter extends ExternalItemConverter<GoogleDriveFile, VanityRedirect> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VanityRedirectGoogleDriveFileConverter.class);

    @DisplayName("Overwrite Existing Redirects")
    public boolean overwriteExistingRedirects;

    public boolean isOverwriteExistingRedirects() {
        return overwriteExistingRedirects;
    }

    public void setOverwriteExistingRedirects(boolean overwriteExistingRedirects) {
        this.overwriteExistingRedirects = overwriteExistingRedirects;
    }

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

    private Collection<VanityRedirect> extractRedirects(GoogleDriveFile source) throws IOException {

        Collection<VanityRedirect> result = new ArrayList<>();

        // Extract redirects from the spreadsheet using csv
        CSVParser csvParser = GoogleCSVUtils.getCsv(source, true);

        List<CSVRecord> records = csvParser.getRecords();

        if (records == null) {
            throw new ExternalItemImportException("File does not exist!");
        }

        if (records.size() == 0) {
            throw new ExternalItemImportException("File contains no records!");
        }

        // We can look at the first row to determine if the column size is wrong (not 4 columns)
        if (records.get(0).size() != 4) {
            throw new ExternalItemImportException("File contains wrong number of columns!");
        }

        final Map<String, Integer> headerMap = records.get(0).getParser().getHeaderMap();

        // Check if the record names are correct, ignoring case
        List<String> headers = new ArrayList<>(headerMap.keySet());

        if (headers.size() == 4
            && headers.get(0).equalsIgnoreCase("local path")
            && headers.get(1).equalsIgnoreCase("new url")
            && headers.get(2).equalsIgnoreCase("status")
            && headers.get(3).equalsIgnoreCase("query string")) {
            throw new ExternalItemImportException("File contains incorrect header names or they are in the wrong order!");
        }

        Site site = WebRequest.getCurrent().as(ToolRequest.class).getCurrentSite();

        if (site == null) {
            throw new IllegalStateException("An owner Site must be specified.");
        }

        // Iterating over spreadsheet rows
        for (CSVRecord csvRecord : records) {

            String localPath = csvRecord.get(0);
            String newUrl = csvRecord.get(1);
            String status = csvRecord.get(2);
            String queryString = csvRecord.get(3);

            // Attempt to find existing vanity redirect local path that is identical to the current rows local path
            VanityRedirect existingVanityRedirect = Query.from(VanityRedirect.class).where("localUrls = ?", localPath).first();

            String existingVanityRedirectLocalPath = "";

            if (existingVanityRedirect != null) {
                existingVanityRedirectLocalPath = existingVanityRedirect.getLocalUrls().stream().findFirst().orElse(null);
            }

            if (existingVanityRedirect == null) {

                VanityRedirect vanityRedirect = new VanityRedirect();
                vanityRedirect = setVanityRedirectValues(vanityRedirect, localPath, newUrl, status, queryString, site);
                result.add(vanityRedirect);

            } else if (existingVanityRedirect.getLocalUrls().size() == 1
               && Objects.equals(localPath, existingVanityRedirectLocalPath)
               && isOverwriteExistingRedirects()) {

                // If existing vanity redirect local url exists, and overwrite existing redirects boolean is checked,
                // replace instead of creating a new one
                existingVanityRedirect = setVanityRedirectValues(existingVanityRedirect, localPath, newUrl, status, queryString, site);
                result.add(existingVanityRedirect);

            }
        }

        // Return redirects
        return result;
    }

    public VanityRedirect setVanityRedirectValues(VanityRedirect vanityRedirect, String localPath, String newUrl, String status, String queryString, Site site) {

        vanityRedirect.as(Site.ObjectModification.class).setOwner(site);
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

        return vanityRedirect;

    }
}

