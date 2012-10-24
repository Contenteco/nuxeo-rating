/*
 * (C) Copyright 2006-2012 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.rating;

import static org.nuxeo.ecm.activity.ActivityHelper.createDocumentActivityObject;
import static org.nuxeo.ecm.activity.ActivityHelper.createUserActivityObject;
import static org.nuxeo.ecm.core.schema.FacetNames.SUPER_SPACE;
import static org.nuxeo.ecm.rating.LikesCountActivityStreamFilter.ACTOR_PARAMETER;
import static org.nuxeo.ecm.rating.LikesCountActivityStreamFilter.CONTEXT_PARAMETER;
import static org.nuxeo.ecm.rating.LikesCountActivityStreamFilter.FROMDT_PARAMETER;
import static org.nuxeo.ecm.rating.LikesCountActivityStreamFilter.OBJECT_PARAMETER;
import static org.nuxeo.ecm.rating.LikesCountActivityStreamFilter.QueryType.GET_DOCUMENTS_COUNT;
import static org.nuxeo.ecm.rating.LikesCountActivityStreamFilter.QueryType.GET_MINI_MESSAGE_COUNT;
import static org.nuxeo.ecm.rating.LikesCountActivityStreamFilter.TODT_PARAMETER;
import static org.nuxeo.ecm.rating.RatingActivityStreamFilter.QUERY_TYPE_PARAMETER;
import static org.nuxeo.ecm.rating.api.Constants.LIKE_ASPECT;
import static org.nuxeo.ecm.rating.api.LikeStatus.DISLIKED;
import static org.nuxeo.ecm.rating.api.LikeStatus.LIKED;
import static org.nuxeo.ecm.rating.api.LikeStatus.UNKNOWN;

import java.io.Serializable;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.activity.ActivitiesList;
import org.nuxeo.ecm.activity.ActivitiesListImpl;
import org.nuxeo.ecm.activity.Activity;
import org.nuxeo.ecm.activity.ActivityBuilder;
import org.nuxeo.ecm.activity.ActivityHelper;
import org.nuxeo.ecm.activity.ActivityStreamService;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.rating.api.LikeService;
import org.nuxeo.ecm.rating.api.LikeStatus;
import org.nuxeo.ecm.rating.api.RatingService;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * Default implementation of {@see LikeService}.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
public class LikeServiceImpl extends DefaultComponent implements LikeService {

    private static final Log log = LogFactory.getLog(LikeServiceImpl.class);

    public static final String LIKE_VERB = "like";

    public static final String DISLIKE_VERB = "dislike";

    public static final int LIKE_RATING = 1;

    public static final int DISLIKE_RATING = -1;

    @Override
    public void like(String username, String activityObject) {
        RatingService ratingService = Framework.getLocalService(RatingService.class);
        ratingService.cancelRate(username, activityObject, LIKE_ASPECT);
        ratingService.rate(username, LIKE_RATING, activityObject, LIKE_ASPECT);
    }

    @Override
    public void like(String username, DocumentModel doc) {
        like(username, createDocumentActivityObject(doc));
        // add like activities for the doc
        addLikeActivities(username, doc);
    }

    @Override
    public boolean hasUserLiked(String username, String activityObject) {
        RatingService ratingService = Framework.getLocalService(RatingService.class);
        double average = ratingService.getAverageRatingForUser(username,
                activityObject, LIKE_ASPECT);
        return average > 0;
    }

    @Override
    public boolean hasUserLiked(String username, DocumentModel doc) {
        return hasUserLiked(username, createDocumentActivityObject(doc));
    }

    @Override
    public long getLikesCount(String activityObject) {
        RatingService ratingService = Framework.getLocalService(RatingService.class);
        return ratingService.getRatesCount(activityObject, LIKE_RATING,
                LIKE_ASPECT);
    }

    @Override
    public long getLikesCount(DocumentModel doc) {
        return getLikesCount(createDocumentActivityObject(doc));
    }

    @Override
    public void dislike(String username, String activityObject) {
        RatingService ratingService = Framework.getLocalService(RatingService.class);
        ratingService.cancelRate(username, activityObject, LIKE_ASPECT);
        ratingService.rate(username, DISLIKE_RATING, activityObject,
                LIKE_ASPECT);
    }

    @Override
    public void dislike(String username, DocumentModel doc) {
        dislike(username, createDocumentActivityObject(doc));
        // add dislike activities for the doc
        addDislikeActivities(username, doc);
    }

    @Override
    public boolean hasUserDisliked(String username, String activityObject) {
        RatingService ratingService = Framework.getLocalService(RatingService.class);
        double average = ratingService.getAverageRatingForUser(username,
                activityObject, LIKE_ASPECT);
        return average < 0;
    }

    @Override
    public boolean hasUserDisliked(String username, DocumentModel doc) {
        return hasUserDisliked(username, createDocumentActivityObject(doc));
    }

    @Override
    public long getDislikesCount(String activityObject) {
        RatingService ratingService = Framework.getLocalService(RatingService.class);
        return ratingService.getRatesCount(activityObject, DISLIKE_RATING,
                LIKE_ASPECT);
    }

    @Override
    public long getDislikesCount(DocumentModel doc) {
        return getDislikesCount(createDocumentActivityObject(doc));
    }

    @Override
    public void cancel(String username, String activityObject) {
        RatingService ratingService = Framework.getLocalService(RatingService.class);
        ratingService.cancelRate(username, activityObject, LIKE_ASPECT);
    }

    @Override
    public void cancel(String username, DocumentModel doc) {
        cancel(username, createDocumentActivityObject(doc));
    }

    @Override
    public LikeStatus getLikeStatus(String activityObject) {
        long likesCount = getLikesCount(activityObject);
        long dislikesCount = getDislikesCount(activityObject);
        return new LikeStatus(activityObject, likesCount, dislikesCount);
    }

    @Override
    public LikeStatus getLikeStatus(DocumentModel doc) {
        return getLikeStatus(createDocumentActivityObject(doc));
    }

    @Override
    public LikeStatus getLikeStatus(String username, String activityObject) {
        long likesCount = getLikesCount(activityObject);
        long dislikesCount = getDislikesCount(activityObject);
        int userLikeStatus = hasUserLiked(username, activityObject) ? LIKED
                : hasUserDisliked(username, activityObject) ? DISLIKED
                        : UNKNOWN;
        return new LikeStatus(activityObject, likesCount, dislikesCount,
                username, userLikeStatus);
    }

    @Override
    public LikeStatus getLikeStatus(String username, DocumentModel doc) {
        return getLikeStatus(username, createDocumentActivityObject(doc));
    }

    @Override
    public ActivitiesList getMostLikedActivities(CoreSession session,
            int limit, DocumentModel source, Date fromDt, Date toDt) {
        // Get most liked Documents
        Map<String, Serializable> parameters = new HashMap<String, Serializable>();
        if (fromDt != null && toDt != null) {
            parameters.put(FROMDT_PARAMETER, fromDt);
            parameters.put(TODT_PARAMETER, toDt);
        }
        parameters.put(CONTEXT_PARAMETER, createDocumentActivityObject(source));
        parameters.put(OBJECT_PARAMETER, Integer.valueOf(LIKE_RATING));
        parameters.put(ACTOR_PARAMETER,
                createUserActivityObject(session.getPrincipal().getName()));
        parameters.put(QUERY_TYPE_PARAMETER, GET_DOCUMENTS_COUNT);

        ActivityStreamService activityStreamService = Framework.getLocalService(ActivityStreamService.class);
        ActivitiesList documentActivitiesList = activityStreamService.query(
                LikesCountActivityStreamFilter.ID, parameters);
        ActivitiesList mostLikedActivities = documentActivitiesList.filterActivities(session);

        // Get most liked minimessages
        parameters.put(QUERY_TYPE_PARAMETER, GET_MINI_MESSAGE_COUNT);
        ActivitiesList miniMessageActivitiesList = activityStreamService.query(
                LikesCountActivityStreamFilter.ID, parameters, 0, limit);
        mostLikedActivities.addAll(miniMessageActivitiesList);

        // Sort by Object
        Collections.sort(mostLikedActivities, new Comparator<Activity>() {
            @Override
            public int compare(Activity o1, Activity o2) {
                return o2.getObject().compareTo(o1.getObject());
            }
        });

        if (mostLikedActivities.size() > limit) {
            return new ActivitiesListImpl(mostLikedActivities.subList(0, limit));
        }
        return mostLikedActivities;
    }

    @Override
    public ActivitiesList getMostLikedActivities(CoreSession session,
            int limit, DocumentModel source) {
        return getMostLikedActivities(session, limit, source, null, null);
    }

    private void addLikeActivities(String username, DocumentModel doc) {
        addActivities(LIKE_VERB, username, doc);
    }

    private void addDislikeActivities(String username, DocumentModel doc) {
        addActivities(DISLIKE_VERB, username, doc);
    }

    private void addActivities(String verb, String username, DocumentModel doc) {
        try {
            Principal principal = Framework.getLocalService(UserManager.class).getPrincipal(
                    username);
            if (principal == null) {
                return;
            }

            ActivityStreamService activityStreamService = Framework.getLocalService(ActivityStreamService.class);
            Activity activity = toActivity(verb, principal, doc);
            activityStreamService.addActivity(activity);

            for (DocumentRef docRef : getParentSuperSpaceRefs(doc)) {
                String context = ActivityHelper.createDocumentActivityObject(
                        doc.getRepositoryName(), docRef.toString());
                activityStreamService.addActivity(new ActivityBuilder(activity).context(
                        context).build());
            }
        } catch (ClientException e) {
            log.error(String.format("Error while adding like activities: %s",
                    e.getMessage()));
            if (log.isDebugEnabled()) {
                log.debug(e, e);
            }
        }
    }

    private Activity toActivity(String verb, Principal principal,
            DocumentModel doc) {
        return new ActivityBuilder().actor(
                ActivityHelper.createUserActivityObject(principal)).displayActor(
                ActivityHelper.generateDisplayName(principal)).verb(verb).object(
                ActivityHelper.createDocumentActivityObject(doc)).displayObject(
                ActivityHelper.getDocumentTitle(doc)).build();
    }

    private List<DocumentRef> getParentSuperSpaceRefs(final DocumentModel doc)
            throws ClientException {
        final List<DocumentRef> parents = new ArrayList<DocumentRef>();
        new UnrestrictedSessionRunner(doc.getRepositoryName()) {
            @Override
            public void run() throws ClientException {
                List<DocumentModel> parentDocuments = session.getParentDocuments(doc.getRef());
                for (DocumentModel parent : parentDocuments) {
                    if (parent.hasFacet(SUPER_SPACE)) {
                        parents.add(parent.getRef());
                    }
                }
            }
        }.runUnrestricted();
        return parents;
    }

}
