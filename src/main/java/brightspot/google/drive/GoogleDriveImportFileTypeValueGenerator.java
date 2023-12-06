package brightspot.google.drive;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import com.psddev.cms.db.ValueGenerator;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.web.WebRequest;
import com.psddev.google.drive.GoogleDriveFile;

/**
 * Returns a {@link List} of {@link com.psddev.cms.db.ValueGenerator.Value}s representing the {@link GoogleDriveImport}
 * types available for conversion.
 */
public class GoogleDriveImportFileTypeValueGenerator implements ValueGenerator {

    @Override
    public List<Value> generate(ToolPageContext page, Object object, String input) {
        List<Value> availableImportTypes = new ArrayList<>();

        UUID externalObjectId = WebRequest.getCurrent().getParameter(UUID.class, "externalObjectId");
        GoogleDriveFile googleDriveFile = Query.from(GoogleDriveFile.class).where("_id = ?", externalObjectId).first();
        String googleDriveMimeType = Optional.ofNullable(googleDriveFile)
            .map(GoogleDriveFile::getMimeType)
            .orElse("");

        // Google Forms not currently supported as an import type
        if (googleDriveMimeType.equals(GoogleDriveFileConverter.GOOGLE_FORM_MIME)) {
            return Collections.emptyList();
        }

        if (googleDriveMimeType.contains("google")) {
            googleDriveMimeType = GoogleDriveFileConverter.getMimeTypeForGoogleType(googleDriveMimeType);
        }

        for (ObjectType type : ObjectType.getInstance(GoogleDriveImport.class).findConcreteTypes()) {
            GoogleDriveImport googleDriveImport = (GoogleDriveImport) type.createObject(null);

            if (googleDriveImport.supportsGoogleDriveImport(googleDriveMimeType)) {
                availableImportTypes.add(Value.withLabel(String.valueOf(type.getId()), type.getLabel()));
            }
        }

        return availableImportTypes.stream()
            .filter(ValueGenerator.inputFilterPredicate(input))
            .sorted(ValueGenerator.labelSortComparator())
            .collect(Collectors.toList());
    }
}
