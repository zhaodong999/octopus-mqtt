package org.octopus.admin.api.provider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;

public class GsonMessageBodyHandler implements MessageBodyWriter<Object>,
        MessageBodyReader<Object> {

    private Gson gson;
 
    //Customize the gson behavior here
    private Gson getGson() {
        if (gson == null) {
            final GsonBuilder gsonBuilder = new GsonBuilder();
            gson = gsonBuilder
//                    .setFieldNamingPolicy(FieldNamingPolicy.IDENTITY)
                    .disableHtmlEscaping()
                    .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .create();
        }
        return gson;
    }
 
    @Override
    public boolean isReadable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return true;
    }
 
    @Override
    public Object readFrom(Class<Object> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders, InputStream entityStream) {
        InputStreamReader streamReader = null;
        streamReader = new InputStreamReader(entityStream, StandardCharsets.UTF_8);
        try {
            Type jsonType;
            if (type.equals(genericType)) {
                jsonType = type;
            } else {
                jsonType = genericType;
            }
            return getGson().fromJson(streamReader, jsonType);
        } finally {
            try {
                streamReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
 
    @Override
    public boolean isWriteable(Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return true;
    }
 
    @Override
    public long getSize(Object object, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType) {
        return -1;
    }
 
    @Override
    public void writeTo(Object object, Class<?> type, Type genericType,
            Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException,
            WebApplicationException {
        OutputStreamWriter writer = new OutputStreamWriter(entityStream, StandardCharsets.UTF_8);
        try {
            Type jsonType;
            if (type.equals(genericType)) {
                jsonType = type;
            } else {
                jsonType = genericType;
            }
            getGson().toJson(object, jsonType, writer);
        } finally {
            writer.close();
        }
    }
}

