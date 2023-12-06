package brightspot.google.drive.docs;

import java.util.Collection;
import java.util.Collections;

import com.psddev.cms.db.Content;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.State;
import com.psddev.google.drive.GoogleDriveFile;
import com.psddev.google.drive.GoogleDriveUtils;

/**
 * Responsible for converting a {@link GoogleDriveFile} to a concrete implementation of {@link GoogleDocumentImport}.
 */
@Recordable.DisplayName("Google Doc")
public class GoogleDriveFileDocumentConverter
        extends UpdatableExternalItemConverter<GoogleDriveFile, GoogleDocumentImport> {

    @Recordable.Embedded
    @Recordable.Required
    @Recordable.DisplayName("Convert To Type")
    private DocumentConverter<?> converter;

    public DocumentConverter<?> getConverter() {
        return converter;
    }

    public void setConverter(DocumentConverter<?> converter) {
        this.converter = converter;
    }

    @Override
    public Collection<? extends GoogleDocumentImport> convert(GoogleDriveFile source) {
        String mimeType = source.getMimeType();

        if (!GoogleDriveUtils.DOCUMENT_MIME_TYPE.equals(mimeType)) {
            throw new IllegalArgumentException(String.format(
                    "Can't convert [%s] into an article!",
                    mimeType));
        }

        DocumentConverter<?> converter = getConverter();
        GoogleDocumentImport importedFile = converter.createType();
        importedFile.as(Content.ObjectModification.class).setDraft(true);
        importedFile.as(GoogleDriveSyncableData.class).setSyncTime(source.getModifiedTime());

        // Set up converter mapping so update knows which converter was used on the converted file
        GoogleDocumentImportToConverterMapping converterMapping = new GoogleDocumentImportToConverterMapping();
        converterMapping.setConverter(converter);
        converterMapping.setFileId(State.getInstance(importedFile).getId());
        converterMapping.save();

        converter.convert(importedFile, source);
        return Collections.singleton(importedFile);
    }

    @Override
    public void update(GoogleDriveFile source, GoogleDocumentImport existingObject) {
        GoogleDocumentImportToConverterMapping converterMapping = Query.from(GoogleDocumentImportToConverterMapping.class)
                .where("fileId = ?", existingObject.getState().getId())
                .first();

        if (converterMapping != null) {
            DocumentConverter<?> converter = converterMapping.getConverter();

            if (converter != null) {
                converter.update(existingObject, source);
            }
        }
    }

    @Override
    public boolean isSupported(GoogleDriveFile item) {
        return GoogleDriveUtils.DOCUMENT_MIME_TYPE.equals(item.getMimeType());
    }
}
