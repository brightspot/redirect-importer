package brightspot.google.drive.sheets;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import com.psddev.cms.db.ToolUi;
import com.psddev.cms.db.ToolUser;
import com.psddev.dari.db.Record;

/**
 * Stores pertinent information related to an import of a Google Sheet as performed by {@link ImportGoogleSheetsTask}.
 */
@ToolUi.ReadOnly
public class ImportGoogleSheetsLog extends Record {

    @Required
    @Indexed
    private UUID objectId;

    @Indexed
    private Date submissionDate;

    private ToolUser submittedBy;

    @Indexed
    private ImportGoogleSheetsStatus status;

    private List<String> errors;

    public UUID getObjectId() {
        return objectId;
    }

    public void setObjectId(UUID objectId) {
        this.objectId = objectId;
    }

    public Date getSubmissionDate() {
        return submissionDate;
    }

    public void setSubmissionDate(Date submissionDate) {
        this.submissionDate = submissionDate;
    }

    public ToolUser getSubmittedBy() {
        return submittedBy;
    }

    public void setSubmittedBy(ToolUser submittedBy) {
        this.submittedBy = submittedBy;
    }

    public ImportGoogleSheetsStatus getStatus() {
        return status;
    }

    public void setStatus(ImportGoogleSheetsStatus status) {
        this.status = status;
    }

    public List<String> getErrors() {
        if (errors == null) {
            errors = new ArrayList<>();
        }
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
