package uk.gov.hmcts.reform.divorce.ccd;

import com.google.common.collect.ImmutableMap;
import io.restassured.response.Response;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import uk.gov.hmcts.reform.divorce.model.PinResponse;
import uk.gov.hmcts.reform.divorce.model.UserDetails;
import uk.gov.hmcts.reform.divorce.support.PetitionSupport;
import uk.gov.hmcts.reform.divorce.support.client.CcdClientSupport;
import uk.gov.hmcts.reform.divorce.util.ResourceLoader;
import uk.gov.hmcts.reform.divorce.util.RestUtil;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.domain.model.CcdCaseProperties.CO_RESP_LETTER_HOLDER_ID_FIELD;

public class LinkCoRespondentTest extends PetitionSupport {
    private static final String PAYLOAD_CONTEXT_PATH = "ccd-submission-payload/";
    private static final String CO_RESPONDENT_EMAIL_ADDRESS = "CoRespEmailAddress";
    private static final String START_AOS_EVENT_ID = "startAos";
    private static final String TEST_AOS_AWAITING_EVENT_ID = "testAosAwaiting";

    private static final String INVALID_USER_TOKEN = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJqdGkiOiIwOTg3NjU0M"
        + "yIsInN1YiI6IjEwMCIsImlhdCI6MTUwODk0MDU3MywiZXhwIjoxNTE5MzAzNDI3LCJkYXRhIjoiY2l0aXplbiIsInR5cGUiOiJBQ0NFU1MiL"
        + "CJpZCI6IjEwMCIsImZvcmVuYW1lIjoiSm9obiIsInN1cm5hbWUiOiJEb2UiLCJkZWZhdWx0LXNlcnZpY2UiOiJEaXZvcmNlIiwibG9hIjoxL"
        + "CJkZWZhdWx0LXVybCI6Imh0dHBzOi8vd3d3Lmdvdi51ayIsImdyb3VwIjoiZGl2b3JjZSJ9.lkNr1vpAP5_Gu97TQa0cRtHu8I-QESzu8kMX"
        + "CJOQrVU";

    @Value("${case.maintenance.link-co-respondent.context-path}")
    private String linkCoRespondentContextPath;

    @Value("${case.maintenance.aos-case.context-path}")
    private String retrieveAosCaseContextPath;

    @Autowired
    private CcdClientSupport ccdClientSupport;

    @Test
    public void givenJWTTokenIsNull_whenLinkCoRespondent_thenReturnBadRequest() {
        Response cmsResponse = linkCoRespondent(null, "someCaseId", "someLetterHolderId");

        assertEquals(HttpStatus.BAD_REQUEST.value(), cmsResponse.getStatusCode());
    }

    @Test
    public void givenNoCase_whenLinkCoRespondent_thenReturnBadRequest() {
        Response cmsResponse = linkCoRespondent(getUserToken(), "someCaseId", "someLetterHolderId");

        assertEquals(HttpStatus.BAD_REQUEST.value(), cmsResponse.getStatusCode());
    }

