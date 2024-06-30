package varrivoda;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Comment {
    int index;
    int isReply;// = comment.get("properties").get("replyLevel").getAsInt();
    String commentId;// = comment.get("properties").get("commentId").getAsString();
    String commentTime;// = comment.get("properties").get("publishedTime").getAsString();
    String authorName;// = comment.get("author").get("displayName").getAsString();
    String commentText;// = comment.get("properties").get("content").get("content")


}
