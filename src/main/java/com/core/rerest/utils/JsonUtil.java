package com.core.rerest.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

public final class JsonUtil {

    private static final ObjectMapper mapper = new ObjectMapper();

    private JsonUtil() {

    }

    public static String convertToJsonString(final Object object) throws JsonProcessingException {
        return mapper.writeValueAsString(object);
    }

}
