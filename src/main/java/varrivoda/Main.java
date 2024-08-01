package varrivoda;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.Predicate;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;

import java.util.*;
//import net.minidev.json.JSONArray;

public class Main {
    public static void main(String[] args) {
        long startTime = System.currentTimeMillis();
        Downloader downloader = new Downloader(/* for transcript*/"https://www.youtube.com/watch?v=phlsVGysQSw");///*"https://www.youtube.com/watch?v=4QpnCbi5QyQ");*/"https://youtube.com/watch?v=mxLgL5pk0qg");
        //downloader.download();
        String pathToPhrasesArray = "$.actions[0]" +
                ".updateEngagementPanelAction" +
                ".content" +
                ".transcriptRenderer" +
                ".content" +
                ".transcriptSearchPanelRenderer" +
                ".body" +
                ".transcriptSegmentListRenderer" +
                ".initialSegments";

        String pathToPhrase = "$.transcriptSegmentRenderer" +
                ".snippet" +
                ".runs[0]" +
                ".text";

        String pathToHeader = "$.transcriptSectionHeaderRenderer" +
                ".snippet" +
                ".simpleText";


        List<String> transcript = new ArrayList<>();
//        JsonArray phrasesArray = JsonParser.parseString(JsonPath.read(downloader.transcriptAjaxRequest(), path1)).getAsJsonArray();

        Configuration conf= Configuration.builder()
                .jsonProvider(new GsonJsonProvider())
                .options(Option.SUPPRESS_EXCEPTIONS)
                .build();

        JsonArray phrasesList = JsonPath.using(conf)
                .parse(downloader.transcriptAjaxRequest())
                .read(pathToPhrasesArray);


        for(JsonElement phrase: phrasesList){
            Set<String> keySet = phrase.getAsJsonObject().keySet();

            if(keySet.contains("transcriptSectionHeaderRenderer")){
                System.out.println(">>"+JsonPath.using(conf).parse(phrase).read(pathToHeader).toString());
            }

            if(keySet.contains("transcriptSegmentRenderer")){
                System.out.println(JsonPath.using(conf).parse(phrase).read(pathToPhrase).toString());
            }

        }
        //System.out.println(downloader.transcriptAjaxRequest());

        System.out.println(String.format("Finished in %d seconds", (System.currentTimeMillis()-startTime)/1000));

    }
}

