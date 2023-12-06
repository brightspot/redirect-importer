package brightspot.google.drive;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.google.api.services.drive.Drive;
import com.google.common.base.Throwables;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.file.MetadataAfterSave;
import com.psddev.cms.tool.file.MetadataBeforeSave;
import com.psddev.cms.ui.form.DynamicNoteMethod;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.util.AbstractStorageItem;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.RandomUuidStorageItemPathGenerator;
import com.psddev.dari.util.StorageItem;
import com.psddev.dari.util.UuidUtils;
import com.psddev.dari.web.WebRequest;
import com.psddev.google.drive.GoogleDriveFile;
import com.psddev.google.drive.GoogleDriveUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link UpdatableExternalItemConverter} to convert files from Google Drive into one of the available supported file
 * types. Users can choose which {@link GoogleDriveImport} type to convert to.
 */
@Recordable.DisplayName("Google Drive File to Attachment")
public class GoogleDriveFileConverter extends UpdatableExternalItemConverter<GoogleDriveFile, GoogleDriveImport> {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleDriveFileConverter.class);

    private static final String DEFAULT_AUDIO_MIME = "audio/x-m4a";

    private static final String DEFAULT_ATTACHMENT_MIME = "text/plain";

    private static final String DEFAULT_DOCUMENT_MIME = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

    private static final String DEFAULT_IMAGE_MIME = "image/jpeg";

    private static final String DEFAULT_PRESENTATION_MIME = "application/vnd.openxmlformats-officedocument.presentationml.presentation";

    private static final String DEFAULT_SPREADSHEET_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    private static final String DEFAULT_VIDEO_MIME = "video/mp4";

    public static final String GOOGLE_FORM_MIME = "application/vnd.google-apps.form";

    private static final int MAX_STORAGE_ITEM_PATH_BYTES = 255;

    @DynamicNoteMethod("getImportTypeNoteHtml")
    @ToolUi.ValueGeneratorClass(GoogleDriveImportFileTypeValueGenerator.class)
    @Recordable.Required
    private String importType;

    public String getImportType() {
        return importType;
    }

    public void setImportType(String importType) {
        this.importType = importType;
    }

    @Override
    public Collection<? extends GoogleDriveImport> convert(GoogleDriveFile googleDriveFile) {
        try {
            StorageItem file = createStorageItem(googleDriveFile);
            if (file != null) {
                GoogleDriveImport googleDriveImport = buildImport(googleDriveFile, file);
                setStorageItemPath(googleDriveFile, file, googleDriveImport);

                new MetadataBeforeSave().beforeSave(file, null);
                file.save();
                new MetadataAfterSave().afterSave(file);
                return Collections.singletonList(googleDriveImport);
            }
        } catch (IOException exception) {
            LOGGER.error("Could not download file: " + exception.getMessage());
            Throwables.propagate(exception);
        }
        return Collections.emptyList();
    }

    @Override
    public void update(GoogleDriveFile source, GoogleDriveImport existingObject) {
        if (existingObject != null && source != null) {
            existingObject.setTitleFromGoogleDrive(source.getName());
            StorageItem file = createStorageItem(source);

            if (file != null) {
                try {
                    setStorageItemPath(source, file, existingObject);
                } catch (IOException e) {
                    LOGGER.error("Could not download file: " + e.getMessage());
                    Throwables.propagate(e);
                }
            }

            existingObject.setFileFromGoogleDrive(file);
        }
    }

    private static StorageItem createStorageItem(GoogleDriveFile source) {

        Collection<? extends GoogleDriveImport> result = new ArrayList<>();
        try {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            Drive drive = GoogleDriveUtils.initializeApi(source.getAccountAccess());
            String newMimeType;
            String googleMimeType = source.getMimeType();

            /* Removes leading and trailing whitespace from name so that it may match
               the StorageItem URl generated from path */
            Optional.ofNullable(source.getName())
                .map(String::trim)
                .ifPresent(source::setName);

            if (googleMimeType.contains("google")) {
                newMimeType = getMimeTypeForGoogleType(googleMimeType);
                drive.files().export(
                    source.getFileId(),
                    newMimeType).executeMediaAndDownloadTo(outputStream);
            } else {
                newMimeType = googleMimeType;
                drive.files().get(source.getFileId()).setAlt("media").executeMediaAndDownloadTo(outputStream);
            }

            StorageItem file = StorageItem.Static.create();
            file.setContentType(newMimeType);
            byte[] output = outputStream.toByteArray();
            file.setData(new ByteArrayInputStream(output));
            Optional.ofNullable(file.getMetadata())
                .ifPresent(meta -> meta.put(
                    AbstractStorageItem.HTTP_HEADERS,
                    Collections.singletonMap(
                        "Content-Length",
                        Collections.singletonList(String.valueOf(output.length)))));

            return file;
        } catch (GeneralSecurityException | IOException exception) {
            LOGGER.error("Could not download file: " + exception.getMessage());
            Throwables.propagate(exception);
        }

        return null;
    }

    private static Map<String, Object> initializeEmptyMetadata() {
        Map<String, Object> metadata = new CompactMap<>();
        metadata.put("cms.edits", null);
        metadata.put("cms.focus", null);
        metadata.put("cms.crops", null);
        return metadata;
    }

    static String getMimeTypeForGoogleType(String googleType) {
        String result;
        if (googleType.toLowerCase(Locale.ROOT).contains("audio")) {
            result = DEFAULT_AUDIO_MIME;
        } else if (googleType.toLowerCase(Locale.ROOT).contains("document")) {
            result = DEFAULT_DOCUMENT_MIME;
        } else if (googleType.toLowerCase(Locale.ROOT).contains("image")) {
            result = DEFAULT_IMAGE_MIME;
        } else if (googleType.toLowerCase(Locale.ROOT).contains("spreadsheet")) {
            result = DEFAULT_SPREADSHEET_MIME;
        } else if (googleType.toLowerCase(Locale.ROOT).contains("presentation")) {
            result = DEFAULT_PRESENTATION_MIME;
        } else if (googleType.toLowerCase(Locale.ROOT).contains("video")) {
            result = DEFAULT_VIDEO_MIME;
        } else {
            result = DEFAULT_ATTACHMENT_MIME;
        }

        return result;
    }

    private GoogleDriveImport buildImport(GoogleDriveFile googleDriveFile, StorageItem file)
        throws IOException {
        ObjectType type = ObjectType.getInstance(UuidUtils.fromString(getImportType()));

        GoogleDriveImport googleDriveImport = Optional.ofNullable(type)
            .map(t -> t.createObject(null))
            .map(GoogleDriveImport.class::cast)
            .orElse(null);
        if (googleDriveImport == null) {
            throw new IOException(String.format(
                "Could not create GoogleDriveImport from type: [%s].",
                getImportType()));
        }

        file.setMetadata(initializeEmptyMetadata());

        googleDriveImport.setTitleFromGoogleDrive(googleDriveFile.getName());
        googleDriveImport.setFileFromGoogleDrive(file);
        googleDriveImport.as(GoogleDriveSyncableData.class).setSyncTime(googleDriveFile.getModifiedTime());

        return googleDriveImport;
    }

    private static void setStorageItemPath(
        GoogleDriveFile googleDriveFile,
        StorageItem file,
        GoogleDriveImport googleDriveImport) throws IOException {

        String extension = googleDriveImport.getDefaultGoogleDriveExtension();
        if (extension == null) {
            throw new IOException(String.format(
                "MIME type [%s] not supported by Google Drive file converter",
                googleDriveFile.getMimeType()));
        }

        String driveFileName = googleDriveFile.getName();
        String fileName = StringUtils.appendIfMissing(
            driveFileName,
            FilenameUtils.getExtension(driveFileName),
            extension);

        String path = new RandomUuidStorageItemPathGenerator().createPath(fileName);
        if (path.length() > MAX_STORAGE_ITEM_PATH_BYTES) {
            path = path.substring(0, MAX_STORAGE_ITEM_PATH_BYTES);
        }

        file.setPath(path);
    }

    private String getImportTypeNoteHtml() {
        UUID externalObjectId = WebRequest.getCurrent().getParameter(UUID.class, "externalObjectId");
        GoogleDriveFile googleDriveFile = Query.from(GoogleDriveFile.class).where("_id = ?", externalObjectId).first();
        String googleDriveMimeType = Optional.ofNullable(googleDriveFile)
            .map(GoogleDriveFile::getMimeType)
            .orElse("");

        if (googleDriveMimeType.equals(GOOGLE_FORM_MIME)) {
            return "Google Form not currently supported as an import type.";
        }

        return null;
    }
}
