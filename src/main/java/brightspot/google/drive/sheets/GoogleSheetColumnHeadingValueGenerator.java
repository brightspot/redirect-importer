package brightspot.google.drive.sheets;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.psddev.cms.db.ValueGenerator;
import com.psddev.cms.tool.ToolPageContext;
import com.psddev.dari.db.Query;
import com.psddev.dari.web.WebRequest;
import com.psddev.google.drive.GoogleDriveFile;
import com.psddev.google.drive.GoogleDriveUtils;

/**
 * Returns a {@link List} of {@link com.psddev.cms.db.ValueGenerator.Value}s representing the imported sheet's header
 * row values.
 */
public class GoogleSheetColumnHeadingValueGenerator implements ValueGenerator {

    private static final LoadingCache<UUID, Integer> COLUMNS_CACHE = CacheBuilder
        .newBuilder()
        .expireAfterWrite(1, TimeUnit.MINUTES)
        .build(new CacheLoader<UUID, Integer>() {

            @Override
            public Integer load(UUID externalId) {
                return GoogleDriveUtils.getNumberOfColumns(((GoogleDriveFile) Query.fromType(WebRequest.getCurrent()
                        .as(ExternalItemRequest.class)
                        .getExternalType())
                    .where("id = ?", externalId)
                    .first()));
            }
        });

    @Override
    public List<Value> generate(ToolPageContext page, Object object, String input) {
        List<Value> values = new ArrayList<>();
        WebRequest webRequest = WebRequest.getCurrent();
        Object item = webRequest.as(ExternalItemRequest.class).getExternalItem();
        if (item instanceof GoogleDriveFile && object instanceof GoogleSheetsFieldMapping) {
            GoogleDriveFileSheetConverter converter = ((GoogleSheetsFieldMapping) object).getGoogleDriveFileSheetConverter();
            if (converter.hasHeaderRow()) {
                GoogleDriveUtils.getHeaderRow((GoogleDriveFile) item).forEach(v -> values.add(Value.withLabel(v, v)));
            } else {
                try {
                    Integer numColumns = COLUMNS_CACHE.get(webRequest.as(ExternalItemRequest.class).getExternalId());
                    if (numColumns != null) {
                        for (int i = 0; i < numColumns; i++) {
                            String v = GoogleDriveUtils.generateColumnHeading(i);
                            values.add(Value.withLabel(v, v));
                        }
                    }
                } catch (ExecutionException e) {
                    // Ignore.
                }
            }
        }
        return values;
    }
}
