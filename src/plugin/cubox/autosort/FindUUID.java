package plugin.cubox.autosort;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;
import java.util.concurrent.Callable;

class FindUUID implements Callable<UUID> {

    private final String name;

    private FindUUID(String name) {
        this.name = name;
    }

    @SuppressWarnings("deprecation")
    public static UUID getUUIDFromPlayerName(String name) {
        try {
            Player player = Util.getPlayer(name);
            if (AutoSort.ONLINE_UUID_CHECK) {
                return player != null ? player.getUniqueId() : new FindUUID(name).call();
            } else {
                return player != null ? player.getUniqueId() : Bukkit.getOfflinePlayer(name).getUniqueId();
            }
        } catch (Exception e) {
            if (AutoSort.getDebug() == 3)
                e.printStackTrace();
            return null;
        }
    }

    public UUID call() throws Exception {
        JSONParser jsonParser = new JSONParser();
        URL url = new URL(AutoSort.PROFILE_URL);
        HttpURLConnection httpConnection = (HttpURLConnection) url.openConnection();
        httpConnection.setConnectTimeout(AutoSort.httpConnectTimeout);
        httpConnection.setReadTimeout(AutoSort.httpReadTimeout);
        httpConnection.setRequestMethod("POST");
        httpConnection.setRequestProperty("Content-Type", "application/json");
        httpConnection.setUseCaches(false);
        httpConnection.setDoInput(true);
        httpConnection.setDoOutput(true);
        String jsonString = "[\"" + name + "\"]";
        OutputStream outputStream = httpConnection.getOutputStream();
        outputStream.write(jsonString.getBytes());
        outputStream.flush();
        outputStream.close();
        JSONArray jsonArray = (JSONArray) jsonParser.parse(new InputStreamReader(httpConnection.getInputStream()));
        JSONObject userProfile = (JSONObject) jsonArray.get(0);
        String id = (String) userProfile.get("id");
        UUID uuid = UUID.fromString(id.substring(0, 8) + "-" + id.substring(8, 12) + "-" + id.substring(12, 16) + "-" + id.substring(16, 20) + "-" + id.substring(20, 32));
        Thread.sleep(100L);
        return uuid;
    }
}
