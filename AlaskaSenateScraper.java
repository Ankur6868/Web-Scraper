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
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .followRedirects(HttpClient.Redirect.ALWAYS)
                    .build();

            // Fetch senate page
            HttpRequest req = HttpRequest.newBuilder(URI.create(base + "/senate.php"))
                    .GET()
                    .header("User-Agent", "Java HttpClient")
                    .build();

            System.out.println("Fetching senate page...");
            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            String html = resp.body();

            // Each senator is in an li block containing name and details
            Pattern senatorPat = Pattern.compile("<li>\\s*<a[^>]+>\\s*<img[^>]+>\\s*<strong class=\"name\">([^<]+)</strong>\\s*</a>\\s*<dl>(.*?)</dl>\\s*</li>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Pattern detailPat = Pattern.compile("<dt>([^<]+)</dt>\\s*<dd>([^<]+)</dd>", Pattern.CASE_INSENSITIVE);
            Pattern urlPat = Pattern.compile("<li>\\s*<a href=\"([^\"]+)\"[^>]*><img[^>]+><strong class=\"name\"", Pattern.CASE_INSENSITIVE);

            JSONArray out = new JSONArray();

            Matcher sm = senatorPat.matcher(html);
            while (sm.find()) {
                String name = sm.group(1).trim();
                String details = sm.group(2);

                // Get URL
                String profile = "";
                String searchFrom = html.substring(Math.max(0, sm.start() - 200), sm.start());
                Matcher um = urlPat.matcher(searchFrom);
                if (um.find()) {
                    profile = um.group(1);
                    if (profile.startsWith("http")) profile = profile;
                    else if (profile.startsWith("//")) profile = "https:" + profile;
                    else if (profile.startsWith("/")) profile = base + profile;
                    else profile = base + "/" + profile;
                }

                JSONObject obj = new JSONObject();
                obj.put("name", name);
                obj.put("title", "Senator");
                obj.put("profile", profile);
                obj.put("url", profile);
                obj.put("dob", "");
                obj.put("type", "");
                obj.put("country", "Alaska");

                // Parse details
                StringBuilder other = new StringBuilder();
                String party = "", phone = "", city = "", district = "";
                Matcher dm = detailPat.matcher(details);
                while (dm.find()) {
                    String label = dm.group(1).trim().toLowerCase();
                    String value = dm.group(2).trim();
                    switch (label) {
                        case "party:":
                            party = value;
                            break;
                        case "phone:":
                            if (phone.isEmpty()) phone = value;
                            break;
                        case "toll-free:":
                            if (phone.isEmpty()) phone = value;
                            break;
                        case "city:":
                            city = value;
                            break;
                        case "district:":
                            district = "District " + value;
                            break;
                    }
                }

                obj.put("party", party);

                // Build otherinfo
                if (!city.isEmpty()) other.append("City: ").append(city);
                if (!district.isEmpty()) {
                    if (other.length() > 0) other.append(" | ");
                    other.append("District: ").append(district);
                }
                if (!phone.isEmpty()) {
                    if (other.length() > 0) other.append(" | ");
                    other.append("Phone: ").append(phone);
                }
                obj.put("otherinfo", other.toString());

                out.put(obj);
                System.out.println("  scraped: " + name);
            }

            System.out.println("Finished. Writing JSON...");
            try (FileWriter fw = new FileWriter("alaska_senate.json", StandardCharsets.UTF_8)) {
                fw.write(out.toString(4));
            }

            System.out.println("Wrote alaska_senate.json");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
