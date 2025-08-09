package varrivoda;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.spi.json.GsonJsonProvider;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
//import net.minidev.json.JSONArray;

public class Main {
    static String URL_PREFIX="https://www.youtube.com/watch?v=";
    //private static String WATCH = "phlsVGysQSw";


//    static String SHORT_ID=
    static String URL = "https://www.youtube.com/watch?v=gGn2zoUKsg0";

    private static String WATCH = URL.substring(32);


    public static void main(String[] args) throws IOException {
        //yt забываем ?watch=
        long startTime = System.currentTimeMillis();
        Downloader downloader = new Downloader(URL);

        //writeToFile(downloader.getYtcfg());//Data());

/*MAIN COMMENTS DOWNLOAD AND PRINT METHOD
*
*/
        downloader.download();

/* THIS CODE BELOW IS for transcript
*  also there's some changes in downloder
*  TODO describe some changes in downloder
*/
        getTranscript(downloader);

        System.out.println(String.format("Finished in %d seconds", (System.currentTimeMillis()-startTime)/1000));

    }

    private static void writeToFile(String output) throws IOException {
        String path = "src/test/java/";
        String name=WATCH+"-ytCfg.json";

        writeToFile(output, path, name);
    }

    private static void writeToFile(String output, String path, String name) throws IOException {
        BufferedWriter writer = new BufferedWriter(
                new FileWriter(path+name));
        writer.write(output);
        writer.close();
        System.out.println("ytData has successfully written to " + path);
    }

    private static void getTranscript(Downloader downloader) {
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

                String str = JsonPath.using(conf).parse(phrase).read(pathToPhrase).toString();
                System.out.println(str.substring(1,str.length()-1));
            }

        }
    }
}

