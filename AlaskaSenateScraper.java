import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AlaskaSenateScraper {
    public static void main(String[] args) {
        try {
            String base = "https://akleg.gov";
            // determine chamber from args: default is senate, accept --chamber=house or --chamber house or first arg
            String chamber = "senate";
            if (args != null && args.length > 0) {
                for (int i=0;i<args.length;i++) {
                    String a = args[i].trim().toLowerCase();
                    if (a.startsWith("--chamber=")) chamber = a.substring("--chamber=".length());
                    else if (a.equals("--chamber") && i+1<args.length) { chamber = args[i+1].toLowerCase(); i++; }
                    else if (a.equals("house") || a.equals("senate")) chamber = a;
                }
            }
            String pagePath = "/senate.php";
            String outFile = "alaska_senate.json";
            if ("house".equals(chamber)) { pagePath = "/house.php"; outFile = "alaska_house.json"; }
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();

        // Fetch chamber page
        System.out.println("Using chamber: " + chamber + " (fetching " + pagePath + ")");
        HttpRequest req = HttpRequest.newBuilder(URI.create(base + pagePath))
                    .GET()
                    .header("User-Agent", "Java HttpClient")
                    .build();
        System.out.println("Fetching page: " + pagePath + "...");
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String html = resp.body();

            // Process each <li> block representing a person; HTML varies, so parse the li content
            
            // anchor that wraps the image (img-holder) or the <img> itself so we pick the member's profile link
            Pattern hrefPat = Pattern.compile("<a\\s+href=\\\"([^\\\"]+)\\\"[^>]*>\\s*(?:<div\\s+class=\\\"img-holder\\\"|<img)", Pattern.CASE_INSENSITIVE);
            Pattern namePat = Pattern.compile("<strong class=\\\"name\\\">([^<]+)</strong>", Pattern.CASE_INSENSITIVE);
            Pattern posPat = Pattern.compile("<span class=\\\"position\\\">([^<]+)</span>", Pattern.CASE_INSENSITIVE);
            Pattern dlDetailPat = Pattern.compile("<dt>([^<]+)</dt>\\s*<dd>([^<]+)</dd>", Pattern.CASE_INSENSITIVE);
            Pattern listInfoDistrict = Pattern.compile("<li>\\s*District\\s*<span>([^<]+)</span>", Pattern.CASE_INSENSITIVE);
            Pattern listInfoParty = Pattern.compile("<li>\\s*Party:\\s*<span>([^<]+)</span>", Pattern.CASE_INSENSITIVE);
            Pattern listInfoCity = Pattern.compile("<li>\\s*City:\\s*<span>([^<]+)</span>", Pattern.CASE_INSENSITIVE);
            Pattern phonePatAlt = Pattern.compile("<dd>(\\(?\\d{3}\\)?[-.\\s]?\\d{3}[-.\\s]?\\d{4})</dd>", Pattern.CASE_INSENSITIVE);

            JSONArray out = new JSONArray();

            // Match any <li> that contains a <strong class="name"> to find person entries
            Pattern personPat = Pattern.compile("<li[\\s\\S]*?<strong class=\\\"name\\\">([^<]+)</strong>[\\s\\S]*?</li>", Pattern.CASE_INSENSITIVE);
            Matcher lm2 = personPat.matcher(html);
            java.util.Set<String> seen = new java.util.LinkedHashSet<>();
            while (lm2.find()) {
                String li = lm2.group(0);

                // href
                String href = "";
                Matcher hm = hrefPat.matcher(li);
                if (hm.find()) href = hm.group(1).trim();

                // name
                String name = "";
                Matcher nm = namePat.matcher(li);
                if (nm.find()) name = nm.group(1).trim();

                if (name.isEmpty()) continue; // skip non-person li

                // position (leadership) - if present use as title
                String title = "Senator";
                Matcher pm = posPat.matcher(li);
                if (pm.find()) title = pm.group(1).trim();

                // normalize url
                String profile = "";
                if (!href.isEmpty()) {
                    if (href.startsWith("http")) profile = href;
                    else if (href.startsWith("//")) profile = "https:" + href;
                    else if (href.startsWith("/")) profile = base + href;
                    else profile = base + "/" + href;
                }

                // dedupe by profile or name
                String dedupeKey = !profile.isEmpty() ? profile : name;
                if (seen.contains(dedupeKey)) continue;
                seen.add(dedupeKey);

                // party/city/district/phone
                String party = "", city = "", district = "", phone = "";
                Matcher lmDist = listInfoDistrict.matcher(li);
                if (lmDist.find()) district = lmDist.group(1).trim();
                Matcher lmParty = listInfoParty.matcher(li);
                if (lmParty.find()) party = lmParty.group(1).trim();
                Matcher lmCity = listInfoCity.matcher(li);
                if (lmCity.find()) city = lmCity.group(1).trim();

                // fallback: dl details
                Matcher dm = dlDetailPat.matcher(li);
                while (dm.find()) {
                    String label = dm.group(1).trim().toLowerCase();
                    String value = dm.group(2).trim();
                    switch (label) {
                        case "party:": party = value; break;
                        case "city:": city = value; break;
                        case "district:": district = value; break;
                        case "phone:": if (phone.isEmpty()) phone = value; break;
                        case "toll-free:": if (phone.isEmpty()) phone = value; break;
                    }
                }

                // phone alternative
                Matcher phm = phonePatAlt.matcher(li);
                if (phm.find() && phone.isEmpty()) phone = phm.group(1).trim();

                JSONObject obj = new JSONObject();
                obj.put("name", name);
                obj.put("title", title);
                obj.put("profile", profile);
                obj.put("url", profile);
                obj.put("dob", "");
                obj.put("type", "current");
                obj.put("country", "Alaska");
                obj.put("party", party);

                StringBuilder other = new StringBuilder();
                if (!city.isEmpty()) other.append("City: ").append(city);
                if (!district.isEmpty()) { if (other.length()>0) other.append(" | "); other.append("District: ").append("District ").append(district); }
                if (!phone.isEmpty()) { if (other.length()>0) other.append(" | "); other.append("Phone: ").append(phone); }
                obj.put("otherinfo", other.toString());

                out.put(obj);
                System.out.println("  scraped: " + name);
            }

            System.out.println("Finished. Writing JSON to " + outFile + "...");
            try (FileWriter fw = new FileWriter(outFile, StandardCharsets.UTF_8)) {
                fw.write(out.toString(4));
            }

            System.out.println("Wrote " + outFile);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
