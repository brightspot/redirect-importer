package brightspot.google.drive.docs;

import java.util.UUID;

import com.psddev.dari.db.Modification;
import com.psddev.dari.db.Query;

/**
 * Handles the deletion of a {@link GoogleDocumentImportToConverterMapping} when the associated
 * {@link GoogleDocumentImport} is deleted.
 */
public class GoogleDocumentImportModification extends Modification<GoogleDocumentImport> {

    @Override
    protected void afterDelete() {
        super.afterDelete();

        UUID fileId = getOriginalObject().getState().getId();
        if (fileId != null) {
            Query.from(GoogleDocumentImportToConverterMapping.class).where("fileId = ?", fileId).deleteAll();
        }
    }
}
