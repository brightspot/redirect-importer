package brightspot.google.drive.sheets;

import com.psddev.dari.util.Utils;

/**
 * Enumeration of the possible states that a {@link ImportGoogleSheetsLog} can be in.
 */
public enum ImportGoogleSheetsStatus {

    IN_PROGRESS,
    PARTIALLY_SUCCESSFUL,
    SUCCESS,
    FAILURE;

    @Override
    public String toString() {
        return Utils.toLabel(name());
    }
}
