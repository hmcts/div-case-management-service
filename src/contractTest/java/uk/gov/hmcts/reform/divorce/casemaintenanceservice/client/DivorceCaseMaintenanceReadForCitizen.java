package uk.gov.hmcts.reform.divorce.casemaintenanceservice.client;

import au.com.dius.pact.consumer.dsl.PactDslWithProvider;
import au.com.dius.pact.consumer.junit5.PactTestFor;
import au.com.dius.pact.core.model.RequestResponsePact;
import au.com.dius.pact.core.model.annotations.Pact;
import org.apache.http.client.fluent.Executor;
import org.hamcrest.core.Is;
import org.json.JSONException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.gov.hmcts.reform.ccd.client.CoreCaseDataApi;
import uk.gov.hmcts.reform.ccd.client.model.CaseDataContent;
import uk.gov.hmcts.reform.ccd.client.model.CaseDetails;
import uk.gov.hmcts.reform.ccd.client.model.Event;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.client.util.DivorceCaseMaintenancePact;

import java.io.IOException;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;
import static uk.gov.hmcts.reform.divorce.casemaintenanceservice.client.util.PactDslBuilderForCaseDetailsList.buildCaseDetailsDsl;


public class DivorceCaseMaintenanceReadForCitizen extends DivorceCaseMaintenancePact {

    private Map<String, Object> caseDetailsMap;
    private CaseDataContent caseDataContent;

    @BeforeAll
    public void setUp() throws Exception {
        caseDetailsMap = getCaseDetailsAsMap("divorce-map.json");
        caseDataContent = CaseDataContent.builder()
            .eventToken("someEventToken")
            .event(
                Event.builder()
                    .id(createEventId)
                    .summary(DIVORCE_CASE_SUBMISSION_EVENT_SUMMARY)
                    .description(DIVORCE_CASE_SUBMISSION_EVENT_DESCRIPTION)
                    .build()
            ).data(caseDetailsMap.get("case_data"))
            .build();
    }

    @Pact(provider = "ccdDataStoreAPI_CaseController", consumer = "divorce_caseMaintenanceService")
    RequestResponsePact readForCitizen(PactDslWithProvider builder) {
        // @formatter:off
        return builder
            .given("A Read For Citizen is  requested", getCaseDataContentAsMap(caseDataContent))
            .uponReceiving("A Read For Citizen is requested")
            .path("/citizens/"
                + USER_ID
                + "/jurisdictions/"
                + jurisdictionId
                + "/case-types/"
                + caseType
                + "/cases/"
                +  CASE_ID)
            .method("GET")
            .headers(HttpHeaders.AUTHORIZATION, SOME_AUTHORIZATION_TOKEN, SERVICE_AUTHORIZATION,
                SOME_SERVICE_AUTHORIZATION_TOKEN)
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .willRespondWith()
            .matchHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .status(200)
            .body(buildCaseDetailsDsl(CASE_ID, "emailAddress@email.com",false, false))
            .toPact();
    }

    @Test
    @PactTestFor(pactMethod = "readForCitizen")
    public void verifyReadForCitizen() throws IOException, JSONException {

        CaseDetails caseDetailsReponse = coreCaseDataApi.readForCitizen(SOME_AUTHORIZATION_TOKEN,
            SOME_SERVICE_AUTHORIZATION_TOKEN, USER_ID, jurisdictionId,
            caseType,String.valueOf(CASE_ID));

        assertThat(caseDetailsReponse.getId(), Is.is(CASE_ID));
        assertThat(caseDetailsReponse.getJurisdiction(), Is.is("DIVORCE"));

        assertThat(caseDetailsReponse.getData().get("applicationType"), equalTo("Personal"));
        assertThat(caseDetailsReponse.getData().get("primaryApplicantForenames"), equalTo("Jon"));
        assertThat(caseDetailsReponse.getData().get("primaryApplicantSurname"), equalTo("Snow"));
    }
}
