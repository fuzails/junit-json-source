package com.onelinefix.junit.jupiter.provider;

import java.lang.reflect.Type;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.support.AnnotationConsumer;

public class JsonArgumentsProvider implements ArgumentsProvider, AnnotationConsumer<JsonSource> {

    private String[] value;
    private SimpleDateFormat df;

    public JsonArgumentsProvider() {
    }

    @Override
    public void accept(final JsonSource annotation) {
        value = annotation.value();
        String format = annotation.dateFormat();
        String timeZone = annotation.timeZone();
        if(!format.trim().equals("")) {
            df = new SimpleDateFormat(format);
            if(!timeZone.trim().equals("")) {
                df.setTimeZone(TimeZone.getTimeZone(timeZone));
            }
        } else {
            df = new SimpleDateFormat();
        }

    }

    @Override
    public Stream<? extends Arguments> provideArguments(final ExtensionContext context) throws Exception {
        return Arrays.asList(value).stream().map(
                e -> {
                    List<Object> arguments = new ArrayList<>();
                    try {
                        if (e.startsWith("[")) {
                            JSONArray array = new JSONArray(e);
                            for (int i = 0; i < array.length(); i++) {
                                JSONObject object = array.getJSONObject(i);
                                mapJsonObjectToActualType(arguments, object);
                            }

                        } else {
                            JSONObject object = new JSONObject(e);
                            mapJsonObjectToActualType(arguments, object);
                        }
                    } catch (JSONException | ClassNotFoundException e1) {
                        throw new IllegalArgumentException(e1);
                    }
                    return Arguments.of(arguments.toArray());
                }
        );
    }

    private void mapJsonObjectToActualType(List<Object> arguments, JSONObject object) throws ClassNotFoundException {
        final String value = object.getString("value");
        final String type = object.getString("type");
        Class clazz = Class.forName(type);
        arguments.add(map(value, clazz));
    }

    @SuppressWarnings("unchecked")
    private <R> R map(final String value, Type type) {
        if (value.equals("null")) {
            return null;
        }
        if (((Class) type).isAssignableFrom(Date.class)) {
            try {
                return (R) df.parse(value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } else if (((Class) type).isAssignableFrom(String.class)) {
            return (R) value;
        } else {
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setTimeZone(TimeZone.getTimeZone("GMT+2:00"));
            objectMapper.configure(JsonParser.Feature.ALLOW_SINGLE_QUOTES, true);
            objectMapper.setDateFormat(df);
            try {
                if(value.startsWith("[")) {
                    return (R) objectMapper.readValue(value, objectMapper.getTypeFactory().constructCollectionType(List.class, (Class) type));
                }
                return (R) objectMapper.readValue(value, (Class) type);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
