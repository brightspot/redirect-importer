package brightspot.google.drive;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.google.api.services.drive.Drive;
import com.psddev.cms.tool.file.MetadataAfterSave;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.RandomUuidStorageItemPathGenerator;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.UuidUtils;
import com.psddev.google.GoogleAccountAccess;
import com.psddev.google.drive.GoogleDriveFile;
import org.apache.commons.io.FilenameUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zwobble.mammoth.Result;
import org.zwobble.mammoth.images.Image;

import static com.psddev.google.drive.GoogleDriveUtils.*;

public final class GoogleDriveUtilsExtended {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleDriveUtilsExtended.class);

    private static final String DEFAULT_DOCUMENT_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private static final String DEFAULT_IMAGE_MIME = "image/jpeg";

    private static final int MAX_STORAGE_ITEM_PATH_BYTES = 255;

    private static int imageCounter = 1;

    private GoogleDriveUtilsExtended() {
    }

    public static Element getHtml(GoogleDriveFile source) {
        if (source == null) {
            return null;
        }

        imageCounter = 1;
        org.zwobble.mammoth.DocumentConverter converter = new org.zwobble.mammoth.DocumentConverter()
            .addStyleMap("u => u") // Underlined text is ignored by default since it can appear like a hyperlink
            .preserveEmptyParagraphs()
            .imageConverter(image -> convertImageAndCreateStorageItem(image, source.getName()));

        Result<String> result;

        try (InputStream input = getFiles(source.getAccountAccess())
            .export(source.getFileId(), DEFAULT_DOCUMENT_MIME)
            .executeMediaAsInputStream()) {
            result = converter.convertToHtml(input);
        } catch (IOException error) {
            throw new RuntimeException(
                "Can't export the Google Drive file into HTML!",
                error);
        }

        if (!result.getWarnings().isEmpty()) {
            LOGGER.warn(
                "Warnings received when converting Microsoft Drives File with ID [{}] to HTML:",
                source.getFileId());
            result.getWarnings().forEach(LOGGER::warn);
        }

        String htmlStr = result.getValue();
        Document document = Jsoup.parse(htmlStr);
        Element body = document.body();

        // Convert embedded hyperlinks to LinkRichTextElements
        for (Element anchor : body.select("a")) {
            String hrefAttrKey = "href";
            String origHref = anchor.attr(hrefAttrKey);
            try {
                List<NameValuePair> params = URLEncodedUtils.parse(
                    new URI(origHref),
                    StandardCharsets.UTF_8);

                params.stream()
                    .filter(param -> "q".equals(param.getName()))
                    .findFirst().ifPresent(qParam -> anchor.attr(hrefAttrKey, qParam.getValue()));

            } catch (URISyntaxException e) {
                LOGGER.warn(String.format("Unable to convert embedded hyperlink [%s]", origHref), e);
            }
        }

        // Remove styling from table cells
        for (Element td : body.select("td")) {
            td.removeAttr("style");
        }

        // Remove H1 elements but keep their text
        for (Element h1 : body.select("h1")) {
            h1.unwrap();
        }

        document.outputSettings().prettyPrint(false);
        return body;
    }

    private static Map<String, String> convertImageAndCreateStorageItem(Image image, String filename) {
        String fileTitle = Optional.ofNullable(filename)
            .map(FilenameUtils::removeExtension)
            .orElse(null);
        String imageFilename = fileTitle + "_image_" + imageCounter++;
        String path = new RandomUuidStorageItemPathGenerator().createPath(fileTitle) + "/" + imageFilename;
        if (path.length() > MAX_STORAGE_ITEM_PATH_BYTES) {
            path = path.substring(0, MAX_STORAGE_ITEM_PATH_BYTES);
        }
        StorageItem file = StorageItem.Static.create();
        file.setContentType(Optional.ofNullable(image.getContentType()).orElse(DEFAULT_IMAGE_MIME));
        file.setPath(path);
        int imageSizeBytes;
        try {
            file.setData(image.getInputStream());
            imageSizeBytes = image.getInputStream().available();
            file.save();
        } catch (IOException e) {
            LOGGER.warn("Could not retrieve input stream from image and save!", e);
            return null;
        }
        new MetadataAfterSave().afterSave(file);

        Map<String, String> attributes = new HashMap<>();
        attributes.put("src", file.getSecurePublicUrl());
        attributes.put("data-storage", file.getStorage());
        attributes.put("data-path", file.getPath());
        attributes.put("data-contentType", file.getContentType());
        attributes.put("data-metadata", ObjectUtils.toJson(file.getMetadata()));

        String identifier = imageSizeBytes + filename + fileTitle;
        String hashedIdentifier = UuidUtils.createVersion3Uuid(identifier).toString();
        attributes.put("checksum", hashedIdentifier);

        return attributes;
    }

    /**
     * Returns all the files accessible by this {@link GoogleAccountAccess}.
     */
    private static Drive.Files getFiles(GoogleAccountAccess access) {
        try {
            return initializeApi(access).files();

        } catch (GeneralSecurityException | IOException error) {
            throw new IllegalStateException(
                    "Can't initialize the Google Drive API!",
                    error);
        }
    }
}
