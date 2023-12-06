package brightspot.google.drive.sheets;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.psddev.cms.db.ToolUi;
import com.psddev.cms.ui.form.Note;
import com.psddev.dari.db.DatabaseEnvironment;
import com.psddev.dari.db.ObjectField;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.StringException;
import com.psddev.dari.util.ClassFinder;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.util.TypeDefinition;
import com.psddev.google.drive.GoogleDriveFile;
import com.psddev.google.drive.GoogleDriveUtils;
import org.jooq.tools.csv.CSVParser;

/**
 * Responsible for converting a {@link GoogleDriveFile} to a concrete implementation of
 * {@link AbstractImportedGoogleSheet}.
 */
@Recordable.DisplayName("Google Sheet")
public class GoogleDriveFileSheetConverter
    extends UpdatableExternalItemConverter<GoogleDriveFile, AbstractImportedGoogleSheet> {

    @ToolUi.ValueGeneratorClass(ValidGoogleSheetsConversionTypesValueGenerator.class)
    @Recordable.Required
    private String convertToType;

    @Recordable.DisplayName("Has Header Row?")
    private Boolean headerRow;

    @Recordable.Embedded
    @Note("Any columns not mapped to a field will be excluded from import.")
    private List<GoogleSheetsFieldMapping> fieldMappings;

    public String getConvertToType() {
        return convertToType;
    }

    public void setConvertToType(String convertToType) {
        this.convertToType = convertToType;
    }

    public boolean hasHeaderRow() {
        return Boolean.TRUE.equals(headerRow);
    }

    public void setHeaderRow(boolean headerRow) {
        this.headerRow = headerRow ? Boolean.TRUE : null;
    }

    public List<GoogleSheetsFieldMapping> getFieldMappings() {
        if (fieldMappings == null) {
            fieldMappings = new ArrayList<>();
        }
        return fieldMappings;
    }

    public void setFieldMappings(List<GoogleSheetsFieldMapping> fieldMappings) {
        this.fieldMappings = fieldMappings;
    }

    @Override
    public Collection<? extends AbstractImportedGoogleSheet> convert(GoogleDriveFile source) {
        String mimeType = source.getMimeType();

        if (!GoogleDriveUtils.SHEET_MIME_TYPE.equals(mimeType)) {
            throw new IllegalArgumentException(String.format(
                "Can't import from [%s]!",
                mimeType));
        }

        Class<?> sheetClass = findSheetClass(ObjectUtils.getClassByName(getConvertToType()));

        if (sheetClass != null) {
            return Collections.singleton(ObjectUtils.build(
                (AbstractImportedGoogleSheet) TypeDefinition.getInstance(sheetClass).newInstance(),
                sheet -> {
                    sheet.setName(source.getName());
                    sheet.setType(ObjectType.getInstance(getConvertToType()));
                    sheet.asSyncableData().setSyncTime(source.getModifiedTime());
                    sheet.setConverterData(this.getState().getSimpleValues());
                    update(source, sheet);
                }));
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public boolean isSupported(GoogleDriveFile item) {
        return GoogleDriveUtils.SHEET_MIME_TYPE.equals(item.getMimeType());
    }

    @Override
    public void update(GoogleDriveFile source, AbstractImportedGoogleSheet existingObject) {
        CSVParser csv = GoogleDriveUtils.getCsv(source, hasHeaderRow());

        try {
            List<Map<String, String>> csvRecords = new ArrayList<>();
            csv.getRecords().forEach(csvRecord -> csvRecords.add(csvRecord.toMap()));
            existingObject.setCsvRecords(csvRecords);

            if (hasHeaderRow() || !getFieldMappings().isEmpty()) {
                existingObject.processRecords();
            }
        } catch (IOException error) {
            throw new RuntimeException(
                "Can't export the Google Drive file into CSV!",
                error);
        }
    }

    /**
     * Validates that all required fields are mapped to a column, excluding any fields returned by {@link AbstractImportedGoogleSheet#getFieldsToExclude()}.
     */
    @Override
    protected void onValidate() {
        super.onValidate();

        List<String> mappedFields = getFieldMappings().stream()
            .map(GoogleSheetsFieldMapping::getField)
            .collect(Collectors.toList());

        ObjectType convertToTypeType = DatabaseEnvironment.getCurrent().getTypeByName(convertToType);

        Class<?> sheetClass = findSheetClass(ObjectUtils.getClassByName(convertToType));
        List<String> excludedFields = sheetClass != null
            ? ((AbstractImportedGoogleSheet<?>) TypeDefinition.getInstance(sheetClass)
            .newInstance()).getFieldsToExclude()
            : Collections.emptyList();

        List<ObjectField> requiredFields = convertToTypeType != null ? convertToTypeType.getFields()
            .stream()
            .filter(ObjectField::isRequired)
            .filter(f -> !excludedFields.contains(f.getInternalName()))
            .collect(Collectors.toList()) : Collections.emptyList();

        if ((ObjectUtils.isBlank(mappedFields) && !ObjectUtils.isBlank(requiredFields))
            || !requiredFields.stream().allMatch(f -> mappedFields.contains(f.getInternalName()))) {
            requiredFields.removeIf(f -> mappedFields.contains(f.getInternalName()));
            getState().addError(
                getState().getField("fieldMappings"),
                new StringException("All required fields must be mapped to a column: " + requiredFields.stream()
                    .map(ObjectField::getLabel)
                    .collect(Collectors.joining(", "))));
        }

        if (sheetClass != null) {
            try {
                ((AbstractImportedGoogleSheet<?>) TypeDefinition.getInstance(sheetClass)
                    .newInstance()).validateMappings(getFieldMappings());
            } catch (StringException e) {
                getState().addError(getState().getField("fieldMappings"), e);
            }
        }
    }

    @Override
    protected void beforeSave() {
        super.beforeSave();
        getFieldMappings().forEach(f -> f.setGoogleDriveFileSheetConverter(this));
    }

    /**
     * Utility method for finding the concrete implementation of {@link AbstractImportedGoogleSheet} that is targeting
     * the given {@param targetClass}.
     *
     * @return Nullable.
     */
    static Class<?> findSheetClass(Class<?> targetClass) {
        return ClassFinder.findConcreteClasses(AbstractImportedGoogleSheet.class)
            .stream()
            .filter(c -> TypeDefinition.getInstance(c)
                .getInferredGenericTypeArgumentClass(AbstractImportedGoogleSheet.class, 0)
                .equals(targetClass))
            .findFirst()
            .orElse(null);
    }
}
