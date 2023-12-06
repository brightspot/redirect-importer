package brightspot.redirectimporter;

import java.util.Map;

import com.psddev.dari.db.Record;
import com.psddev.dari.db.Recordable;

@Recordable.Embedded
public abstract class QueryStringOption extends Record {

    public abstract String modifyUrl(String url, String requestPath, Map<String, String[]> requestParameterMap);
}