package uk.gov.hmcts.reform.divorce.casemaintenanceservice.functionaltest;

import com.fasterxml.jackson.databind.ObjectMapper;

class ObjectMapperTestUtil {

    static <T> T convertStringToObject(String data, Class type) {
        try {
            return (T)new ObjectMapper().readValue(data, type);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static String convertObjectToJsonString(final Object object) {
        try {
            return new ObjectMapper().writeValueAsString(object);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