    @Test
    public void givenNoLetterHolderId_whenLinkCoRespondent_thenReturnNotFound() {
        Map caseData = ResourceLoader.loadJsonToObject(PAYLOAD_CONTEXT_PATH + "addresses.json", Map.class);

        Long caseId = ccdClientSupport.submitCase(caseData, getCaseWorkerUser()).getId();

        Response cmsResponse = linkCoRespondent(getUserToken(), caseId.toString(), "someLetterHolderId");

        assertEquals(HttpStatus.NOT_FOUND.value(), cmsResponse.getStatusCode());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void givenLetterHolderDoNotMatch_whenLinkCoRespondent_thenReturnNotFound() {
        Map caseData = ResourceLoader.loadJsonToObject(PAYLOAD_CONTEXT_PATH + "addresses.json", Map.class);
        caseData.put(CO_RESP_LETTER_HOLDER_ID_FIELD, "nonMatchingLetterHolderId");

        Long caseId = ccdClientSupport.submitCase(caseData, getCaseWorkerUser()).getId();

        Response cmsResponse = linkCoRespondent(getUserToken(), caseId.toString(), "someLetterHolderId");

        assertEquals(HttpStatus.NOT_FOUND.value(), cmsResponse.getStatusCode());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void givenCaseAlreadyLinked_whenLinkCoRespondent_thenReturnNotFound() {
        final String respondentFirstName = "respondent-" + UUID.randomUUID().toString();

        final PinResponse pinResponse = idamTestSupport.createPinUser(respondentFirstName);

        Map caseData = ResourceLoader.loadJsonToObject(PAYLOAD_CONTEXT_PATH + "linked-case.json", Map.class);
        caseData.put(CO_RESP_LETTER_HOLDER_ID_FIELD, pinResponse.getUserId());

        Long caseId = ccdClientSupport.submitCase(caseData, getCaseWorkerUser()).getId();

        Response cmsResponse = linkCoRespondent(getUserToken(), caseId.toString(), pinResponse.getUserId());

        assertEquals(HttpStatus.NOT_FOUND.value(), cmsResponse.getStatusCode());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void givenInvalidUserToken_whenLinkCoRespondent_thenReturnForbidden() throws Exception {
        final String respondentFirstName = "respondent-" + UUID.randomUUID().toString();

        final PinResponse pinResponse = idamTestSupport.createPinUser(respondentFirstName);

        Map caseData = ResourceLoader.loadJsonToObject(PAYLOAD_CONTEXT_PATH + "addresses.json", Map.class);
        caseData.put(CO_RESP_LETTER_HOLDER_ID_FIELD, pinResponse.getUserId());

        Long caseId = ccdClientSupport.submitCase(caseData, getCaseWorkerUser()).getId();

        updateCase((String)null, caseId, TEST_AOS_AWAITING_EVENT_ID, getCaseWorkerUser().getAuthToken());

        Response cmsResponse = linkCoRespondent(INVALID_USER_TOKEN, caseId.toString(), pinResponse.getUserId());

        assertEquals(HttpStatus.FORBIDDEN.value(), cmsResponse.getStatusCode());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void givenLetterHolderIdAndCaseStateMatches_whenLinkCoRespondent_thenShouldBeAbleToAccessTheCase()
        throws Exception {

        final String respondentFirstName = "respondent-" + UUID.randomUUID().toString();

        final PinResponse pinResponse = idamTestSupport.createPinUser(respondentFirstName);

        UserDetails upliftedUser = idamTestSupport.createRespondentUser(respondentFirstName, pinResponse.getPin());

        Map caseData = ResourceLoader.loadJsonToObject(PAYLOAD_CONTEXT_PATH + "addresses.json", Map.class);
        caseData.put(CO_RESP_LETTER_HOLDER_ID_FIELD, pinResponse.getUserId());

        Long caseId = ccdClientSupport.submitCase(caseData, getCaseWorkerUser()).getId();

        updateCase((String)null, caseId, TEST_AOS_AWAITING_EVENT_ID, getCaseWorkerUser().getAuthToken());

        linkCoRespondent(upliftedUser.getAuthToken(), caseId.toString(), pinResponse.getUserId());

        updateCase(ImmutableMap.of(CO_RESPONDENT_EMAIL_ADDRESS, upliftedUser.getEmailAddress()),
            caseId, START_AOS_EVENT_ID, getCaseWorkerUser().getAuthToken());

        Response response = retrieveCase(upliftedUser.getAuthToken(), true);

        assertEquals(caseId, response.path("id"));
    }

    private Response linkCoRespondent(String authToken, String caseId, String letterHolderId) {
        return RestUtil.postToRestService(
            serverUrl + linkCoRespondentContextPath + "/" + caseId + "/" + letterHolderId,
            Collections.singletonMap(HttpHeaders.AUTHORIZATION, authToken),
            null);
    }

    @Override
    protected String getRetrieveCaseRequestUrl() {
        return serverUrl + retrieveAosCaseContextPath;
    }
}
