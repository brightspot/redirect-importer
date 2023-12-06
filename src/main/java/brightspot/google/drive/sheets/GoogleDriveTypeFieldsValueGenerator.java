package brightspot.google.drive.sheets;

import java.util.List;
import java.util.stream.Collectors;

import com.psddev.cms.db.ToolUi;
import com.psddev.cms.db.ValueGenerator;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.ui.ToolLocalization;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.TypeDefinition;

/**
 * Returns a {@link List} of {@link com.psddev.cms.db.ValueGenerator.Value}s representing the available fields from the
 * import file type to be mapped to corresponding header row values.
 */
public class GoogleDriveTypeFieldsValueGenerator implements ValueGenerator {

    @Override
    public List<Value> generate(ToolPageContext page, Object object, String input) {
        if (object instanceof GoogleSheetsFieldMapping) {
            GoogleDriveFileSheetConverter converter = ((GoogleSheetsFieldMapping) object).getGoogleDriveFileSheetConverter();

            if (converter == null) {
                return null;
            }

            Class<?> sheetClass = GoogleDriveFileSheetConverter.findSheetClass(ObjectUtils.getClassByName(converter.getConvertToType()));

            if (sheetClass != null) {
                AbstractImportedGoogleSheet abstractImportedGoogleSheet = (AbstractImportedGoogleSheet) TypeDefinition.getInstance(
                        sheetClass)
                    .newInstance();
                return ObjectType.getInstance(ObjectUtils.getClassByName(converter.getConvertToType()))
                    .getFields().stream()
                    .filter(f -> !f.as(ToolUi.class).isReadOnly())
                        .filter(f -> abstractImportedGoogleSheet.getFieldsToConvert().contains(f.getInternalName())
                                || f.isRequired())
                        .filter(f -> !abstractImportedGoogleSheet.getFieldsToExclude()
                            .contains(f.getInternalName()))
                        .map(field -> {
                            String internalName = field.getInternalName();
                            return Value.withLabel(
                                    internalName,
                                    ToolLocalization.text(field, "field." + internalName, field.getDisplayName())
                                            + (field.isRequired()
                                            ? " " + ToolLocalization.text(null, "placeholder.required")
                                            : ""));
                        })
                        .filter(ValueGenerator.inputFilterPredicate(input))
                        .collect(Collectors.toList());
            }
        }
        return null;
    }
}
