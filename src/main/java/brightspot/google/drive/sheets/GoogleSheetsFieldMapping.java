package brightspot.google.drive.sheets;

import java.util.List;

import com.psddev.cms.db.ToolUi;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;

/**
 * Maps the header row value from the Google Drive sheet to be imported to the corresponding field on the import file
 * type.
 */
@Recordable.Embedded
public class GoogleSheetsFieldMapping extends Record {

    @ToolUi.Hidden
    private transient GoogleDriveFileSheetConverter googleDriveFileSheetConverter;

    @ToolUi.ValueGeneratorClass(GoogleSheetColumnHeadingValueGenerator.class)
    @Required
    private String column;

    @ToolUi.ValueGeneratorClass(GoogleDriveTypeFieldsValueGenerator.class)
    @Required
    private String field;

    public GoogleDriveFileSheetConverter getGoogleDriveFileSheetConverter() {
        return googleDriveFileSheetConverter;
    }

    public void setGoogleDriveFileSheetConverter(GoogleDriveFileSheetConverter googleDriveFileSheetConverter) {
        this.googleDriveFileSheetConverter = googleDriveFileSheetConverter;
    }

    public String getColumn() {
        return column;
    }

    public void setColumn(String column) {
        this.column = column;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    /**
     * Utility method for finding the {@link GoogleSheetsFieldMapping} that corresponds to a particular String
     * {@param identifier}.
     *
     * @return Nullable.
     */
    public static GoogleSheetsFieldMapping findMapping(
        List<GoogleSheetsFieldMapping> fieldMappings,
        String identifier) {
        return fieldMappings.stream()
            .filter(m -> m.getField().contains(identifier))
            .findFirst()
            .orElse(null);
    }
}
