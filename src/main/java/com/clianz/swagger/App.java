package com.clianz.swagger;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;
import com.github.mustachejava.TemplateFunction;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import com.jayway.jsonpath.spi.json.JacksonJsonNodeJsonProvider;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

public class App {

    public static void main(String[] args) throws URISyntaxException, IOException {
        ScriptEngineManager engineManager =
                new ScriptEngineManager();
        ScriptEngine engine =
                engineManager.getEngineByName("nashorn");

        URL resource = App.class.getResource("/api.yaml");
        String content = new String(Files.readAllBytes(Paths.get(resource.toURI())));
        String swaggerJson = convertYamlToJson(content);
        log(swaggerJson);
        Configuration jpConfig = Configuration.builder()
                .jsonProvider(new JacksonJsonNodeJsonProvider())
                .build();
        ReadContext jp = JsonPath.parse(swaggerJson, jpConfig);

        MustacheFactory mf = new DefaultMustacheFactory();
        Mustache mustache = mf.compile("template.mustache");

        Map<String, Object> gMap = new ConcurrentHashMap();

        Map<String, Object> functionMap = new HashMap<>();
        functionMap.put("name", (TemplateFunction) s -> "name value");
        functionMap.put("x", (TemplateFunction) s -> {
            Object jpVal = jp.read("$." + s, JsonNode.class);
            log("json node: " + jpVal);
            try {
                String id = UUID.randomUUID()
                        .toString();
                Object evalRes = engine.eval("Object.keys(" + jpVal + ")");
                gMap.put(id, evalRes);
                log("id: " + id);
                log("evalRes: " + evalRes);
//                return "[TemplateFunction cb: " + engine.eval("Object.keys(" + jpVal + ")") + "]";
//                return engine.eval("Object.keys(" + jpVal + ")");
                return "{{#y}}" + id + "{{/y}}";
            } catch (ScriptException e) {
                throw new IllegalStateException(e);
            }
        });
        functionMap.put("y", //new Object() {
//            Function lambda() {
//                log("y triggered");
//                return s -> {
//                    log("y got: " + s);
//                    return gMap.get(s);
//                };
//            }
                //}
                (Function) s -> {
                    log("y got: " + s);
                    return gMap.get(s);
                }
        );
        mustache.execute(new PrintWriter(System.out), functionMap)
                .flush();
    }

    private static String convertYamlToJson(String yaml) throws IOException {
        ObjectMapper yamlReader = new ObjectMapper(new YAMLFactory());
        Object obj = yamlReader.readValue(yaml, Object.class);

        ObjectMapper jsonWriter = new ObjectMapper();
        return jsonWriter.writeValueAsString(obj);
    }

    private static void log(String s) {
        System.out.println(s);
    }
}
