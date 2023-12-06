package brightspot.google.drive.conversion.vanityredirect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import brightspot.google.drive.sheets.AbstractImportedGoogleSheet;
import brightspot.google.drive.sheets.GoogleDriveFileSheetConverter;
import brightspot.google.drive.sheets.GoogleSheetsFieldMapping;
import com.psddev.cms.db.Draft;
import com.psddev.cms.db.ToolUser;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * Middleman record connecting a {@link GoogleDriveFileSheetConverter} to the {@link VanityRedirect} that resulted from the
 * conversion process.
 */
public class ImportedGoogleSheetVanityRedirect extends AbstractImportedGoogleSheet<VanityRedirect> {

    public static final List<String> INCOMING_FIELDS = Arrays.asList("displayName", "parent", "description");

    private transient Map<String, String> csvRecord;

    public Map<String, String> getCsvRecord() {
        return csvRecord;
    }

    public void setCsvRecord(Map<String, String> csvRecord) {
        this.csvRecord = csvRecord;
    }

    @Override
    public List<Record> buildRecords(
            GoogleDriveFileSheetConverter converter,
            Map<String, String> csvRecord,
            ToolUser user,
            Set<Record> recordsToPublish) {

        if (!VanityRedirect.class.equals(ObjectUtils.getClassByName(converter.getConvertToType()))) {
            return Collections.emptyList();
        }

        List<Record> records = new ArrayList<>();
        setCsvRecord(csvRecord);
        State originalState = null;
        List<GoogleSheetsFieldMapping> fieldMappings = converter.getFieldMappings();

        GoogleSheetsFieldMapping vanityRedirectMapping = GoogleSheetsFieldMapping.findMapping(
                fieldMappings,
                "displayName");
        VanityRedirect vanityRedirect = findVanityRedirect(vanityRedirectMapping, recordsToPublish);
        if (vanityRedirect == null) {
            vanityRedirect = createVanityRedirect(vanityRedirectMapping);
        } else {
            if (!vanityRedirect.getState().isNew()) {
                originalState = vanityRedirect.getState();
                vanityRedirect = (VanityRedirect) vanityRedirect.clone();
            }
        }

        if (vanityRedirect == null) {
            return records;
        }

        GoogleSheetsFieldMapping parentMapping = GoogleSheetsFieldMapping.findMapping(
                fieldMappings,
                "parent");
        VanityRedirect parentVanityRedirect = findVanityRedirect(parentMapping, recordsToPublish);
        if (parentVanityRedirect == null) {
            parentVanityRedirect = createVanityRedirect(parentMapping);
            if (parentVanityRedirect != null) {
                records.add(parentVanityRedirect);
                vanityRedirect.setParent(parentVanityRedirect);
            }
        } else {
            vanityRedirect.setParent(parentVanityRedirect);
        }

        Optional.ofNullable(GoogleSheetsFieldMapping.findMapping(fieldMappings, "description"))
                .map(GoogleSheetsFieldMapping::getColumn)
                .map(getCsvRecord()::get)
                .filter(StringUtils::isNotBlank)
                .ifPresent(vanityRedirect::setDescription);

        if (originalState != null) {
            Map<String, Map<String, Object>> diffMap = Draft.findDifferences(
                    Database.Static.getDefault().getEnvironment(),
                    originalState.getSimpleValues(),
                    vanityRedirect.getState().getSimpleValues());
            if (!diffMap.isEmpty()) {
                Draft draft = new Draft();
                draft.setObjectType(originalState.getType());
                draft.setObjectId(originalState.getId());
                draft.setDifferences(diffMap);
                draft.setName(GoogleDriveSyncable.generateReimportName());
                draft.setOwner(user);
                records.add(draft);
            }
        } else {
            records.add(vanityRedirect);
        }

        return records;
    }

    @Override
    public List<String> getFieldsToConvert() {
        return INCOMING_FIELDS;
    }

    private VanityRedirect findVanityRedirect(GoogleSheetsFieldMapping fieldMapping, Set<Record> recordsToPublish) {
        String name = Optional.ofNullable(fieldMapping)
                .map(GoogleSheetsFieldMapping::getColumn)
                .map(getCsvRecord()::get)
                .filter(StringUtils::isNotBlank)
                .orElse(null);

        if (name == null) {
            return null;
        }

        return Query.from(VanityRedirect.class)
                .where("google.drive.importKey = ?", name)
                .findFirst()
                .orElseGet(() -> recordsToPublish.stream()
                        .filter(VanityRedirect.class::isInstance)
                        .map(VanityRedirect.class::cast)
                        .filter(s -> s.getDisplayName().equals(name))
                        .findFirst()
                        .orElse(null));
    }

    private VanityRedirect createVanityRedirect(GoogleSheetsFieldMapping fieldMapping) {
        return Optional.ofNullable(fieldMapping)
                .map(GoogleSheetsFieldMapping::getColumn)
                .map(getCsvRecord()::get)
                .filter(StringUtils::isNotBlank)
                .map(name -> ObjectUtils.build(new VanityRedirect(), vanityRedirect -> {
                    vanityRedirect.setInternalName(name);
                    vanityRedirect.setDisplayName(name);
                    GoogleDriveVanityRedirectModification VanityRedirectModification = vanityRedirect.as(
                            GoogleDriveVanityRedirectModification.class);
                    VanityRedirectModification.setOriginalImportedGoogleSheetVanityRedirect(this);
                    VanityRedirectModification.setImportKey(name);
                }))
                .orElse(null);
    }
}