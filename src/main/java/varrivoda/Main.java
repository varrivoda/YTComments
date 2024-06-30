package varrivoda;

public class Main {
    public static void main(String[] args){
        long startTime = System.currentTimeMillis();
        Downloader downloader = new Downloader("https://youtube.com/watch?v=mxLgL5pk0qg");
        downloader.download();
        System.out.println(String.format("Finished in %d seconds", (System.currentTimeMillis()-startTime)/1000));
    }
}
