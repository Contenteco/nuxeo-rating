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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.ecm.activity.ActivitiesList;
import org.nuxeo.ecm.activity.ActivityHelper;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.6
 */
@RunWith(FeaturesRunner.class)
@Features({ RatingFeature.class })
@SuppressWarnings("boxing")
public class TestRatingService extends AbstractRatingTest {

    public static final String STARS_ASPECT = "stars";

    public static final String OTHER_ASPECT = "other";

    @Inject
    protected EventService eventService;

    @Test
    public void serviceRegistration() throws IOException {
        assertNotNull(ratingService);
    }

    @Test
    public void differentUsersCanRateADocument() throws ClientException {
        DocumentModel doc = createTestDocument("file1");
        String docActivityObject = ActivityHelper.createDocumentActivityObject(doc);

        ratingService.rate("bender", 5, docActivityObject, STARS_ASPECT);
        assertTrue(ratingService.hasUserRated("bender", docActivityObject, STARS_ASPECT));

        ratingService.rate("fry", 2, docActivityObject, STARS_ASPECT);
        ratingService.rate("leela", 1, docActivityObject, STARS_ASPECT);
        assertTrue(ratingService.hasUserRated("fry", docActivityObject, STARS_ASPECT));
        assertTrue(ratingService.hasUserRated("leela", docActivityObject, STARS_ASPECT));
    }

    @Test
    public void differentUsersCanRateAnActivity() throws ClientException {
        String activityObject = ActivityHelper.createActivityObject(15L);

        ratingService.rate("leela", 5, activityObject, STARS_ASPECT);
        assertTrue(ratingService.hasUserRated("leela", activityObject, STARS_ASPECT));

        ratingService.rate("bender", 2, activityObject, STARS_ASPECT);
        ratingService.rate("fry", 1, activityObject, STARS_ASPECT);
        assertTrue(ratingService.hasUserRated("bender", activityObject, STARS_ASPECT));
        assertTrue(ratingService.hasUserRated("fry", activityObject, STARS_ASPECT));
    }

    @Test
    public void shouldRetrieveRatesCountForAnActivityObject() throws ClientException {
        String activityObject = ActivityHelper.createActivityObject(15L);

        ratingService.rate("bender", 5, activityObject, STARS_ASPECT);
        ratingService.rate("fry", 2, activityObject, STARS_ASPECT);
        ratingService.rate("leela", 3, activityObject, STARS_ASPECT);
        assertTrue(ratingService.hasUserRated("bender", activityObject, STARS_ASPECT));
        assertTrue(ratingService.hasUserRated("fry", activityObject, STARS_ASPECT));
        assertTrue(ratingService.hasUserRated("leela", activityObject, STARS_ASPECT));

        long ratesCount = ratingService.getRatesCount(activityObject, STARS_ASPECT);
        assertEquals(3, ratesCount);
    }

    @Test
    public void shouldRetrieveRatesCountOfAGivenRatingForAnActivityObject() throws ClientException {
        String activityObject = ActivityHelper.createActivityObject(15L);

        ratingService.rate("bender", 5, activityObject, STARS_ASPECT);
        ratingService.rate("fry", 2, activityObject, STARS_ASPECT);
        ratingService.rate("leela", 5, activityObject, STARS_ASPECT);
        assertTrue(ratingService.hasUserRated("bender", activityObject, STARS_ASPECT));
        assertTrue(ratingService.hasUserRated("fry", activityObject, STARS_ASPECT));
        assertTrue(ratingService.hasUserRated("leela", activityObject, STARS_ASPECT));

        long ratesCount = ratingService.getRatesCount(activityObject, 5, STARS_ASPECT);
        assertEquals(2, ratesCount);
    }

