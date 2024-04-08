import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Test {

    public static void main(String[] args) {
//        long a;
//        try {
//            a = one("http://221.226.4.10:9901/tsfile/live/0001_1.m3u8?key=txiptv&playlive=1&authid=0");
//            System.out.println(a);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        try {
//            a = one("http://123.138.216.44:9902/tsfile/live/0001_1.m3u8?key=txiptv&playlive=1&authid=0");
//            System.out.println(a);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
        try {
            two("123.138.216.44");
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

    private static long two(String ip) throws IOException {
        URL url = new URL("https://qifu-api.baidubce.com/ip/geo/v1/district?ip=" + ip);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
//        conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
        conn.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0");

        conn.setDoOutput(true);
        conn.setConnectTimeout(5000);
        conn.setReadTimeout(5000);
        long start = System.currentTimeMillis();

        int code = conn.getResponseCode();
        if (code != HttpURLConnection.HTTP_OK) {
            throw new RuntimeException("Failed : HTTP error code : " + code);
        }

        BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        String inputLine;
        StringBuilder content = new StringBuilder();

        while ((inputLine = in.readLine()) != null) {
            content.append(inputLine);
            content.append(System.lineSeparator());
            System.out.println(inputLine);
        }

        in.close();
        conn.disconnect();

        return System.currentTimeMillis() - start;
    }

}