package brightspot.google.drive.docs;

import java.util.UUID;

import com.psddev.dari.db.Record;

/**
 * {@link Record} for mapping a {@link GoogleDocumentImport} to the {@link DocumentConverter} implementation used for
 * the initial import, so this same {@link DocumentConverter} can be used on subsequent sync calls.
 */
public class GoogleDocumentImportToConverterMapping extends Record {

    @Indexed
    private UUID fileId;

    private DocumentConverter converter;

    public UUID getFileId() {
        return fileId;
    }

    public void setFileId(UUID fileId) {
        this.fileId = fileId;
    }

    public DocumentConverter getConverter() {
        return converter;
    }

    public void setConverter(DocumentConverter converter) {
        this.converter = converter;
    }
}
