package brightspot.redirectimporter;

import java.util.Map;

import com.psddev.dari.db.Recordable;

@Recordable.DisplayName("Ignore")
public class QueryStringOptionIgnore extends QueryStringOption {

    @Override
    public String modifyUrl(String url, String requestPath, Map<String, String[]> requestParameterMap) {
        return url;
    }
}