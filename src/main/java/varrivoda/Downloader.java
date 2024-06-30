package varrivoda;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.JsonPath;
import kong.unirest.GetRequest;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Downloader {
    private final String RE_YTCFG="ytcfg\\.set\\s*(\\s*\\(\\{.+?\\}\\)\\s*)\\s*;";
    private final String RE_DATA="(?:ytInitialData)\s*=\s*(.+?)\s*;\s*(?:</script|\n)";
    private String ytcfg;
    private String ytData;

    public Downloader(String url) {
        GetRequest getRequest = Unirest.get(url);
        String html = getRequest.asString().getBody();
        this.ytcfg = regexSearch(html, RE_YTCFG);
        ytcfg = ytcfg.substring(10,ytcfg.length()-2);
        this.ytData = regexSearch(html, RE_DATA);
        ytData = ytData.substring(16,ytData.length()-9);
    }

    public void download(){
       //initial data for first filling Continuations list
        List<JsonElement> sortMenu=jsonSearch(JsonParser.parseString(this.ytData), "sortFilterSubMenuRenderer").stream()
                .map(element->{return element.getAsJsonObject().get("subMenuItems");})
                .collect(Collectors.toList());

        List<JsonElement> continuations = new ArrayList<>();
        continuations.add(sortMenu.get(1).getAsJsonArray().get(0).getAsJsonObject().get("serviceEndpoint"));

        List<Comment> comments = getComments(continuations, JsonParser.parseString(ytcfg));
        sortComments(comments);
        printComments(comments);

    }

    private void sortComments(List<Comment> comments) {
        Collections.sort(comments ,new Comparator<Comment>(){
            @Override
            public int compare(Comment comment1, Comment comment2) {
                String comment1IdPart = comment1.getCommentId().split("\\.")[0];
                String comment2IdPart = comment2.getCommentId().split("\\.")[0];
                int commentIdComparison = comment1IdPart.compareTo(comment2IdPart);

                if(commentIdComparison == 0){
                    return Integer.compare(comment1.getIndex(), comment2.getIndex());
                }
                return commentIdComparison;
            }
        });
    }

    private void printComments(List<Comment> comments) {

        comments.forEach(c-> {
            if(c.isReply>0){
                c.authorName = "+--Ответ: " + c.getAuthorName();
                c.setCommentText("          " + c.getCommentText());
            } else {
                c.setAuthorName("\n"+c.getAuthorName());
                c.setCommentText(c.getCommentText() + "\n");
            }

            System.out.println(c.getAuthorName() + " "
                            + c.getCommentTime() + ": \n"
                            + c.getCommentText());
        });

//        comments.forEach(comment-> System.out.println(comment.getIndex()+") "
//                + comment.getAuthorName()+": \n"
//                + comment.getCommentText()+", cId:"
//                + comment.getCommentId()));
    }

    public List<Comment> getComments(List<JsonElement> continuations, JsonElement ytcfg){
        int index=0;
        List<Comment> comments = new ArrayList<>();

        while(!continuations.isEmpty()){
            JsonElement continuation = continuations.remove(0);
            JsonElement ajaxResponse = JsonParser.parseString(ajaxRequest(continuation, ytcfg));

            List<JsonElement> actions = new ArrayList<>();
            actions.addAll(jsonSearch(ajaxResponse, "reloadContinuationItemsCommand"));
            actions.addAll(jsonSearch(ajaxResponse, "appendContinuationItemsAction"));

            appendContinuationsFromActions(continuations, actions);
            //payloads
            //payments
            //toolbarPayloads
            //toolbarStates

            for(JsonElement comment : jsonSearch(ajaxResponse, "commentEntityPayload")) {
                //System.out.println(comment.toString());
                int isReply = JsonPath.read(comment.toString(), "$.properties.replyLevel");
                String commentId = JsonPath.read(comment.toString(), "$.properties.commentId");
                String date = JsonPath.read(comment.toString(), "$.properties.publishedTime");
                String authorName = JsonPath.read(comment.toString(), "$.author.displayName");
                String text = JsonPath.read(comment.toString(), "$.properties.content.content");

                comments.add(new Comment(index++, isReply, commentId, date, authorName, text));
            }
        }

        return comments;
    }

    private void appendContinuationsFromActions(List<JsonElement> continuations, List<JsonElement> actions) {
        for(JsonElement action: actions){
            JsonArray items = action.getAsJsonObject().get("continuationItems").getAsJsonArray();
            for(JsonElement item: items){
                if(List.of("comments-section",
                                "engagement-panel-comments-section",
                                "shorts-engagement-panel-comments-section")
                        .contains(action.getAsJsonObject()
                                .get("targetId").getAsString())){
                    continuations.addAll(0,jsonSearch(item, "continuationEndpoint"));
                }

                if(action.getAsJsonObject().get("targetId").getAsString()
                        .startsWith("comment-replies-item")
                        && //(Stream.of(
                        item.getAsJsonObject().keySet()/*Array()).map(e->e.getAsString())
                        .toList()*/.contains("continuationItemRenderer")){
                    continuations.add(jsonSearch(item,"buttonRenderer").get(0)
                            .getAsJsonObject().get("command"));
                }
            }
        }
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
//                    System.out.println(key+" has found");
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


//    private Collection<? extends JsonElement> getReloadContinuations(String ajaxResponse) {
//        Filter commentEntityPayloadFilter = Filter.filter(Criteria.where("payload").contains("commentEntityPayload"));
//        List<JsonElement> results = JsonPath.parse(ajax).read("$['onResponseReceivedEndpoints'].[?]", commentEntityPayloadFilter);
//        return results;
//    }
//    private List<JsonElement> getCommentsFromAjaxResponse(String ajax){
//        //List<JsonElement> mutations = JsonPath.read(ajax, "$.frameworkUpdates.entityBatchUpdate.mutations");
//        //mutations.stream().forEach(mutation->mutations.put());
////        mutations.stream().filter(mutation->JsonPath.read(mutation, "$.payload") == "commentEntityPayload").collect(Collectors.toList());
//
//        /*Filter commentEntityPayloadFilter = Filter.filter(Criteria.where("payload").contains("commentEntityPayload"));
//        List<JsonElement> results = JsonPath.parse(ajax).read("$.frameworkUpdates.entityBatchUpdate.['mutations'].[?]", commentEntityPayloadFilter);
//        */
//
//
//        return results;
//    }

    private String ajaxRequest(JsonElement continuation, JsonElement ytcfg){
        String url = "https://www.youtube.com"
                + JsonPath.read(continuation.toString(), "$.commandMetadata.webCommandMetadata.apiUrl");

        String key = ytcfg.getAsJsonObject().get("INNERTUBE_API_KEY").getAsString();

        JsonObject body = new JsonObject();
        body.add("context", ytcfg.getAsJsonObject().get("INNERTUBE_CONTEXT"));
        body.add("continuation", JsonParser.parseString(
            JsonPath.read(continuation.toString(), "$.continuationCommand.token")));

        HttpResponse ajaxResponse = Unirest.post(url)
                .header("Content-type", "application/json")
                .header("Accept", "application/json")
                .body(body)
                .asJson();

        return ajaxResponse.getBody().toString();
    }

}
