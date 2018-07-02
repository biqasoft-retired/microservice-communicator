package com.biqasoft.microservice.communicator.internal;

import com.biqasoft.microservice.communicator.exceptions.InvalidStateException;
import com.biqasoft.microservice.communicator.interfaceimpl.annotation.MicroPayloadVar;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.lang.reflect.Parameter;
import java.util.List;

/**
 * Created by ya on 4/8/2017.
 */
@Service
public class JsonObjectFromParametersService {

    private final JsonNodeFactory factory = JsonNodeFactory.instance;
    private final ObjectMapper objectMapper;

    @Autowired
    public JsonObjectFromParametersService(DefaultObjectMapperConfiguration.DefaultObjectMapperConfigurationData objectMapper) {
        this.objectMapper = objectMapper.getObjectMapper();
    }

    /**
     * @param objects    passed objects to interface method
     * @param parameters interface method definition
     * @return json object(payload)
     */
    public Object createJsonRequestObjectFromParameters(Object[] objects, List<Parameter> parameters) {
        Object payload;
        ObjectNode rootNode = factory.objectNode();
        payload = rootNode;

        for (Parameter parameter : parameters) {
            MicroPayloadVar param = AnnotationUtils.findAnnotation(parameter, MicroPayloadVar.class);
            if (param == null) {
                continue;
            }

            String jsonName;
            String delimiter = ".";

            if (!StringUtils.isEmpty(param.path())) {
                jsonName = param.path();
            } else {
                // java 8 param reflection
                if (!parameter.isNamePresent()) {
                    throw new InvalidStateException("You try to use java 8 param name extract via reflection, but looks like, not compile javac with -parameters");
                }
                jsonName = parameter.getName();
                delimiter = "_";
            }

            ObjectNode latestNode = rootNode;
            if (jsonName.contains(delimiter)) {
                String[] split = jsonName.split(delimiter.equals(".") ? "\\." : "_");
                int i = 0;
                for (String s : split) {
                    if ((i + 1) == split.length) {
                        break;
                    }

                    if (latestNode.path(s).isObject()) {
                        latestNode = (ObjectNode) latestNode.path(s);
                    } else {
                        ObjectNode newNode = factory.objectNode();
                        latestNode.set(s, newNode);
                        latestNode = newNode;
                    }
                    i++;
                }
                jsonName = split[split.length - 1];
            }

            JsonNode node = objectMapper.convertValue(objects[parameters.indexOf(parameter)], JsonNode.class);
            latestNode.set(jsonName, node);
        }
        return payload;
    }

}
