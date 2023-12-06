package brightspot.google.drive;

import com.psddev.dari.util.StorageItem;

/**
 * Used to denote if content is importable from GoogleDrive
 */
public interface GoogleDriveImport extends GoogleDriveSyncable {

    /**
     * @return the default file extension of the implementing type
     */
    String getDefaultGoogleDriveExtension();

    /**
     * This method should be used to set the file field of the implementing type from the Google Drive file.
     *
     * @param file The Google Drive file
     */
    void setFileFromGoogleDrive(StorageItem file);

    /**
     * This method should be used to set the title field of the implementing type from the title supplied by the Google
     * Drive file.
     *
     * @param title The title of the Google Drive file
     */
    void setTitleFromGoogleDrive(String title);

    /**
     * This method should be used to check if the Google Drive file can be imported as the implementing type.
     *
     * @param mimeType The mime type of the Google Drive file to be imported
     * @return true if Google Drive import is supported for the given mime type, otherwise false
     */
    boolean supportsGoogleDriveImport(String mimeType);
}