    @Test
    public void shouldRetrieveUserRatesCountForAnActivityObject() throws ClientException {
        String activityObject = ActivityHelper.createActivityObject(15L);

        ratingService.rate("bender", 3, activityObject, STARS_ASPECT);
        ratingService.rate("bender", 5, activityObject, STARS_ASPECT);
        ratingService.rate("bender", 3, activityObject, STARS_ASPECT);
        ratingService.rate("bender", 4, activityObject, STARS_ASPECT);
        ratingService.rate("bender", 3, activityObject, STARS_ASPECT);
        ratingService.rate("fry", 2, activityObject, STARS_ASPECT);
        ratingService.rate("leela", 3, activityObject, STARS_ASPECT);
        ratingService.rate("leela", 5, activityObject, STARS_ASPECT);
        ratingService.rate("leela", 5, activityObject, STARS_ASPECT);

        long ratesCount = ratingService.getRatesCountForUser("bender", activityObject, 3, STARS_ASPECT);
        assertEquals(3, ratesCount);
        ratesCount = ratingService.getRatesCountForUser("leela", activityObject, 5, STARS_ASPECT);
        assertEquals(2, ratesCount);
        ratesCount = ratingService.getRatesCountForUser("fry", activityObject, 5, STARS_ASPECT);
        assertEquals(0, ratesCount);
    }

    @Test
    public void shouldRetrieveUserRatesCountOfAGivenRatingForAnActivityObject() throws ClientException {
        String activityObject = ActivityHelper.createActivityObject(15L);

        ratingService.rate("bender", 5, activityObject, STARS_ASPECT);
        ratingService.rate("bender", 2, activityObject, STARS_ASPECT);
        ratingService.rate("bender", 3, activityObject, STARS_ASPECT);
        ratingService.rate("fry", 2, activityObject, STARS_ASPECT);
        ratingService.rate("leela", 3, activityObject, STARS_ASPECT);
        ratingService.rate("leela", 5, activityObject, STARS_ASPECT);

        long ratesCount = ratingService.getRatesCountForUser("bender", activityObject, STARS_ASPECT);
        assertEquals(3, ratesCount);
        ratesCount = ratingService.getRatesCountForUser("fry", activityObject, STARS_ASPECT);
        assertEquals(1, ratesCount);
        ratesCount = ratingService.getRatesCountForUser("leela", activityObject, STARS_ASPECT);
        assertEquals(2, ratesCount);
    }

    @Test
    public void shouldRetrieveAverageRatingForAnActivityObject() throws ClientException {
        String activityObject = ActivityHelper.createActivityObject(15L);

        ratingService.rate("bender", 5, activityObject, STARS_ASPECT);
        ratingService.rate("fry", 2, activityObject, STARS_ASPECT);
        ratingService.rate("leela", 3, activityObject, STARS_ASPECT);
        assertTrue(ratingService.hasUserRated("bender", activityObject, STARS_ASPECT));
        assertTrue(ratingService.hasUserRated("fry", activityObject, STARS_ASPECT));
        assertTrue(ratingService.hasUserRated("leela", activityObject, STARS_ASPECT));

        double average = ratingService.getAverageRating(activityObject, STARS_ASPECT);
        assertEquals(3.3, average, 0.1);
    }

    @Test
    public void shouldRetrieveUserAverageRatingForAnActivityObject() throws ClientException {
        String activityObject = ActivityHelper.createActivityObject(15L);

        ratingService.rate("bender", 5, activityObject, STARS_ASPECT);
        ratingService.rate("bender", 1, activityObject, STARS_ASPECT);
        ratingService.rate("bender", 4, activityObject, STARS_ASPECT);
        ratingService.rate("fry", 2, activityObject, STARS_ASPECT);
        ratingService.rate("leela", 3, activityObject, STARS_ASPECT);
        ratingService.rate("leela", 5, activityObject, STARS_ASPECT);

        double average = ratingService.getAverageRatingForUser("bender", activityObject, STARS_ASPECT);
        assertEquals(3.3, average, 0.1);
        average = ratingService.getAverageRatingForUser("leela", activityObject, STARS_ASPECT);
        assertEquals(4, average, 0.1);
    }

