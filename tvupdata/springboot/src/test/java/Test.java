import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

public class Test {

    public static void main(String[] args) {
        long a;
        try {
            a = one("http://221.226.4.10:9901/tsfile/live/0001_1.m3u8?key=txiptv&playlive=1&authid=0");
            System.out.println(a);
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            a = one("http://123.138.216.44:9902/tsfile/live/0001_1.m3u8?key=txiptv&playlive=1&authid=0");
            System.out.println(a);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static long one(String urlstr) throws IOException {
        URL url = new URL(urlstr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");

        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        long start = System.currentTimeMillis();

        int code = conn.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Failed : HTTP error code : " + code);
        }

        conn.disconnect();

        return System.currentTimeMillis() - start;
    }

}