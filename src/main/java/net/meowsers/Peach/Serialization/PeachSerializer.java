package net.meowsers.Peach.Serialization;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import net.meowsers.Peach.ECS.Component;
import net.meowsers.Peach.ECS.Components.TransformComponent;
import net.meowsers.Peach.ECS.GameObject;
import net.meowsers.Peach.GameEngine.PeachLevel;
import net.meowsers.Peach.Graphics.Mesh;
import net.meowsers.Peach.Graphics.Model;
import net.meowsers.Peach.Graphics.Texture;
import net.meowsers.Peach.Utils.PeachException;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PeachSerializer {

    private static final Gson gson = new GsonBuilder()
            .registerTypeAdapter(Component.class, new ComponentAdapter())
            .registerTypeAdapter(Model.class, new ModelAdapter())
            .registerTypeAdapter(Texture.class, new TextureAdapter())
            .setPrettyPrinting()
            .create();

    public static Gson getGson() {
        return gson;
    }

    public static void saveHashMap(String filePath, Map<?, ?> map) {
        String jsonStr = gson.toJson(map != null ? map : new HashMap<>());
        writeToFile(filePath, jsonStr);
    }
    public static <K, V> Map<K, V> loadHashMap(String filePath, Class<K> keyClass, Class<V> valueClass) {
        try {
            String content = Files.readString(Paths.get(filePath));
            if (content == null || content.isBlank()) return new HashMap<>();
            Type mapType = TypeToken.getParameterized(Map.class, keyClass, valueClass).getType();
            Map<K, V> result = gson.fromJson(content, mapType);
            return result != null ? result : new HashMap<>();
        } catch (IOException e) {
            throw new PeachException("Failed to read file: " + filePath, e);
        }
    }

    public static void saveGameObjects(String filePath, GameObject... gameObjects) {
        saveGameObjects(filePath, Arrays.asList(gameObjects));
    }
    public static void saveGameObjects(String filePath, List<GameObject> gameObjects) {
        String jsonStr = serialize(gameObjects);
        writeToFile(filePath, jsonStr);
    }
    public static void saveGameObject(String filePath, GameObject gameObject) {
        String jsonStr = serialize(gameObject);
        writeToFile(filePath, jsonStr);
    }

    public static void saveLevel(String filePath, PeachLevel level) {
        if (level == null) return;
        List<GameObject> gos = new ArrayList<>(level.getGameObjects());
        saveGameObjects(filePath, gos);
    }

    public static String serialize(GameObject... gameObjects) {
        return serialize(Arrays.asList(gameObjects));
    }
    public static String serialize(List<GameObject> gameObjects) {
        return gson.toJson(gameObjects != null ? gameObjects : List.of());
    }
    public static String serialize(GameObject gameObject) {
        return gson.toJson(gameObject);
    }
    public static String serialize(PeachLevel level) {
        return serialize(level != null ? level.getGameObjects() : List.of());
    }

    public static List<GameObject> loadGameObjects(String filePath) {
        try {
            String content = Files.readString(Paths.get(filePath));
            return deserializeGameObjects(content);
        } catch (IOException e) {
            throw new PeachException("Failed to read file: " + filePath, e);
        }
    }
    public static GameObject loadGameObject(String filePath) {
        try {
            String content = Files.readString(Paths.get(filePath));
            return deserializeGameObject(content);
        } catch (IOException e) {
            throw new PeachException("Failed to read file: " + filePath, e);
        }
    }

    public static List<GameObject> loadLevel(String filePath, PeachLevel level) {
        List<GameObject> gameObjects = loadGameObjects(filePath);
        if (level != null) {
            level.clearGameObjects();
            if (gameObjects != null) {
                for (GameObject go : gameObjects) {
                    level.addGameObject(go);
                }
            }
        }
        return gameObjects;
    }
    public static PeachLevel loadLevel(String filePath) {
        PeachLevel level = new PeachLevel() {};
        loadLevel(filePath, level);
        return level;
    }

    public static List<GameObject> deserializeGameObjects(String json) {
        if (json == null || json.isBlank()) return new ArrayList<>();
        String trimmed = json.trim();
        if (trimmed.startsWith("[")) {
            Type listType = new TypeToken<List<GameObject>>() {}.getType();
            List<GameObject> list = gson.fromJson(trimmed, listType);
            if (list == null) return new ArrayList<>();
            for (GameObject go : list) {
                postProcess(go);
            }
            return list;
        } else {
            GameObject single = deserializeGameObject(trimmed);
            List<GameObject> list = new ArrayList<>();
            if (single != null) {
                list.add(single);
            }
            return list;
        }
    }
    public static GameObject deserializeGameObject(String json) {
        if (json == null || json.isBlank()) return null;
        GameObject go = gson.fromJson(json, GameObject.class);
        return postProcess(go);
    }
    public static List<GameObject> deserializeLevel(String json, PeachLevel level) {
        List<GameObject> gameObjects = deserializeGameObjects(json);
        if (level != null) {
            level.clearGameObjects();
            if (gameObjects != null) {
                for (GameObject go : gameObjects) {
                    level.addGameObject(go);
                }
            }
        }
        return gameObjects;
    }

    public static GameObject postProcess(GameObject go) {
        if (go == null) return null;
        TransformComponent transform = go.getComponent(TransformComponent.class);
        if (transform == null) {
            transform = new TransformComponent();
            go.addComponent(transform);
        }
        go.transform = transform;
        if (go.getComponents() != null) {
            for (Component c : go.getComponents()) {
                if (c != null) {
                    c.setGameObject(go);
                }
            }
        }
        return go;
    }

    private static void writeToFile(String filePath, String data) {
        try {
            Path path = Paths.get(filePath);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, data);
        } catch (IOException e) {
            throw new PeachException("Failed to write file: " + filePath, e);
        }
    }

    private static class ComponentAdapter implements JsonSerializer<Component>, JsonDeserializer<Component> {
        @Override
        public JsonElement serialize(Component src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject jsonObject = context.serialize(src, src.getClass()).getAsJsonObject();
            jsonObject.addProperty("type", src.getClass().getName());
            return jsonObject;
        }

        @Override
        public Component deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (!json.isJsonObject()) return null;
            JsonObject jsonObject = json.getAsJsonObject();
            JsonElement typeElement = jsonObject.get("type");
            if (typeElement == null || !typeElement.isJsonPrimitive()) {
                return null;
            }
            String typeName = typeElement.getAsString();
            Class<?> clazz = resolveClass(typeName);
            if (clazz == null || !Component.class.isAssignableFrom(clazz)) {
                return null;
            }
            return context.deserialize(jsonObject, clazz);
        }

        private Class<?> resolveClass(String typeName) {
            try {
                return Class.forName(typeName);
            } catch (ClassNotFoundException ignored) {
            }
            try {
                return Class.forName("net.meowsers.Peach.ECS.Components." + typeName);
            } catch (ClassNotFoundException ignored) {
            }
            try {
                return Class.forName("net.meowsers.Peach.ECS." + typeName);
            } catch (ClassNotFoundException ignored) {
            }
            return null;
        }
    }
    private static class ModelAdapter implements JsonSerializer<Model>, JsonDeserializer<Model> {
        @Override
        public JsonElement serialize(Model src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) return JsonNull.INSTANCE;
            JsonObject obj = new JsonObject();
            if (src.getFilepath() != null && !src.getFilepath().isBlank()) {
                obj.addProperty("filepath", src.getFilepath());
            }
            if (src.getName() != null) {
                obj.addProperty("name", src.getName());
            }
            if (src.getPosition() != null) {
                obj.add("position", context.serialize(src.getPosition()));
            }
            if (src.getRotation() != null) {
                obj.add("rotation", context.serialize(src.getRotation()));
            }
            if (src.getScale() != null) {
                obj.add("scale", context.serialize(src.getScale()));
            }
            if (src.getTransform() != null) {
                obj.add("transform", context.serialize(src.getTransform()));
            }
            if ((src.getFilepath() == null || src.getFilepath().isBlank()) && src.getMeshes() != null && !src.getMeshes().isEmpty()) {
                obj.add("meshes", context.serialize(src.getMeshes()));
            }
            if (src.getTextures() != null && !src.getTextures().isEmpty()) {
                obj.add("textures", context.serialize(src.getTextures()));
            }
            return obj;
        }

        @Override
        public Model deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || !json.isJsonObject()) return null;
            JsonObject obj = json.getAsJsonObject();
            Model model;
            if (obj.has("filepath") && !obj.get("filepath").isJsonNull()) {
                String filepath = obj.get("filepath").getAsString();
                if (filepath != null && !filepath.isBlank()) {
                    try {
                        model = new Model(filepath);
                    } catch (Exception e) {
                        model = new Model();
                        model.setFilepath(filepath);
                    }
                } else {
                    model = new Model();
                }
            } else {
                model = new Model();
                if (obj.has("meshes")) {
                    Type listType = new TypeToken<List<Mesh>>() {}.getType();
                    List<Mesh> meshes = context.deserialize(obj.get("meshes"), listType);
                    if (meshes != null) {
                        for (Mesh m : meshes) {
                            model.addMesh(m);
                        }
                    }
                }
            }

            if (obj.has("name") && !obj.get("name").isJsonNull()) {
                model.setName(obj.get("name").getAsString());
            }
            if (obj.has("position") && !obj.get("position").isJsonNull()) {
                Vector3f pos = context.deserialize(obj.get("position"), Vector3f.class);
                if (pos != null) model.setPosition(pos);
            }
            if (obj.has("rotation") && !obj.get("rotation").isJsonNull()) {
                Vector3f rot = context.deserialize(obj.get("rotation"), Vector3f.class);
                if (rot != null) model.setRotation(rot);
            }
            if (obj.has("scale") && !obj.get("scale").isJsonNull()) {
                Vector3f sca = context.deserialize(obj.get("scale"), Vector3f.class);
                if (sca != null) model.setScale(sca);
            }
            if (obj.has("transform") && !obj.get("transform").isJsonNull()) {
                Matrix4f trans = context.deserialize(obj.get("transform"), Matrix4f.class);
                if (trans != null) model.setTransform(trans);
            }
            if (obj.has("textures") && !obj.get("textures").isJsonNull()) {
                Type texListType = new TypeToken<List<Texture>>() {}.getType();
                List<Texture> textures = context.deserialize(obj.get("textures"), texListType);
                if (textures != null) {
                    for (Texture t : textures) {
                        if (t != null) model.addTexture(t);
                    }
                }
            }
            return model;
        }
    }
    private static class TextureAdapter implements JsonSerializer<Texture>, JsonDeserializer<Texture> {
        @Override
        public JsonElement serialize(Texture src, Type typeOfSrc, JsonSerializationContext context) {
            if (src == null) return JsonNull.INSTANCE;
            JsonObject obj = new JsonObject();
            if (src.getFilepath() != null && !src.getFilepath().isBlank()) {
                obj.addProperty("filepath", src.getFilepath());
            }
            return obj;
        }

        @Override
        public Texture deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            if (json == null || !json.isJsonObject()) return null;
            JsonObject obj = json.getAsJsonObject();
            if (obj.has("filepath") && !obj.get("filepath").isJsonNull()) {
                String filepath = obj.get("filepath").getAsString();
                if (filepath != null && !filepath.isBlank()) {
                    try {
                        return new Texture(filepath);
                    } catch (Exception e) {
                        return null;
                    }
                }
            }
            return null;
        }
    }
}