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


    private long startTimeMillis;
    private long lastTimeMillis;
    private int delta = 0;
    private List<Integer> summary = new ArrayList<>();

    public Downloader(String url) {
        startTimeMillis = System.currentTimeMillis();
        lastTimeMillis = startTimeMillis;

        GetRequest getRequest = Unirest.get(url);
        benchmark("first html get request");
        String html = getRequest.asString().getBody();
        benchmark("getRequest.asString.getBody()");
        System.out.println("html.length is "+html.length());
        this.ytcfg = regexSearch(html, RE_YTCFG);
        benchmark("regex_Search YTCFG");
        ytcfg = ytcfg.substring(10,ytcfg.length()-2);
        this.ytData = regexSearch(html, RE_DATA);
        benchmark("regex_Search DATA");
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

//        sortComments(comments);
//        printComments(comments);

        benchmarkResults();

    }

    private void benchmarkResults() {
        System.out.println("Number of meaningful method calls = " + summary.size());
        System.out.println("Average benchmark = " + summary.stream().collect(Collectors.averagingInt(s-> s)));
        System.out.println("Min benchmark = " + summary.stream().min(Comparator.comparingInt(s -> s)));
        System.out.println("Max benchmark = " + summary.stream().max(Comparator.comparingInt(s->s)));


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
        for(Comment c: comments){
            if(c.isReply>0){
                c.authorName = "+--Ответ: " + c.getAuthorName();
                c.setCommentText("          " + c.getCommentText());
            } else {
                c.setAuthorName("\n"+c.getAuthorName());
                c.setCommentText(c.getCommentText() + "\n");
            }

            System.out.println(c.getIndex() + ") " + c.getAuthorName() + " "
                            + c.getCommentTime() + ": \n"
                            + c.getCommentText());
        }

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

            String response = ajaxRequest(continuation, ytcfg);
            benchmark("ajaxRequest()", true);
            JsonObject ajaxResponse = JsonParser.parseString(response).getAsJsonObject();
//            benchmark("parseString(firstResponse)");
            List<JsonElement> actions = new ArrayList<>();

//            JsonArray responseEndpoints = ajaxResponse.get("onResponseReceivedEndpoints").getAsJsonArray();
//            for (JsonElement element : responseEndpoints) {
//                if (element.toString().contains("reloadContinuationItemsCommand"))
//                    actions.add(element.toString().//indexOf("reloadContinuationItemsCommand"));
//            }

            //get("reloadContinuationItemsCommand");});
            //->element."reloadContinuationItemsCommand"
            //либо -> "appendContinuationItemsAction"


            actions.addAll(actionsSearch(ajaxResponse, new ArrayList<JsonElement>()));
//            actions.addAll(jsonSearch(ajaxResponse, "reloadContinuationItemsCommand"));
//            actions.addAll(jsonSearch(ajaxResponse, "appendContinuationItemsAction"));
            benchmark("actions.addAll(actionsSearch(ajaxRespnse))");


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

    private List<JsonElement> jsonSearch(JsonElement json, String key) {
        return jsonSearch(json, new ArrayList<>(), key);
    }
    private List<JsonElement> jsonSearch(JsonElement json, List<JsonElement> results, String key) {
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

    private List<JsonElement> actionsSearch(JsonElement json, List<JsonElement> results) {
        String key1 = "reloadContinuationItemsCommand";
        String key2 = "appendContinuationItemsAction";
        //System.out.println("new actionsSearch");
        if(json.isJsonObject()){
            JsonObject jsonObject = json.getAsJsonObject();
            for (String k : jsonObject.keySet()) {
                JsonElement value = jsonObject.get(k);
                if(k.equals(key1)) {
                    results.add(value);
                    System.out.println("key1 has found");
                }else if(k.equals(key2)) {
                    results.add(value);
                    System.out.println("key2 has found");
                }
                actionsSearch(value, results);
            }
        } else if (json.isJsonArray()) {
            JsonArray jsonArray = json.getAsJsonArray();
            for (JsonElement element : jsonArray) {
                //System.out.println("jsonArray being iterated...");
                actionsSearch(element, results);
            }
        }else if (json.isJsonPrimitive()) {
            //nothing to do
        }
        return results;
    }
    private String regexSearch(String text, String pattern) {
        Pattern p = Pattern.compile(pattern);
        Matcher m = p.matcher(text);
        while (m.find())
            return m.group();
        return null;
    }



//    private String getReloadContinuationsFromAjaxResponse(JsonElement ajaxResponse){
//        ajaxResponse.getAsJsonObject().get("onResponseReceivedEndpoints").getAsJsonArray()//get("reloadContinuationItemsCommand")
//    }

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
        System.out.println("AJAX request");
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


    private void benchmark(String name) {
        benchmark(name, false);
    }

    private void benchmark(String name, boolean isSummaryNeeded) {
        delta = (int) (System.currentTimeMillis() - lastTimeMillis);
        lastTimeMillis = System.currentTimeMillis();
        System.out.println("Timestamp for " + name + " = "
                + (System.currentTimeMillis() - startTimeMillis) + ", lead time = " + delta +"\n");
        if(isSummaryNeeded){
            summary.add(delta);
        }

    }

}
