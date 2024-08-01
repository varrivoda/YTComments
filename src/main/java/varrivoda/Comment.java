package varrivoda;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Data
@AllArgsConstructor
public class Comment {
    int index;
    int isReply;// = comment.get("properties").get("replyLevel").getAsInt();
    String commentId;// = comment.get("properties").get("commentId").getAsString();
    String commentTime;// = comment.get("properties").get("publishedTime").getAsString();
    String authorName;// = comment.get("author").get("displayName").getAsString();
    String commentText;// = comment.get("properties").get("content").get("content")

    String getShortDate(){
        List<String> words =  new ArrayList<>(List.of(this.commentTime.split("\s")));
        String number= words.remove(0) + " ";
        String edited="";
        if(words.get(words.size()-1).equals("(изменено)")) {
            edited = "(изм)";
            words.remove(words.size()-1);
        }

        return number
                + words.stream()
                .map(s->s.substring(0,1)+".")
                .collect(Collectors.joining())
                + edited;
    }


}
