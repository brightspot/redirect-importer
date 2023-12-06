package brightspot.google.drive.conversion.vanityredirect;

import java.util.Collections;
import java.util.Set;

import com.psddev.cms.tool.widget.AssociatedContentWidget;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Recordable;

/**
 * Shows the {@link VanityRedirect} objects that were imported from the same Google Sheet.
 */
public class GoogleDriveImportedVanityRedirectWidget extends AssociatedContentWidget {

    private static final String IMPORTED_VANITY_REDIRECT_FIELD =
            GoogleDriveVanityRedirectModification.FIELD_PREFIX + "originalImportedGoogleSheetVanityRedirect";

    @Override
    protected Class<? extends Recordable> getContainingClass() {
        return ImportedGoogleSheetVanityRedirect.class;
    }

    @Override
    protected Set<ObjectType> getAssociatedTypes() {
        return Collections.singleton(ObjectType.getInstance(VanityRedirect.class));
    }

    @Override
    protected Class<? extends Recordable> getAssociationClass() {
        return GoogleDriveVanityRedirectModification.class;
    }

    @Override
    protected String getFullyQualifiedAssociationFieldName() {
        return GoogleDriveVanityRedirectModification.class.getName() + "/" + IMPORTED_VANITY_REDIRECT_FIELD;
    }
}