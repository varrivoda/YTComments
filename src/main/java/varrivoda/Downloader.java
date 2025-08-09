package varrivoda;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import kong.unirest.GetRequest;
import kong.unirest.HttpResponse;
import kong.unirest.Unirest;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class Downloader {
    private static final String PATH_TO_TEST_YT = "src/test/java/yt/";
    private final String RE_YTCFG="ytcfg\\.set\\s*(\\s*\\(\\{.+?\\}\\)\\s*)\\s*;";
    private final String RE_DATA="(?:ytInitialData)\s*=\s*(.+?)\s*;\s*(?:</script|\n)";
    private String ytcfg;
    private String ytData;

    private Byte SORT_BY_NEWEST = 0;

    private long startTimeMillis;
    private long lastTimeMillis;
    private int delta = 0;
    private List<Integer> summary = new ArrayList<>();
    //MultipartBody

    public Downloader(String url) {
        startTimeMillis = System.currentTimeMillis();
        lastTimeMillis = startTimeMillis;

        GetRequest getRequest = Unirest.get(url);
        benchmark("first html get request");

        String html = getRequest.asString().getBody();
        benchmark("getRequest.asString.getBody()");

        saveToFile(html, PATH_TO_TEST_YT, "watch.html");



        this.ytcfg = StringUtils.substring(regexSearch(html, RE_YTCFG),10, -2);
        benchmark("regex_Search YTCFG");

        this.ytData = StringUtils.substring(regexSearch(html, RE_DATA),16, -9);
        benchmark("regex_Search DATA");
    }

    public void download(){
        List<JsonElement> continuations = new ArrayList<>();

        getInitialContinuations(continuations);

        List<Comment> comments = getComments(continuations, JsonParser.parseString(ytcfg));

        sortComments(comments);
        printComments(comments);

        benchmarkResults();

    }

    private void getInitialContinuations(List<JsonElement> continuations) {
        //initial data for first filling Continuations list
        JsonElement sortMenu= jsonSearch(JsonParser.parseString(this.ytData),
                "sortFilterSubMenuRenderer").get(0).getAsJsonObject().get("subMenuItems");

        System.out.println("sortMenu:"+ sortMenu);

        //sortMenu[0] = popular, sortMenu[1] = newest
        continuations.add(sortMenu.getAsJsonArray().get(SORT_BY_NEWEST).getAsJsonObject().get("serviceEndpoint"));
    }

    public List<Comment> getComments(List<JsonElement> continuations, JsonElement ytcfg){
        int index=0;
        List<Comment> comments = new ArrayList<>();

        while(!continuations.isEmpty()){
            JsonElement continuation = continuations.remove(0);

            String response = ajaxRequest(continuation, ytcfg);
            benchmark("ajaxRequest()", true);
            JsonObject ajaxResponse = JsonParser.parseString(response).getAsJsonObject();
            List<JsonElement> actions = new ArrayList<>();
            actions.addAll(actionsSearch(ajaxResponse, new ArrayList<JsonElement>()));
            benchmark("actions.addAll(actionsSearch(ajaxRespnse))");

            appendContinuationsFromActions(continuations, actions);



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
            // для каждого action ищем continuationItems
            JsonArray items = action.getAsJsonObject().get("continuationItems").getAsJsonArray();
            for(JsonElement item: items){
                // Для каждого action.continuationItems проверяем, если в нем targetId == "comments-section" и т.д.
                //если да, то добавляем из текущего item в наш лист токенов
                String targetId = action.getAsJsonObject().get("targetId").getAsString();
                if(targetId.equals("comments-section")
                        || targetId.equals("engagement-panel-comments-section")
                        || targetId.equals("shorts-engagement-panel-c   omments-section")){
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
                    System.out.println(key1 +" has found");
                }else if(k.equals(key2)) {
                    results.add(value);
                    System.out.println(key2 +" has found");
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

    private String ajaxRequest(JsonElement continuation, JsonElement ytcfg){
        System.out.println("AJAX request");
        String key = ytcfg.getAsJsonObject().get("INNERTUBE_API_KEY").getAsString();
        String url = "https://www.youtube.com"
                + JsonPath.read(continuation.toString(), "$.commandMetadata.webCommandMetadata.apiUrl") + "?key=";//+key;

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

    protected String transcriptAjaxRequest(){
        System.out.println("AJAX request");
        // как ни странно, но всё работает без извлеченного апи-ключа
        //String key = this.ytcfg.getAsJsonObject().get("INNERTUBE_API_KEY").getAsString();
        String url = "https://www.youtube.com/youtubei/v1/get_transcript";
        //+ JsonPath.read(this.ytData, "$.commandMetadata.webCommandMetadata.apiUrl") + "?key=";//+key;

        JsonObject body = new JsonObject();
        body.add("context", JsonParser.parseString(ytcfg).getAsJsonObject().get("INNERTUBE_CONTEXT"));

        String transcriptEndpoint = null;

        for (int i = 4; i <= 6; i++) {
            try {
                transcriptEndpoint = getTranscriptEndpoint(i); // получение вынесено в отдельный метод
                break; // если нашли — выходим из цикла
            } catch (PathNotFoundException | ArrayIndexOutOfBoundsException | NullPointerException e) {
                System.out.println("PathNotFoundException: engagementPanel#"+i+" is not for transcript");
                // приложение не упало, просто пробуем следующий индекс
            }
        }

        body.add("params", JsonParser.parseString(transcriptEndpoint));

        HttpResponse ajaxResponse = Unirest.post(url)
                .header("Content-type", "application/json")
                .header("Accept", "application/json")
                .body(body)
                .asJson();

        return ajaxResponse.getBody().toString();
    }

    private String getTranscriptEndpoint(int engagementPanelIndex) {
        String transcriptEndpoint;
        transcriptEndpoint = JsonPath.read(this.ytData,
                "$.engagementPanels[" +
                            engagementPanelIndex +
                        "]" +
                        ".engagementPanelSectionListRenderer" +
                        ".content" +
                        ".continuationItemRenderer" +
                        ".continuationEndpoint" +
                        ".getTranscriptEndpoint" +
                        ".params");
        return transcriptEndpoint;
    }

    private void saveToFile(String html, String pathToTestYt, String filename) {
        //скачаем html для поcледующих тестов
        try (BufferedWriter writer = new BufferedWriter(
                new FileWriter(pathToTestYt+filename))) {
            writer.write(html);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void saveToFile(String html) {
        saveToFile(html, "src/test/java/yt/", "watch.html");
    }

    public String getYtData(){
        return this.ytData;
    }

    public String getYtcfg(){
        return this.ytcfg;
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
        for(int j=0;j<comments.size();j++){//(Comment c: comments){
            Comment c = comments.get(j);
            String gap="\n";
            String padding = "";
            String authorPadding = "";
            //если не ответ, но имеет отеты
            if(c.isReply ==0 && j<comments.size()-1 && comments.get(j + 1).isReply>0){
                c.setCommentText(c.getCommentText() + "\n  Ответы:");
            } else  if (c.isReply>0){
                gap = "";
                padding = "  ";
                authorPadding = " -";
                //c.setAuthorName("\n"+c.getAuthorName());
                //c.setCommentText(c.getCommentText());
            }

            System.out.println(gap + authorPadding + /*c.getIndex() + ") " +*/ c.getAuthorName() + " "
                    + c.getShortDate() + ": \n"
                    + padding + c.getCommentText());
        }
    }

    private void benchmark(String name) {
        benchmark(name, false);
    }

    private void benchmarkResults() {
        System.out.println("\n\n");
        System.out.println("Number of meaningful method calls = " + summary.size());
        System.out.println("Average benchmark = " + summary.stream().collect(Collectors.averagingInt(s-> s)));
        System.out.println("Min benchmark = " + summary.stream().min(Comparator.comparingInt(s -> s)));
        System.out.println("Max benchmark = " + summary.stream().max(Comparator.comparingInt(s->s)));
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
