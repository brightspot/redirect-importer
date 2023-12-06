package brightspot.redirectimporter;

import com.psddev.cms.db.ToolUi;
import com.psddev.dari.db.Modification;
import com.psddev.dari.db.Recordable;

/**
 * {@link Modification} that associates an {@link ImportedGoogleSheetVanityRedirect} to it's corresponding
 * {@link VanityRedirect}.
 */
@Recordable.FieldInternalNamePrefix(GoogleDriveVanityRedirectModification.FIELD_PREFIX)
public class GoogleDriveVanityRedirectModification extends Modification<VanityRedirect> {

    static final String FIELD_PREFIX = "google.drive.";

    @Indexed
    @ToolUi.Hidden
    private String importKey;

    @Indexed
    @ToolUi.Hidden
    private ImportedGoogleSheetVanityRedirect originalImportedGoogleSheetVanityRedirect;

    public String getImportKey() {
        return importKey;
    }

    public void setImportKey(String importKey) {
        this.importKey = importKey;
    }

    public ImportedGoogleSheetVanityRedirect getOriginalImportedGoogleSheetVanityRedirect() {
        return originalImportedGoogleSheetVanityRedirect;
    }

    public void setOriginalImportedGoogleSheetVanityRedirect(ImportedGoogleSheetVanityRedirect originalImportedGoogleSheetVanityRedirect) {
        this.originalImportedGoogleSheetVanityRedirect = originalImportedGoogleSheetVanityRedirect;
    }
}