package brightspot.google.drive.docs;

import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;
import com.psddev.google.drive.GoogleDriveFile;

/**
 * Generic converter for Google Drive document to {@link GoogleDocumentImport}.
 *
 * @param <T> Concrete type extending {@link GoogleDocumentImport}
 */
@Recordable.Embedded
public abstract class DocumentConverter<T extends GoogleDocumentImport> extends Record {

    /**
     * Returns the concrete import type this converter converts Google Drive documents to.
     */
    public abstract T createType();

    /**
     * Handles the initial import of Google Drive document to {@link GoogleDocumentImport}.
     */
    public abstract void convert(GoogleDocumentImport content, GoogleDriveFile fileContext);

    /**
     * Handles the sync when changes have been made in the Google Drive document that need to be brought in.
     */
    public abstract void update(GoogleDocumentImport content, GoogleDriveFile fileContext);
}