    @Test
    public void shouldHandleRatingOnDifferentAspects() {
        String activityObject = ActivityHelper.createActivityObject(15L);

        ratingService.rate("bender", 5, activityObject, STARS_ASPECT);
        ratingService.rate("bender", 2, activityObject, OTHER_ASPECT);
        ratingService.rate("fry", 2, activityObject, STARS_ASPECT);
        ratingService.rate("fry", 1, activityObject, OTHER_ASPECT);
        ratingService.rate("leela", 2, activityObject, STARS_ASPECT);
        ratingService.rate("leela", 3, activityObject, OTHER_ASPECT);

        long ratesCount = ratingService.getRatesCount(activityObject, STARS_ASPECT);
        assertEquals(3, ratesCount);

        ratesCount = ratingService.getRatesCount(activityObject, OTHER_ASPECT);
        assertEquals(3, ratesCount);

        double average = ratingService.getAverageRating(activityObject, STARS_ASPECT);
        assertEquals(3, average, 0.1);
        average = ratingService.getAverageRating(activityObject, OTHER_ASPECT);
        assertEquals(2, average, 0.1);
    }

    @Test
    public void shouldCancelExistingRating() {
        String activityObject = ActivityHelper.createActivityObject(15L);

        ratingService.rate("bender", 5, activityObject, STARS_ASPECT);
        ratingService.rate("fry", 2, activityObject, STARS_ASPECT);
        assertTrue(ratingService.hasUserRated("bender", activityObject, STARS_ASPECT));
        assertTrue(ratingService.hasUserRated("fry", activityObject, STARS_ASPECT));

        ratingService.cancelRate("bender", activityObject, STARS_ASPECT);
        assertFalse(ratingService.hasUserRated("bender", activityObject, STARS_ASPECT));
        assertTrue(ratingService.hasUserRated("fry", activityObject, STARS_ASPECT));

        ratingService.cancelRate("fry", activityObject, STARS_ASPECT);
        assertFalse(ratingService.hasUserRated("bender", activityObject, STARS_ASPECT));
        assertFalse(ratingService.hasUserRated("fry", activityObject, STARS_ASPECT));
    }

    @Test
    public void shouldNotFindRatingWhenDocIsRemoved() throws ClientException {
        DocumentModel doc = createTestDocument("doc1");
        String docActivity = ActivityHelper.createDocumentActivityObject(doc);

        ratingService.rate("Leah", 5, docActivity, STARS_ASPECT);
        assertTrue(ratingService.hasUserRated("Leah", docActivity, STARS_ASPECT));

        DocumentRef docRef = doc.getRef();
        session.removeDocument(docRef);
        session.save();

        assertEquals(0, ratingService.getRatesCount(docActivity, STARS_ASPECT));
    }

    @Test
    public void shouldNotFindRatingWhenDocIsDeleted() throws ClientException {

        DocumentModel doc = createTestDocument("doc1");
        String docActivity = ActivityHelper.createDocumentActivityObject(doc);

        ratingService.rate("Leah", 5, docActivity, STARS_ASPECT);
        assertEquals(1, ratingService.getRatesCount(docActivity, STARS_ASPECT));

        DocumentRef docRef = doc.getRef();
        session.followTransition(docRef, "delete");
        session.save();

        assertEquals(0, ratingService.getRatesCount(docActivity, STARS_ASPECT));
    }

    @Test
    public void shouldGetLatestRateForUser() throws ClientException {
        String activity1 = ActivityHelper.createDocumentActivityObject(createTestDocument("doc1"));
        String activity2 = ActivityHelper.createDocumentActivityObject(createTestDocument("doc2"));
        String activity3 = ActivityHelper.createDocumentActivityObject(createTestDocument("doc3"));
        String activity4 = ActivityHelper.createDocumentActivityObject(createTestDocument("doc4"));

        String username = "Lise";

        ratingService.rate(username, 5, activity1, STARS_ASPECT);
        ratingService.rate(username, 2, activity2, STARS_ASPECT);
        ratingService.rate(username, 3, activity3, STARS_ASPECT);
        ratingService.rate(username, 5, activity4, STARS_ASPECT);

        ActivitiesList lastestRatedDocByUser = ratingService.getLastestRatedDocByUser(username, STARS_ASPECT, 3);
        assertEquals(3, lastestRatedDocByUser.size());
        assertEquals(activity4, lastestRatedDocByUser.get(0).getTarget());
        assertEquals(activity3, lastestRatedDocByUser.get(1).getTarget());
        assertEquals(activity2, lastestRatedDocByUser.get(2).getTarget());
    }
}
