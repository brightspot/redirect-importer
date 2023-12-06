package brightspot.google.drive.sheets;

import java.util.ArrayList;
import java.util.List;

import com.psddev.cms.db.ValueGenerator;
import com.psddev.cms.tool.ClassDisplay;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.cms.ui.ToolLocalization;
import com.psddev.dari.util.TypeDefinition;

/**
 * Returns a {@link List} of {@link com.psddev.cms.db.ValueGenerator.Value}s representing the available import types for
 * a Google Drive sheet.
 */
public class ValidGoogleSheetsConversionTypesValueGenerator implements ValueGenerator {

    @Override
    public List<Value> generate(ToolPageContext page, Object object, String input) {
        List<Value> validTypes = new ArrayList<>();
        for (Class<?> sheetClass : ClassDisplay.findConcreteClasses(
            AbstractImportedGoogleSheet.class)) {
            Class<?> genericClass = TypeDefinition.getInstance(sheetClass)
                .getInferredGenericTypeArgumentClass(AbstractImportedGoogleSheet.class, 0);
            validTypes.add(Value.withLabel(
                genericClass.getCanonicalName(),
                ToolLocalization.text(genericClass, "displayName")));
        }
        return validTypes;
    }
}
