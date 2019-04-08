package uk.gov.hmcts.reform.divorce.petition;

import io.restassured.response.Response;
import org.apache.catalina.User;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.divorce.model.UserDetails;
import uk.gov.hmcts.reform.divorce.support.PetitionSupport;
import uk.gov.hmcts.reform.divorce.util.ResourceLoader;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class CcdRetrieveCaseTest extends PetitionSupport {
    private static final String CASE_DATA_JSON_PATH = "case_data";

    private static final String INVALID_USER_TOKEN = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJqdGkiOiIwOTg3NjU0M"
        + "yIsInN1YiI6IjEwMCIsImlhdCI6MTUwODk0MDU3MywiZXhwIjoxNTE5MzAzNDI3LCJkYXRhIjoiY2l0aXplbiIsInR5cGUiOiJBQ0NFU1MiL"
        + "CJpZCI6IjEwMCIsImZvcmVuYW1lIjoiSm9obiIsInN1cm5hbWUiOiJEb2UiLCJkZWZhdWx0LXNlcnZpY2UiOiJEaXZvcmNlIiwibG9hIjoxL"
        + "CJkZWZhdWx0LXVybCI6Imh0dHBzOi8vd3d3Lmdvdi51ayIsImdyb3VwIjoiZGl2b3JjZSJ9.lkNr1vpAP5_Gu97TQa0cRtHu8I-QESzu8kMX"
        + "CJOQrVU";

    private static final String TEST_AOS_RESPONDED_EVENT = "testAosStarted";
    private static final String AOS_RECEIVED_CONSENT_NO_DEFEND_EVENT = "aosReceivedNoAdConStarted";
    private static final String AMEND_PETITION_EVENT = "amendPetition";

    @Test
    public void givenJWTTokenIsNull_whenRetrieveCase_thenReturnBadRequest() {
        Response cmsResponse = retrieveCase(null);

        assertEquals(HttpStatus.BAD_REQUEST.value(), cmsResponse.getStatusCode());
    }

    @Test
    public void givenInvalidUserToken_whenRetrieveCase_thenReturnForbiddenError() {
        Response cmsResponse = retrieveCase(INVALID_USER_TOKEN);

        assertEquals(HttpStatus.FORBIDDEN.value(), cmsResponse.getStatusCode());
    }

    @Test
    public void givenNoCaseInCcdOrDraftStore_whenRetrieveCase_thenReturnNull() {
        Response cmsResponse = retrieveCase(getUserToken());

        assertEquals(HttpStatus.NO_CONTENT.value(), cmsResponse.getStatusCode());
        assertEquals(cmsResponse.asString(), "");
    }

    @Test
    public void givenOneSubmittedCaseInCcd_whenRetrieveCase_thenReturnTheCase() throws Exception {
        final UserDetails userDetails = getUserDetails();

        Response createCaseResponse = createACaseMakePaymentAndReturnTheCase(userDetails);

        Response cmsResponse = retrieveCase(userDetails.getAuthToken());

        assertEquals(HttpStatus.OK.value(), cmsResponse.getStatusCode());
        assertEquals((Long)createCaseResponse.path("id"), cmsResponse.path("id"));
    }

    @Test
    public void givenMultipleSubmittedCaseInCcd_whenRetrieveCase_thenReturnTheFirstCase() throws Exception {
        final UserDetails userDetails = getUserDetails();

        Response createCaseResponse = createACaseMakePaymentAndReturnTheCase(userDetails);

        createACaseMakePaymentAndReturnTheCase(userDetails);

        Response cmsResponse = retrieveCase(userDetails.getAuthToken());

        assertEquals(HttpStatus.OK.value(), cmsResponse.getStatusCode());
        assertEquals((Long)createCaseResponse.path("id"), cmsResponse.path("id"));
    }

    @Test
    public void givenMultipleSubmittedAndOtherCaseInCcd_whenRetrieveCase_thenReturnFirstSubmittedCase()
        throws Exception {
        final UserDetails userDetails = getUserDetails();

        getCaseIdFromSubmittingANewCase(userDetails);

        Response createCaseResponse = createACaseMakePaymentAndReturnTheCase(userDetails);

        createACaseMakePaymentAndReturnTheCase(userDetails);

        Response cmsResponse = retrieveCase(userDetails.getAuthToken());

        assertEquals(HttpStatus.OK.value(), cmsResponse.getStatusCode());
        assertEquals((Long)createCaseResponse.path("id"), cmsResponse.path("id"));
    }

    @Test
    public void givenOneAwaitingPaymentCaseInCcd_whenRetrieveCase_thenReturnTheCase() throws Exception {
        final UserDetails userDetails = getUserDetails();

        final Long caseId = getCaseIdFromSubmittingANewCase(userDetails);

        Response cmsResponse = retrieveCase(userDetails.getAuthToken());

        assertEquals(HttpStatus.OK.value(), cmsResponse.getStatusCode());
        assertEquals(caseId, cmsResponse.path("id"));
    }

    @Test
    public void givenMultipleAwaitingPaymentAndNoSubmittedCaseInCcd_whenRetrieveCase_thenReturnMultipleChoice()
        throws Exception {
        final UserDetails userDetails = getUserDetails();

        getCaseIdFromSubmittingANewCase(userDetails);
        getCaseIdFromSubmittingANewCase(userDetails);

        Response cmsResponse = retrieveCase(userDetails.getAuthToken());

        assertEquals(HttpStatus.MULTIPLE_CHOICES.value(), cmsResponse.getStatusCode());
    }

    @Test
    public void givenCasesInNotAwaitingPaymentOrNonSubmittedCaseInCcd_whenRetrieveCase_thenReturnNull() {
        final String userToken = getUserToken();

        Response cmsResponse = retrieveCase(userToken);

        assertEquals(HttpStatus.NO_CONTENT.value(), cmsResponse.getStatusCode());
        assertEquals(cmsResponse.asString(), "");
    }

    @Test
    public void givenAmendPetitionCaseAndNoDraft_whenRetrieveCase_thenReturnAmendDraft() throws Exception {
        final UserDetails userDetails = getUserDetails();

        Response submittedCaseResponse = createACaseMakePaymentAndAmendTheCase(userDetails);

        Response cmsResponse = retrieveCase(userDetails.getAuthToken());

        assertEquals("true", cmsResponse.getBody().jsonPath().getString("case_data.fetchedDraft"));
        assertEquals(submittedCaseResponse.getBody().jsonPath().getString("id"),
            cmsResponse.getBody().jsonPath().getString("case_data.previousCaseId"));
    }

    @Test
    public void givenAmendPetitionCaseAndOldDraft_whenRetrieveCase_thenReturnAmendDraft() throws Exception {
        final UserDetails userDetails = getUserDetails();
        final String draftFileName = DIVORCE_FORMAT_DRAFT_CONTEXT_PATH + "existing-draft.json";

        Response submittedCaseResponse = createACaseMakePaymentAndAmendTheCase(userDetails);

        createDraft(userDetails.getAuthToken(), draftFileName,
            Collections.singletonMap(DIVORCE_FORMAT_KEY, true));

        Response cmsResponse = retrieveCase(userDetails.getAuthToken());

        assertEquals("true", cmsResponse.getBody().jsonPath().getString("case_data.fetchedDraft"));
        assertEquals(submittedCaseResponse.getBody().jsonPath().getString("id"),
            cmsResponse.getBody().jsonPath().getString("case_data.previousCaseId"));
        // existing draft defines divorceWho as wife, whilst AmendPetition case has husband.
        assertEquals("husband", cmsResponse.getBody().jsonPath().getString("case_data.divorceWho"));
    }

    @Test
    public void givenAmendPetitionCaseAndAmendedDraft_whenRetrieveCase_thenReturnExisitingDraft() throws Exception {
        final UserDetails userDetails = getUserDetails();
        final String amendDraftFileName = DIVORCE_FORMAT_DRAFT_CONTEXT_PATH + "amend-draft.json";

        createACaseMakePaymentAndAmendTheCase(userDetails);

        createDraft(userDetails.getAuthToken(), amendDraftFileName,
            Collections.singletonMap(DIVORCE_FORMAT_KEY, true));

        Response cmsResponse = retrieveCase(userDetails.getAuthToken());

        assertEquals("true", cmsResponse.getBody().jsonPath().getString("case_data.fetchedDraft"));
        assertEquals("01234567890", cmsResponse.getBody().jsonPath().getString("case_data.previousCaseId"));
    }

    private Response createACaseMakePaymentAndReturnTheCase(UserDetails userDetails) throws Exception {
        Long caseId = getCaseIdFromSubmittingANewCase(userDetails);

        return updateCase("payment-made.json", caseId, EVENT_ID, userDetails.getAuthToken());
    }

    private Response createACaseMakePaymentAndAmendTheCase(UserDetails userDetails) throws Exception {
        Long caseId = getCaseIdFromSubmittingANewCase(userDetails);

        updateCase("basic-update.json", caseId, TEST_AOS_RESPONDED_EVENT, userDetails.getAuthToken());
        updateCase("basic-update.json", caseId, AOS_RECEIVED_CONSENT_NO_DEFEND_EVENT, userDetails.getAuthToken());
        return updateCase("basic-update.json", caseId, AMEND_PETITION_EVENT, userDetails.getAuthToken());
    }
}
