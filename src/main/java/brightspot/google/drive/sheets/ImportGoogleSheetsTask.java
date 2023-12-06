package brightspot.google.drive.sheets;

import java.util.Date;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import com.google.common.base.Preconditions;
import com.psddev.cms.db.Content;
import com.psddev.cms.db.Site;
import com.psddev.cms.db.ToolUser;
import com.psddev.dari.db.Database;
import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.util.Task;
import com.psddev.dari.util.TypeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Publishes each {@link Record} associated with {@link AbstractImportedGoogleSheet#getCsvRecords()}.
 */
public class ImportGoogleSheetsTask<S extends Recordable> extends Task {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImportGoogleSheetsTask.class);
    private static final String NAME = ImportGoogleSheetsTask.class.getSimpleName();

    private AbstractImportedGoogleSheet<S> googleSheet;
    private ToolUser user;
    private Site site;

    public ImportGoogleSheetsTask(AbstractImportedGoogleSheet<S> googleSheet, ToolUser user, Site site) {
        super(NAME + " Executor", NAME);
        Preconditions.checkNotNull(googleSheet);
        this.googleSheet = googleSheet;
        this.user = user;
        this.site = site;
    }

    @Override
    protected void doTask() {
        ImportGoogleSheetsLog log = new ImportGoogleSheetsLog();
        log.setObjectId(googleSheet.getId());
        log.setSubmissionDate(new Date());
        log.setSubmittedBy(user);
        log.setStatus(ImportGoogleSheetsStatus.IN_PROGRESS);
        log.save();

        Set<Record> recordsToPublish = new LinkedHashSet<>();
        int recordsActuallyPublished = 0;
        Database database = Database.Static.getDefault();
        database.beginIsolatedWrites();

        try {
            GoogleDriveFileSheetConverter converter = TypeDefinition.getInstance(GoogleDriveFileSheetConverter.class)
                    .newInstance();
            converter.getState().setValues(googleSheet.getConverterData());

            for (Map<String, String> csvRecord : googleSheet.getCsvRecords()) {
                recordsToPublish.addAll(googleSheet.buildRecords(converter, csvRecord, user, recordsToPublish));
            }

            if (!recordsToPublish.isEmpty()) {
                for (Record record : recordsToPublish) {
                    if (record != null) {
                        try {
                            Content.Static.publish(record, site, user);
                            recordsActuallyPublished++;
                        } catch (Exception e) {
                            LOGGER.error(String.format("Unable to publish record [%s]", record.getId()), e);
                            log.getErrors().add(e.getMessage());
                        }
                    }
                }
            }

            int recordsToPublishSize = recordsToPublish.size();
            if (recordsActuallyPublished == recordsToPublishSize) {
                log.setStatus(ImportGoogleSheetsStatus.SUCCESS);
            } else if (recordsActuallyPublished < recordsToPublishSize) {
                log.setStatus(ImportGoogleSheetsStatus.PARTIALLY_SUCCESSFUL);
            }
            log.save();

            database.commitWrites();
        } catch (Exception e) {
            LOGGER.error(String.format("Unable to complete import for item [%s]", googleSheet.getState().getId()), e);
            log.getErrors().add(e.getMessage());
            log.setStatus(ImportGoogleSheetsStatus.FAILURE);
            log.save();
        } finally {
            database.endWrites();
        }

    }
}
