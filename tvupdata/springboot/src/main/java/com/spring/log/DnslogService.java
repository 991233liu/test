package com.spring.log;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson.JSONObject;

@Service
public class DnslogService implements CommandLineRunner {

    @SuppressWarnings("unchecked")
    @Override
    public void run(String... args) {
        try {
            List<String> logs = FileUtils.readLines(new File("d:\\temp\\dns-log\\dns-log.html"), "UTF-8");
            if (logs == null || logs.size() == 0)
                return;
            List<String> db = FileUtils.readLines(new File("d:\\temp\\dns-log\\allDns.db"), "UTF-8");
            Map<String, Map<String, String>> allDns = JSONObject.parseObject(db.get(0), HashMap.class);
            Map<String, Map<String, String>> dns2map = JSONObject.parseObject(db.get(1), HashMap.class);
            Collections.reverse(logs);
            for (String string : logs) {
                if (string.indexOf("</td><td align=\"left\">client ") > 0) {
                    String[] log = string.split("</td><td align=\"left\">client ");
                    int ln = log[0].indexOf("<td align=\"center\">") + 19;
                    String dateStr = log[0].substring(ln, ln + 23);
                    System.out.println(dateStr);
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSS", Locale.ENGLISH);
                    Date date = formatter.parse(dateStr);
                    System.out.println(log[1]);
                    String ip = log[1].substring(0, log[1].indexOf("#"));
                    System.out.println(ip);
                    String dns = log[1].substring(log[1].indexOf("(") + 1, log[1].indexOf(")"));
                    System.out.println(dns);
                    if ("192.168.2.12".equals(ip)) {
                        Map<String, String> map = null;
                        if (allDns.containsKey(dns)) {
                            map = allDns.get(dns);
                            String lastUpdated = map.get("lastUpdated");
                            Date d1 = formatter.parse(lastUpdated);
                            if (d1.before(date))
                                map.put("lastUpdated", dateStr);
                        } else {
                            map = new HashMap<>();
                            map.put("lastUpdated", dateStr);
                            map.put("dateCreated", dateStr);
                            allDns.put(dns, map);
                        }
                        String[] d = dns.split("\\.");
                        ln = d.length;
                        String d2 = d[ln - 2] + "." + d[ln - 1];
                        if ("com.cn".equals(d2))
                            d2 = d[ln - 3] + "." + d[ln - 2] + "." + d[ln - 1];
                        if (dns2map.containsKey(d2)) {
                            map = dns2map.get(d2);
                            String lastUpdated = map.get("lastUpdated");
                            Date d1 = formatter.parse(lastUpdated);
                            if (d1.before(date))
                                map.put("lastUpdated", dateStr);
                        } else {
                            map = new HashMap<>();
                            map.put("lastUpdated", dateStr);
                            map.put("dateCreated", dateStr);
                            dns2map.put(d2, map);
                        }
                    }
                }
            }
            List<String> dns2 = new ArrayList<>();
            dns2.add("2级域名,创建时间,最后访问时间");
            for (Map.Entry<String, Map<String, String>> entry : dns2map.entrySet()) {
                dns2.add(entry.getKey() + "," + entry.getValue().get("dateCreated") + "," + entry.getValue().get("lastUpdated"));
            }
            FileUtils.write(new File("d:\\temp\\dns-log\\dns2.csv"), dns2.stream().collect(Collectors.joining("\n")), "GBK");
            db = new ArrayList<>();
            db.add(JSONObject.toJSONString(allDns));
            db.add(JSONObject.toJSONString(dns2map));
            FileUtils.write(new File("d:\\temp\\dns-log\\allDns.db"), db.stream().collect(Collectors.joining("\n")), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
