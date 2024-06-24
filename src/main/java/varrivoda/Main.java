package varrivoda;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.JsonPath;
import kong.unirest.*;
import kong.unirest.gson.GsonObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Main {
    public static void main(String[] args) throws UnirestException {
        //TODO сделать нормальную регулярку для ytcfg
        //пример дляпоиска по регулярке:
        // <html> ytcfg.set ( {"CLIENT_CA....."});</html>
        // оригинал на не r'ytcfg\.set\s*\(\s*({.+?})\s*\)\s*;'
        String RE_YTCFG ="ytcfg\\.set\\s*(\\s*\\(\\{.+?\\}\\)\\s*)\\s*;";
        String RE_DATA = "(?:ytInitialData)\s*=\s*(.+?)\s*;\s*(?:</script|\n)";

        JsonElement response;
        String url = "https://youtube.com/watch?v=9pdQeM0w3xQ";

        GetRequest getRequest = Unirest.get(url);
        String html = getRequest.asString().getBody();

        //CONFIG
        String ytConfig = regexSearch(html, RE_YTCFG);
        //TODO переделать в регулярку
        ytConfig = ytConfig.substring(10,ytConfig.length()-2);

        String ytData = regexSearch(html, RE_DATA);
        ytData = ytData.substring(16,ytData.length()-9);
        JsonElement jsonElement =JsonParser.parseString(ytData);

        JsonElement sortMenuu=jsonSearch(jsonElement, "sortFilterSubMenuRenderer")
                .get(0)
                .getAsJsonObject()
                .get("subMenuItems");

        List<JsonElement> sortMenu=jsonSearch(jsonElement, "sortFilterSubMenuRenderer").stream()
                .map(element->{return element.getAsJsonObject().get("subMenuItems");})
                .collect(Collectors.toList());

        List<JsonElement> continuations = new ArrayList<>();

        continuations.add(sortMenu.get(1).getAsJsonArray().get(0).getAsJsonObject().get("serviceEndpoint"));
        continuations.forEach(s-> System.out.println("\n NEXT CONTINUATION:\n" + s));

        List<JsonElement> comments = getComments(continuations, JsonParser.parseString(ytConfig));
        comments.forEach(c-> System.out.println("\n NEXT COMMENT: \n" + c));

//        System.out.println(sortMenu);

//        Downloader_old dpwnloader = new Downloader_old(ytcfg, );
    }

    public static List<JsonElement> getComments(List<JsonElement> continuations, JsonElement ytConfig) throws UnirestException {
        JsonArray comments = new JsonArray();
        JsonObject newComment = new JsonObject();

        while (!continuations.isEmpty()) {
            JsonElement continuation = continuations.remove(0);
            JsonElement response = ajaxRequest(continuation, ytConfig);

            //System.out.println("\n\n NEXT_AJAX_RESPONSE: \n" + response.toString());

            //
            List<JsonElement> actions =new ArrayList<>();
            actions.addAll(jsonSearch(response, "reloadContinuationItemsCommand"));
            actions.addAll(jsonSearch(response, "appendContinuationItemsAction"));
            //actions.forEach(a->System.out.println("\n\nNEXT ACTION: \n" + a));


            for(JsonElement action: actions){
                JsonArray items = action.getAsJsonObject().get("continuationItems").getAsJsonArray();
                for(JsonElement item: items){
                    //System.out.println("\n\n NEXT ITEM:" + item);
                    // System.out.println("ACTION.targetId"+);
                    if(List.of("comments-section",
                                    "engagement-panel-comments-section",
                                    "shorts-engagement-panel-comments-section")
                            .contains(action.getAsJsonObject()
                                    .get("targetId").getAsString())){
                        continuations.addAll(0,jsonSearch(item, "continuationEndpoint"));
                    }

                    if(action.getAsJsonObject().get("targetId").getAsString()
                            .startsWith("comment-replies-item")
                            && Stream.of(item.getAsJsonArray()).map(e->e.getAsString())
                            .toList().contains("continuationItemRenderer")){
                        continuations.add(jsonSearch(item,"buttonRenderer").get(0)
                                .getAsJsonObject().get("command"));
                    }
                }
            }
            //payloads
            //payments
            //toolbarPayloads
            //toolbarStates

            jsonSearch(response, "commentEntityPayload")
                    .forEach(s-> System.out.println("\n\n NEXT CommentEntityPayload: \n" + s));

            for(JsonElement comment: jsonSearch(response, "commentEntityPayload")){

                //System.out.println("\n\n FOR COMMENT: response() \n" + comment.toString());

                String commentText = comment.getAsJsonObject().get("properties")
                        .getAsJsonObject().get("content")
                        .getAsJsonObject().get("content").getAsString();
                String authorName = comment.getAsJsonObject().get("author")
                        .getAsJsonObject().get("displayName").getAsString();

                newComment.addProperty("authorName", authorName);
                newComment.addProperty("commentText", commentText);

                comments.add(newComment);
            }

        }
        return List.of(comments);
    }



    private static List<JsonElement> jsonSearch(JsonElement json, String key) {
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

    private static String regexSearch(String text, String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(text);
        while (m.find())
            return m.group();
        return null;
    }


    private static JsonElement ajaxRequest(JsonElement continuation, JsonElement ytcfg) throws UnirestException {
        String url = "https://www.youtube.com"
                + JsonPath.read(continuation.toString(), "$.commandMetadata.webCommandMetadata.apiUrl");
//                + continuation.getAsJsonObject().get("commandMetadata")
//                .getAsJsonObject().get("webCommandMetadata")
//                .getAsJsonObject().get("apiUrl");

        String key = ytcfg.getAsJsonObject().get("INNERTUBE_API_KEY").getAsString();

        JsonObject data = new JsonObject();
        data.add("context", ytcfg.getAsJsonObject().get("INNERTUBE_CONTEXT"));
        data.add("continuation",JsonParser.parseString(
                JsonPath.read(continuation.toString(), "$.continuationCommand.token")));
//                .getAsJsonObject().get("continuationCommand")
//                .getAsJsonObject().get("token")
//                .getAsString());


        //Unirest.config().setObjectMapper((ObjectMapper) new GsonObjectMapper());
        HttpResponse ajaxResponse = Unirest.post(url)
                .header("Content-type", "application/json")
                .header("Accept", "application/json")
                .body(data)
                .asJson();
        return JsonParser.parseString(ajaxResponse.getBody().toString());
    }

}