package varrivoda;

public class YTRequestBody {
    String context;
    String continuation;

    public YTRequestBody(String context, String continuation) {
        this.context = context;
        this.continuation = continuation;
    }

    public String getContinuation() {
        return continuation;
    }

    public void setContinuation(String continuation) {
        this.continuation = continuation;
    }


    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}