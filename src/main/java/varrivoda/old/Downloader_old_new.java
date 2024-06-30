package varrivoda.old;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.jayway.jsonpath.JsonPath;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import kong.unirest.UnirestException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Downloader_old_new {
    String RE_YTCFG ="ytcfg\\.set\\s*(\\s*\\(\\{.+?\\}\\)\\s*)\\s*;";
    String RE_DATA = "(?:ytInitialData)\s*=\s*(.+?)\s*;\s*(?:</script|\n)";

    String html;

    public Downloader_old_new(String url) throws UnirestException {
        html = Unirest.get(url).asString().getBody();
        String ytConfig = regexSearch(html, RE_YTCFG);
        ytConfig = ytConfig.substring(10, ytConfig.length() - 2);
        String ytData = regexSearch(html, RE_DATA);


    }



//    public List<JsonObject> getCommentsFromContinuation(String continuation, String ytConfig){
//
//    }

    private static String regexSearch(String text, String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(text);
        while (m.find())
            return m.group();
        return null;
    }

    private List<JsonElement> jsonSearch(JsonElement json, String key) {
        return jsonSearch(json, new ArrayList<>(), key);
    }
    private static List<JsonElement> jsonSearch(JsonElement json, List<JsonElement> results, String key) {
        if(json.isJsonObject()){
            JsonObject jsonObject = json.getAsJsonObject();
            for (String k : jsonObject.keySet()) {
                JsonElement value = jsonObject.get(k);
                if(k.equals(key)) {
                    results.add(value);
                    System.out.println(key+" has found");
                }
                jsonSearch(value, results, key);
            }
        } else if (json.isJsonArray()) {
            JsonArray jsonArray = json.getAsJsonArray();
            for (JsonElement element : jsonArray) {
                jsonSearch(element, results, key);
            }
        }else if (json.isJsonPrimitive()) {
            //nothing to do
        }
        return results;
    }

    private JsonElement ajaxRequest(String continuation, JsonObject ytcfg) throws UnirestException {
        String url = "https://www.youtube.com"
                + JsonPath.read(continuation, "$.commandMetadata.webCommandMetadata.apiUrl");
//                + continuation.getAsJsonObject().get("commandMetadata")
//                .getAsJsonObject().get("webCommandMetadata")
//                .getAsJsonObject().get("apiUrl");

        String key = ytcfg.get("INNERTUBE_API_KEY").getAsString();

        JsonObject data = new JsonObject();
        data.add("context", ytcfg.get("INNERTUBE_CONTEXT"));
        data.addProperty("continuation",
                (String) JsonPath.read(continuation, "$.continuationCommand.token"));
//                .getAsJsonObject().get("continuationCommand")
//                .getAsJsonObject().get("token")
//                .getAsString());

        HttpResponse ajaxResponse = Unirest.post(url)
                .header("Content-type", "application/json")
                .header("Accept", "application/json")
                .body(data)
                .asJson();
        return (JsonElement) ajaxResponse.getBody();
    }
}


//        private JsonElement ajaxRequestOld(JsonElement endpoint, JsonObject ytcfg){
//        JsonElement response;
//        String url = "https://www.youtube.com"
//                + endpoint.getAsJsonObject().get("commandMetadata")
//                .getAsJsonObject().get("webCommandMetadata")
//                .getAsJsonObject().get("apiUrl");
//
//        String key = ytcfg.get("INNERTUBE_API_KEY").getAsString();
//
//        try(CloseableHttpClient client = HttpClients.createDefault()){
//            HttpPost post = new HttpPost(url + "?key=" + key);
//            post.setHeader("Content-type", "application/json");
//            post.setHeader("Accept", "application/json");
//
//            JsonObject data = new JsonObject();
//            data.add("context", ytcfg.get("INNERTUBE_CONTEXT"));
//            data.addProperty("continuation", endpoint
//                    .getAsJsonObject().get("continuationCommand")
//                    .getAsJsonObject().get("token")
//                    .getAsString());
//            post.setEntity(new StringEntity(data.toString(), "UTF-8"));
//
//            response = (JsonElement) JsonParser.parseString(
//                    EntityUtils.toString((HttpEntity)client.execute(post)));
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
//        return response;
//
//}
