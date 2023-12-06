package brightspot.google.drive.conversion.vanityredirect;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableList;
import com.psddev.cms.db.Content;
import com.psddev.cms.db.Directory;
import com.psddev.cms.db.Site;
import com.psddev.cms.db.ToolUi;
import com.psddev.cms.tool.ContentEditWidgetDisplay;
import com.psddev.cms.tool.content.UrlsWidget;
import com.psddev.cms.ui.ToolRequest;
import com.psddev.cms.ui.form.Note;
import com.psddev.dari.util.ObjectUtils;
import com.psddev.dari.web.WebRequest;
import org.apache.commons.lang3.StringUtils;

@ToolUi.ExcludeFromGlobalSearch
public abstract class AbstractRedirect extends Content implements ContentEditWidgetDisplay {

    /* Hide the URL widget */
    private static final List<String> HIDDEN_WIDGETS = ImmutableList.of(
            UrlsWidget.class.getName()
    );

    // override to provide other mutative functionality
    public String getRedirectDestination(String requestPath, Map<String, String[]> requestParameterMap) {

        String destinationUrl = getDestination();

        // delegate query string logic to the vanity URL redirect
        if (getQueryString() != null) {
            destinationUrl = getQueryString().modifyUrl(destinationUrl, requestPath, requestParameterMap);
        }

        return destinationUrl;
    }

    @Indexed
    @ToolUi.Placeholder(dynamicText = "${content.namePlaceholder}", editable = true)
    private String name;

    @Required
    @Indexed
    @Note("Only requires path e.g. /mypage")
    private Set<String> localUrls;

    @Required
    @Indexed
    @ToolUi.Placeholder("https://")
    private String destination;

    private Boolean temporary;

    @Required
    protected QueryStringOption queryString = new QueryStringOptionIgnore();

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Set<String> getLocalUrls() {
        if (localUrls == null) {
            localUrls = new HashSet<>();
        }
        return localUrls;
    }

    public Set<String> getNormalizedLocalUrls() {

        return getLocalUrls().stream()
                .filter(u -> !StringUtils.isBlank(u))
                .map(u -> StringUtils.prependIfMissing(u, "/"))
                .collect(Collectors.toSet());
    }

    public void setLocalUrls(Set<String> localUrls) {
        this.localUrls = localUrls;
    }

    public boolean isTemporary() {
        return Boolean.TRUE.equals(temporary);
    }

    public void setTemporary(boolean temporary) {
        this.temporary = Boolean.TRUE.equals(temporary) ? temporary : null;
    }

    public QueryStringOption getQueryString() {
        return queryString;
    }

    public void setQueryString(QueryStringOption queryString) {
        this.queryString = queryString;
    }

    public String getNamePlaceholder() {
        StringBuilder s = new StringBuilder();
        if (!getNormalizedLocalUrls().isEmpty()) {
            s.append(getNormalizedLocalUrls().iterator().next()).append(" -> ");
        }
        if (getDestination() != null) {
            s.append(getDestination());
        }
        return s.toString();
    }

    /**
     * Ensures that the destination is in the form of a valid URL.
     */
    @Override
    protected void onValidate() {
        if (destination != null) {
            try {
                new URL(destination);
            } catch (MalformedURLException e) {
                getState().addError(getState().getField("destination"), ("Invalid URL, ").concat(e.getMessage()));
            }
        }

        if (getState().as(Site.ObjectModification.class).getOwner() == null) {
            getState().addError(null, "An owner Site must be specified.");
        }
    }

    /**
     * Adds valid Site path(s) for the given published {@code locaUrls}, ensuring that each is prefixed with a leading
     * slash character.
     */
    @Override
    protected void beforeSave() {

        // obtain the owner site
        // if the content is new, initialize the owner site to the user's current site using ToolPageContext#getSite
        Site ownerSite = getState().as(Site.ObjectModification.class).getOwner();
        if (getState().isNew() && ownerSite == null) {
            Site currentUserSite = WebRequest.getCurrent()
                    .as(ToolRequest.class)
                    .getCurrentSite();

            if (currentUserSite != null) {
                getState().as(Site.ObjectModification.class).setOwner(currentUserSite);
            }
        }

        // always clear all of the previous URLs
        as(Directory.Data.class).clearPaths();

        // don't create any permalinks if no Site is specified
        // this should also be enforced by onValidate but good to safeguard here as well
        if (ownerSite == null) {
            return;
        }

        Set<String> normalizedLocalUrls = getNormalizedLocalUrls();

        // force normalization of local URLs
        setLocalUrls(normalizedLocalUrls);

        // then re-create the correct set of URLs based on the current state
        for (String localUrl : normalizedLocalUrls) {
            as(Directory.Data.class).addPath(ownerSite, localUrl, Directory.PathType.PERMALINK);
        }

        if (getName() == null || StringUtils.isBlank(getName())) {
            setName(getNamePlaceholder());
        }
    }

    @Override
    public boolean shouldDisplayContentEditWidget(String widgetName) {
        return !HIDDEN_WIDGETS.contains(widgetName);
    }

    @Override
    public String getLabel() {
        return ObjectUtils.firstNonBlank(getName(), getNamePlaceholder());
    }
}