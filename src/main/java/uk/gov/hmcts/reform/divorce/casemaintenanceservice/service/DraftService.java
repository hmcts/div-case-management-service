package uk.gov.hmcts.reform.divorce.casemaintenanceservice.service;

import uk.gov.hmcts.reform.divorce.casemaintenanceservice.draftstore.model.Draft;
import uk.gov.hmcts.reform.divorce.casemaintenanceservice.draftstore.model.DraftList;

import java.util.Map;

public interface DraftService {
    DraftList getAllDrafts(String userToken);

    void saveDraft(String userToken, Map<String, Object> data, boolean divorceFormat);

    Draft getDraft(String userToken);

    void createDraft(String userToken, Map<String, Object> data, boolean divorceFormat);

    void deleteDraft(String authorisation);

    boolean isInCcdFormat(Draft draft);
}
