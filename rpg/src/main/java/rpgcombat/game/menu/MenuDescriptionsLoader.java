package rpgcombat.game.menu;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class MenuDescriptionsLoader {
    private MenuDescriptionsLoader() {}

    private static final Gson GSON = new Gson();

    public static Map<String, String> load(Path path) throws IOException {
        Type type = new TypeToken<Map<String, String>>() {}.getType();
        
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            Map<String, String> information = GSON.fromJson(reader, type);
            return information != null ? information : Collections.emptyMap();
        }
    }
}
