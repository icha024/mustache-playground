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
import jdk.nashorn.api.scripting.ScriptUtils;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
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

        Map<String, Object> gMap = new ConcurrentHashMap<>();

        Map<String, Object> functionMap = new HashMap<>();
        functionMap.put("name", (TemplateFunction) s -> "name value");
        functionMap.put("x", (TemplateFunction) s -> {
            Object jpVal = jp.read("$." + s, JsonNode.class);
            log("json node: " + jpVal);
            try {
                String id = UUID.randomUUID()
                        .toString();
                Object evalRes = engine.eval("print(Object.keys("+ jpVal +")); Object.keys(" + jpVal + ")");
                String javaEvalRes = (String) ScriptUtils.convert(evalRes, String.class);
//                String javaEvalRes = (String) evalRes;
                gMap.put(id, javaEvalRes);
                log("id: " + id);
//                log("evalRes: " + Arrays.asList(evalRes).forEach(eachVal -> log(eachVal)));
                log("evalRes: ");
                Arrays.asList(javaEvalRes.split(",")).forEach(eachVal -> log(javaEvalRes));

//                return "[TemplateFunction cb: " + engine.eval("Object.keys(" + jpVal + ")") + "]";
//                return engine.eval("Object.keys(" + jpVal + ")");
//                return "{{#y}}" + id + "{{/y}}";

//                functionMap.put(id, evalRes);
//                functionMap.put(id, Arrays.asList(evalRes));
                functionMap.put(id, javaEvalRes.split(","));
                functionMap.put("secondName", "a second name");
                return "Getting id...:\n {{#" + id + "}} key: {{.}}\n {{/" + id + "}} \n{{secondName}}";
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

    private static void log(Object s) {
        System.out.println("DEBUG> " + s);
    }
}
