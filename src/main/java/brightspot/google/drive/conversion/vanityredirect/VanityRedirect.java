package brightspot.google.drive.conversion.vanityredirect;

import java.util.Optional;
import java.util.UUID;

import com.psddev.cms.db.Directory;
import com.psddev.dari.db.ObjectIndex;
import com.psddev.dari.db.Query;
import com.psddev.dari.db.Recordable;
import com.psddev.dari.db.State;
import com.psddev.dari.util.ObjectUtils;

@Recordable.DisplayName("Vanity URL Redirect")
public class VanityRedirect extends AbstractRedirect {

    @Override
    protected void onValidate() {
        super.onValidate();

        if (getLocalUrls().stream().anyMatch(url -> url != null && url.contains("*"))) {

            getState().addError(getState().getField("localUrls"), "Invalid Local URL: cannot contain '*'");
        }
    }

    @Override
    protected boolean onDuplicate(ObjectIndex index) {
        if (Directory.PATHS_FIELD.equals(index.getField())) {

            Object value = index.getValue(getState());
            if (!ObjectUtils.isBlank(value)) {

                UUID duplicateId = Optional.ofNullable(Query
                                .from(Object.class)
                                .where("id != ?", getId())
                                .and(index.getUniqueName() + " = ?", value)
                                .using(getState().getDatabase())
                                .referenceOnly()
                                .first())
                        .map(State::getInstance)
                        .map(State::getId)
                        .orElse(null);

                getState().addError(
                        getState().getField("localUrls"),
                        "Must be unique"
                                + (duplicateId != null ? " but duplicate at " + duplicateId : "")
                                + "!");
            }
        }

        return super.onDuplicate(index);
    }
}