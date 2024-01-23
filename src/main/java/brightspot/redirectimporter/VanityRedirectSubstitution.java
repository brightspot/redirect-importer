package brightspot.redirectimporter;

import brightspot.redirect.VanityRedirect;
import com.psddev.dari.db.ObjectIndex;
import com.psddev.dari.util.Substitution;

public class VanityRedirectSubstitution extends VanityRedirect implements Substitution {

    @Override
    protected boolean onDuplicate(ObjectIndex index) {
        return false;
    }
}
