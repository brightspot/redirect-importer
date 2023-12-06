package brightspot.google.drive.sheets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.psddev.cms.db.Content;
import com.psddev.cms.db.ExternalItem;
import com.psddev.cms.db.ExternalItemConverter;
import com.psddev.cms.db.ExternalItemImported;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.db.ToolUser;
import com.psddev.cms.ui.LocalizationContext;
import com.psddev.cms.ui.ToolLocalization;
import com.psddev.cms.ui.ToolRequest;
import com.psddev.cms.ui.form.DynamicNoteMethod;
import com.psddev.dari.db.ObjectType;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.StringException;
import com.psddev.dari.html.Node;
import com.psddev.dari.html.Nodes;
import com.psddev.dari.html.text.TextNode;
import com.psddev.dari.util.CompactMap;
import com.psddev.dari.util.TypeDefinition;
import com.psddev.dari.web.WebRequest;
import com.psddev.google.drive.GoogleDriveFile;
import com.psddev.google.drive.GoogleDriveUtils;

/**
 * The abstract implementation of a middleman record that connects a {@link GoogleDriveFileSheetConverter} to the pieces
 * of content that resulted from the conversion process.
 *
 * @param <S> The type of content that was created from the Google Sheet conversion.
 */
@Content.Searchable
public abstract class AbstractImportedGoogleSheet<S extends Recordable> extends Record implements GoogleDriveSyncable {

    @DynamicNoteMethod("getInfoNoteHtml")
    @ToolUi.ReadOnly
    private String name;

    @ToolUi.ReadOnly
    private ObjectType type;

    @ToolUi.Hidden
    @Raw
    private Map<String, Object> converterData;

    @ToolUi.Hidden
    private List<Map<String, String>> csvRecords;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ObjectType getType() {
        return type;
    }

    public void setType(ObjectType type) {
        this.type = type;
    }

    public Map<String, Object> getConverterData() {
        if (converterData == null) {
            converterData = new CompactMap<>();
        }
        return converterData;
    }

    public void setConverterData(Map<String, Object> converterData) {
        this.converterData = converterData;
    }

    public List<Map<String, String>> getCsvRecords() {
        if (csvRecords == null) {
            csvRecords = new ArrayList<>();
        }
        return csvRecords;
    }

    public void setCsvRecords(List<Map<String, String>> csvRecords) {
        this.csvRecords = csvRecords;
    }

    /**
     * Kicks off a new {@link ImportGoogleSheetsTask} in order to begin the process of converting CSV records to
     * Brightspot records.
     */
    public void processRecords() {
        ToolRequest toolRequest = WebRequest.getCurrent().as(ToolRequest.class);
        new ImportGoogleSheetsTask(this, toolRequest.getCurrentUser(), toolRequest.getCurrentSite()).submit();
    }

    /**
     * As the name suggests, takes the given {@param csvRecord} and builds a corresponding record, along with any
     * additional records necessary to complete construction of the main record's fields.
     */
    public abstract List<Record> buildRecords(
            GoogleDriveFileSheetConverter converter,
            Map<String, String> csvRecord,
            ToolUser user,
            Set<Record> recordsToPublish);

    /**
     * Intended to return a list of strings representing the internal names of fields that should be allowed as options
     * for column headers coming from the Google Sheet; i.e., these are the fields that will be converted in
     * {@link AbstractImportedGoogleSheet#buildRecords}.
     */
    public abstract List<String> getFieldsToConvert();

    /**
     * Intended to return a list of strings representing the internal names of fields that should be excluded as options
     * for column headers coming from the Google sheet. This can be used if there is a {@link
     * Recordable.Required} field you need to exempt from a field mapping as its value gets populated elsewhere. Note
     * that this could potentially break importing functionality if a field is excluded that needs to have a value
     * mapped on import.
     *
     * <p>Overrides field's specified in {@link #getFieldsToConvert()} as well as any {@link Recordable.Required}
     * fields.</p>
     *
     * @return Empty list by default, i.e. no fields are explicitly excluded
     */
    public List<String> getFieldsToExclude() {
        return Collections.emptyList();
    }

    /**
     * Used to add custom validation to a {@link GoogleDriveFileSheetConverter} specific to the generic type
     * {@link S}. Should throw a {@link StringException} with a helpful message for the user if the mappings are invalid.
     * Default implementation assumes no validation is necessary.
     *
     * @param fieldMappings The mappings specified in the {@link GoogleDriveFileSheetConverter} that will be
     * converted to an instance of {@link AbstractImportedGoogleSheet}.
     */
    public void validateMappings(List<GoogleSheetsFieldMapping> fieldMappings) throws StringException {
    }

    @Override
    public Object synchronizeFromSource() {
        reimport(this);
        setSyncTime(this);
        this.save();
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void reimport(GoogleDriveSyncable syncable) {
        try {
            ExternalItemImported externalItemImported = this.as(ExternalItemImported.class);
            GoogleDriveFile sourceFile = GoogleDriveUtils.getFile(externalItemImported);
            List<Class<?>> converterClasses = externalItemImported.getOrFindConverterClass(
                    sourceFile.getClass(),
                    this.getClass());

            for (Class<?> converterClass : converterClasses) {
                ExternalItemConverter<ExternalItem, Recordable> converter = (ExternalItemConverter<ExternalItem, Recordable>)
                        TypeDefinition.getInstance(converterClass).newInstance();
                if (converter instanceof UpdatableExternalItemConverter) {
                    converter.getState().setValues(this.getConverterData());
                    ((UpdatableExternalItemConverter) converter).update(sourceFile, syncable);
                }
            }
        } catch (Exception e) {
            LOGGER.error(String.format(
                    "Unable to reimport item [%s] due to [%s] ",
                    this.getState().getId(),
                    e.getMessage()));
        }
    }

    private Node getInfoNoteHtml() {
        ImportGoogleSheetsLog log = Query.from(ImportGoogleSheetsLog.class)
                .where("objectId = ?", this.getId())
                .sortDescending("submissionDate")
                .first();
        ToolRequest toolRequest = WebRequest.getCurrent().as(ToolRequest.class);
        if (log == null) {
            return null;
        } else if (ImportGoogleSheetsStatus.IN_PROGRESS.equals(log.getStatus())) {
            return new TextNode(ToolLocalization.text(AbstractImportedGoogleSheet.class, "label.inProgress"));
        } else if (ImportGoogleSheetsStatus.FAILURE.equals(log.getStatus())) {
            return Nodes.raw(ToolLocalization.text(
                    new LocalizationContext(
                            AbstractImportedGoogleSheet.class,
                            Collections.singletonMap(
                                    "rawResponseUrl",
                                    toolRequest.getPathBuilder("/contentRaw").addParameter("id", log.getId()).build())),
                    "link.viewFailedLog"));
        } else {
            return Nodes.raw(ToolLocalization.text(
                    new LocalizationContext(
                            AbstractImportedGoogleSheet.class,
                            Collections.singletonMap(
                                    "rawResponseUrl",
                                    toolRequest.getPathBuilder("/contentRaw").addParameter("id", log.getId()).build())),
                    "link.viewLog"));

        }
    }
}
