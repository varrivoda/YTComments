//package varrivoda;
//
//import java.io.IOException;
//import java.util.*;
//import java.util.stream.Stream;
//
//import com.google.gson.JsonArray;
//import com.google.gson.JsonElement;
//import com.google.gson.JsonObject;
//import com.google.gson.JsonParser;
//import org.apache.http.HttpEntity;
//import org.apache.http.client.methods.HttpGet;
//import org.apache.http.client.methods.HttpPost;
//import org.apache.http.entity.StringEntity;
//import org.apache.http.impl.client.CloseableHttpClient;
//import org.apache.http.impl.client.HttpClients;
//import org.apache.http.util.EntityUtils;
//
//public class Downloader_old {
//    //TODO сделать нормальную регулярку для ytcfg
//    //пример дляпоиска по регулярке:
//    // <html> ytcfg.set ( {"CLIENT_CA....."});</html>
//    // оригинал на пайтоне r'ytcfg\.set\s*\(\s*({.+?})\s*\)\s*;'
//    private final String RE_YTCFG ="ytcfg\\.set\\s*(\\s*\\(\\{.+?\\}\\)\\s*)\\s*;";
//    private final String RE_DATA = "(?:ytInitialData)\s*=\s*(.+?)\s*;\s*(?:</script|\n)";
//
//    String url = "https://youtube.com/watch?v=9pdQeM0w3xQ";
//    String html;
//    JsonObject ytConfig;
//    JsonObject ytData;
//
//    public Downloader_old(String url) {
//        this.url = url;
////        this.html = getHtmlFromUrl();
////        this.ytConfig = getYtConfigFromHtml();
////        this.ytData = getYtDataFromHtml();
////        this.continuations =
//    }
//
//
//    public List<JsonElement> getComments() {
//        JsonArray comments = new JsonArray();
//        JsonObject newComment = new JsonObject();
//
//       // JsonObject config = this.getConfig();
//        List<JsonElement> continuations = this.getContinuations();
//
//        while (!continuations.isEmpty()) {
//            JsonElement continuation = continuations.remove(0);
//            JsonElement response = ajaxRequest(continuation, this.ytConfig);
//
//            //
//            List<JsonElement> actions =new ArrayList<>();
//            actions.addAll(searchDict(response, "reloadContinuationItemsCommand"));
//            actions.addAll(searchDict(response, "appendContinuationItemsAction"));
//
//            for(JsonElement action: actions){
//                JsonArray items = action.getAsJsonObject().get("continuationItems").getAsJsonArray();
//                for(JsonElement item: items){
//                    if(List.of("comments-section",
//                            "engagement-panel-comments-section",
//                            "shorts-engagement-panel-comments-section")
//                            .contains(action.getAsJsonObject()
//                                            .get("targetId").getAsString())){
//                        continuations.addAll(0,searchDict(item, "continuationEndpoint"));
//                    }
//
//                    if(action.getAsJsonObject().get("targerId").getAsString()
//                            .startsWith("comment-replies-item")
//                            && Stream.of(item.getAsJsonArray()).map(e->e.getAsString())
//                            .toList().contains("continuationItemRenderer")){
//                        continuations.add(searchDict(item,"buttonRenderer").get(0)
//                                .getAsJsonObject().get("command"));
//                    }
//                }
//            }
//            //payloads
//            //payments
//            //toolbarPayloads
//            //toolbarStates
//
//            for(JsonElement comment: searchDict(response, "CommentEntityPayload")){
//                String commentText = comment.getAsJsonObject().get("properties")
//                        .getAsJsonObject().get("content")
//                        .getAsJsonObject().get("content").getAsString();
//                String authorName = comment.getAsJsonObject().get("author")
//                        .getAsJsonObject().get("displayName").getAsString();
//
//                newComment.addProperty("authorName", authorName);
//                newComment.addProperty("commentText", commentText);
//
//                comments.add(newComment);
//            }
//
//        }
//        return List.of(comments);
//    }
//
////    private List<JsonElement> getContinuations() {
////    }
//
//
//
//    private List<JsonElement> searchDict(JsonElement json, String key) {
//        return searchDict(json, key, new ArrayList<>());
//    }
//
//    private List<JsonElement> searchDict(JsonElement json, String key, List<JsonElement> results) {
//        if(json.isJsonObject()){
//            for(String k: json.getAsJsonObject().keySet()){
//                JsonElement value = json.getAsJsonObject().get(k);
//                if(k.equals(key)) {
//                    results.add(value);
//                }
//                searchDict(value, key, results);
//            }
//        }else if (json.isJsonArray()){
//            for(JsonElement element: json.getAsJsonArray())
//                searchDict(element, key, results);
//        }
//        return results;
//    }
//
//    private JsonElement ajaxRequest(JsonElement endpoint, JsonObject ytcfg){
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
//                post.setHeader("Content-type", "application/json");
//                post.setHeader("Accept", "application/json");
//
//            JsonObject data = new JsonObject();
//                data.add("context", ytcfg.get("INNERTUBE_CONTEXT"));
//                data.addProperty("continuation", endpoint
//                            .getAsJsonObject().get("continuationCommand")
//                            .getAsJsonObject().get("token")
//                            .getAsString());
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
//    }
//
//    private void getYtcfgOlddddd(){
//        JsonElement response;
//
//        try(CloseableHttpClient client = HttpClients.createDefault()){
//            HttpGet get = new HttpGet(url);
//
//            response = (JsonElement) JsonParser.parseString(
//                    EntityUtils.toString((HttpEntity)client.execute(get)));
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }
////        return response;
//    }
//}
