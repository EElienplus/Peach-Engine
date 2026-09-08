package net.meowsers.Peach.GUI;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.flag.ImGuiConfigFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import imgui.type.ImString;
import net.meowsers.Peach.ECS.Component;
import net.meowsers.Peach.ECS.GameObject;
import net.meowsers.Peach.GameEngine.Peach;
import net.meowsers.Peach.GameEngine.PeachLevel;
import net.meowsers.Peach.Structures.Color;
import net.meowsers.Peach.Utils.PeachException;
import net.meowsers.Peach.Utils.Time;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.joml.Vector4i;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class PeachGui {

    private static final ImGuiImplGlfw imGuiGlfw = new ImGuiImplGlfw();
    private static final ImGuiImplGl3 imGuiGl3 = new ImGuiImplGl3();
    private static boolean initialized = false;

    private static final List<Runnable> frameCallbacks = new ArrayList<>();

    private static final Map<Class<?>, Field[]> editorFieldsCache = new ConcurrentHashMap<>();

    private static class EnumCache {
        final Object[] constants;
        final String[] names;

        EnumCache(Class<?> enumClass) {
            this.constants = enumClass.getEnumConstants();
            this.names = new String[this.constants != null ? this.constants.length : 0];
            if (this.constants != null) {
                for (int i = 0; i < this.constants.length; i++) {
                    this.names[i] = ((Enum<?>) this.constants[i]).name();
                }
            }
        }
    }

    private static final Map<Class<?>, EnumCache> enumCache = new ConcurrentHashMap<>();

    private static final float[] floatBuffer = new float[4];
    private static final int[] intBuffer = new int[4];
    private static final ImInt imIntBuffer = new ImInt();
    private static GameObject selectedGameObject = null;


    public static void init(long windowHandle) {
        if (initialized || windowHandle == 0) return;

        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);

        imGuiGlfw.init(windowHandle, true);
        imGuiGl3.init("#version 410 core");

        ImGui.styleColorsDark();
        initialized = true;
    }

    public static void start(long windowHandle) {
        init(windowHandle);
    }

    /**
     * Starts a new ImGui frame and executes registered panel callbacks.
     */
    public static void newFrame() {
        if (!initialized) return;

        imGuiGlfw.newFrame();
        imGuiGl3.newFrame();
        ImGui.newFrame();

        for (int i = 0; i < frameCallbacks.size(); i++) {
            Runnable callback = frameCallbacks.get(i);
            if (callback != null) {
                callback.run();
            }
        }
    }

    public static void update() {
        newFrame();
    }

    public static void render() {
        if (!initialized) return;

        ImGui.render();
        imGuiGl3.renderDrawData(ImGui.getDrawData());
    }

    public static void destroy() {
        if (!initialized) return;

        selectedGameObject = null;
        frameCallbacks.clear();
        imGuiGl3.shutdown();
        imGuiGlfw.shutdown();
        ImGui.destroyContext();
        initialized = false;
    }

    public static boolean isInitialized() {
        return initialized;
    }


    public static boolean wantCaptureMouse() {
        return initialized && ImGui.getIO().getWantCaptureMouse();
    }

    public static boolean wantCaptureKeyboard() {
        return initialized && ImGui.getIO().getWantCaptureKeyboard();
    }


    public static void registerPanel(Runnable callback) {
        if (callback != null && !frameCallbacks.contains(callback)) {
            frameCallbacks.add(callback);
        }
    }

    public static void unregisterPanel(Runnable callback) {
        frameCallbacks.remove(callback);
    }

    public static void window(String title, Runnable content) {
        ImGui.begin(title);
        if (content != null) content.run();
        ImGui.end();
    }

    public static void window(String title, ImBoolean open, Runnable content) {
        ImGui.begin(title, open);
        if (content != null) content.run();
        ImGui.end();
    }

    public static void text(String format, Object... args) {
        if (args == null || args.length == 0) {
            ImGui.text(format);
        } else {
            ImGui.text(String.format(format, args));
        }
    }
    public static void textColored(Color color, String format, Object... args) {
        String msg = (args == null || args.length == 0) ? format : String.format(format, args);
        if (color != null) {
            ImGui.textColored(color.r, color.g, color.b, color.a, msg);
        } else {
            ImGui.text(msg);
        }
    }

    public static boolean button(String label) {
        return ImGui.button(label);
    }
    public static boolean button(String label, Runnable onClick) {
        boolean clicked = ImGui.button(label);
        if (clicked && onClick != null) {
            onClick.run();
        }
        return clicked;
    }

    public static boolean checkbox(String label, ImBoolean state) {
        return ImGui.checkbox(label, state);
    }
    public static boolean checkbox(String label, boolean[] state) {
        if (state == null || state.length == 0) return false;
        ImBoolean b = new ImBoolean(state[0]);
        boolean changed = ImGui.checkbox(label, b);
        if (changed) {
            state[0] = b.get();
        }
        return changed;
    }

    public static boolean dropdown(String label, ImInt currentItem, List<String> items) {
        if (currentItem == null || items == null) return false;
        return ImGui.combo(label, currentItem, items.toArray(new String[0]));
    }
    public static boolean dropdown(String label, int[] currentItem, List<String> items) {
        if (currentItem == null || currentItem.length == 0 || items == null) return false;
        ImInt val = new ImInt(currentItem[0]);
        boolean changed = ImGui.combo(label, val, items.toArray(new String[0]));
        if (changed) {
            currentItem[0] = val.get();
        }
        return changed;
    }

    public static boolean selectable(String label, boolean selected) {
        return ImGui.selectable(label, selected);
    }
    public static boolean selectable(String label, boolean selected, Runnable onClick) {
        boolean clicked = ImGui.selectable(label, selected);
        if (clicked && onClick != null) {
            onClick.run();
        }
        return clicked;
    }
    public static boolean selectable(String label, ImBoolean selected) {
        if (selected == null) return false;
        return ImGui.selectable(label, selected);
    }

    public static boolean dragFloat2(String label, Vector2f vec, float speed) {
        if (vec == null) return false;
        float[] arr = new float[]{vec.x, vec.y};
        boolean changed = ImGui.dragFloat2(label, arr, speed);
        if (changed) {
            vec.set(arr[0], arr[1]);
        }
        return changed;
    }
    public static boolean dragFloat2(String label, Vector2f vec) {
        return dragFloat2(label, vec, 0.1f);
    }

    public static boolean dragFloat3(String label, Vector3f vec, float speed) {
        if (vec == null) return false;
        float[] arr = new float[]{vec.x, vec.y, vec.z};
        boolean changed = ImGui.dragFloat3(label, arr, speed);
        if (changed) {
            vec.set(arr[0], arr[1], arr[2]);
        }
        return changed;
    }
    public static boolean dragFloat3(String label, Vector3f vec) {
        return dragFloat3(label, vec, 0.1f);
    }

    public static boolean dragFloat4(String label, Vector4f vec, float speed) {
        if (vec == null) return false;
        float[] arr = new float[]{vec.x, vec.y, vec.z, vec.w};
        boolean changed = ImGui.dragFloat4(label, arr, speed);
        if (changed) {
            vec.set(arr[0], arr[1], arr[2], arr[3]);
        }
        return changed;
    }
    public static boolean dragFloat4(String label, Vector4f vec) {
        return dragFloat4(label, vec, 0.1f);
    }

    public static boolean colorEdit3(String label, Color color) {
        if (color == null) return false;
        float[] arr = new float[]{color.r, color.g, color.b};
        boolean changed = ImGui.colorEdit3(label, arr);
        if (changed) {
            color.r = arr[0];
            color.g = arr[1];
            color.b = arr[2];
        }
        return changed;
    }

    public static boolean colorEdit4(String label, Color color) {
        if (color == null) return false;
        float[] arr = new float[]{color.r, color.g, color.b, color.a};
        boolean changed = ImGui.colorEdit4(label, arr);
        if (changed) {
            color.r = arr[0];
            color.g = arr[1];
            color.b = arr[2];
            color.a = arr[3];
        }
        return changed;
    }

    public static boolean colorEdit(String label, Color color) {
        return colorEdit4(label, color);
    }

    public static float dragFloat(String label, float val) {
        float[] arr = new float[]{val};
        ImGui.dragFloat(label, arr);
        return arr[0];
    }

    public static void separator() {
        ImGui.separator();
    }

    public static void sameLine() {
        ImGui.sameLine();
    }

    public static void spacing() {
        ImGui.spacing();
    }

    public static Field[] getEditorFields(Class<?> clazz) {
        return editorFieldsCache.computeIfAbsent(clazz, c -> {
            List<Field> fields = new ArrayList<>();
            Class<?> current = c;
            while (current != null && current != Object.class) {
                for (Field field : current.getDeclaredFields()) {
                    if (field.isAnnotationPresent(Editor.class)) {
                        field.setAccessible(true);
                        fields.add(field);
                    }
                }
                current = current.getSuperclass();
            }
            return fields.toArray(new Field[0]);
        });
    }

    public static void drawFieldEditor(Field field, Object instance) throws IllegalAccessException {
        String label = field.getName();
        Class<?> type = field.getType();

        ImGui.pushID(label);

        text(label + ":");
        sameLine();

        ImGui.setNextItemWidth(180.0f);

        if (type == float.class) {
            floatBuffer[0] = field.getFloat(instance);
            if (ImGui.dragFloat("##val", floatBuffer, 0.1f)) {
                field.setFloat(instance, floatBuffer[0]);
            }
        }
        else if (type == Float.class) {
            Object obj = field.get(instance);
            floatBuffer[0] = obj != null ? (Float) obj : 0.0f;
            if (ImGui.dragFloat("##val", floatBuffer, 0.1f)) {
                field.set(instance, floatBuffer[0]);
            }
        }
        else if (type == double.class) {
            floatBuffer[0] = (float) field.getDouble(instance);
            if (ImGui.dragFloat("##val", floatBuffer, 0.1f)) {
                field.setDouble(instance, (double) floatBuffer[0]);
            }
        }
        else if (type == Double.class) {
            Object obj = field.get(instance);
            floatBuffer[0] = obj != null ? ((Double) obj).floatValue() : 0.0f;
            if (ImGui.dragFloat("##val", floatBuffer, 0.1f)) {
                field.set(instance, (double) floatBuffer[0]);
            }
        }
        else if (type == int.class) {
            intBuffer[0] = field.getInt(instance);
            if (ImGui.dragInt("##val", intBuffer)) {
                field.setInt(instance, intBuffer[0]);
            }
        }
        else if (type == Integer.class) {
            Object obj = field.get(instance);
            intBuffer[0] = obj != null ? (Integer) obj : 0;
            if (ImGui.dragInt("##val", intBuffer)) {
                field.set(instance, intBuffer[0]);
            }
        }
        else if (type == long.class) {
            intBuffer[0] = (int) field.getLong(instance);
            if (ImGui.dragInt("##val", intBuffer)) {
                field.setLong(instance, (long) intBuffer[0]);
            }
        }
        else if (type == Long.class) {
            Object obj = field.get(instance);
            intBuffer[0] = obj != null ? ((Long) obj).intValue() : 0;
            if (ImGui.dragInt("##val", intBuffer)) {
                field.set(instance, (long) intBuffer[0]);
            }
        }
        else if (type == short.class) {
            intBuffer[0] = field.getShort(instance);
            if (ImGui.dragInt("##val", intBuffer)) {
                field.setShort(instance, (short) intBuffer[0]);
            }
        }
        else if (type == Short.class) {
            Object obj = field.get(instance);
            intBuffer[0] = obj != null ? (Short) obj : 0;
            if (ImGui.dragInt("##val", intBuffer)) {
                field.set(instance, (short) intBuffer[0]);
            }
        }
        else if (type == byte.class) {
            intBuffer[0] = field.getByte(instance);
            if (ImGui.dragInt("##val", intBuffer)) {
                field.setByte(instance, (byte) intBuffer[0]);
            }
        }
        else if (type == Byte.class) {
            Object obj = field.get(instance);
            intBuffer[0] = obj != null ? (Byte) obj : 0;
            if (ImGui.dragInt("##val", intBuffer)) {
                field.set(instance, (byte) intBuffer[0]);
            }
        }
        else if (type == boolean.class) {
            boolean val = field.getBoolean(instance);
            if (ImGui.checkbox("##val", val)) {
                field.setBoolean(instance, !val);
            }
        }
        else if (type == Boolean.class) {
            Object obj = field.get(instance);
            boolean val = obj != null && (Boolean) obj;
            if (ImGui.checkbox("##val", val)) {
                field.set(instance, !val);
            }
        }
        else if (type == String.class) {
            Object strVal = field.get(instance);
            ImString val = new ImString(strVal != null ? (String) strVal : "", 256);
            if (ImGui.inputText("##val", val)) {
                field.set(instance, val.get());
            }
        }
        else if (type == Color.class) {
            Color color = (Color) field.get(instance);
            if (color == null) {
                color = new Color(1.0f, 1.0f, 1.0f, 1.0f);
                field.set(instance, color);
            }
            floatBuffer[0] = color.r;
            floatBuffer[1] = color.g;
            floatBuffer[2] = color.b;
            floatBuffer[3] = color.a;
            if (ImGui.colorEdit4("##val", floatBuffer)) {
                Color updated = new Color(floatBuffer[0], floatBuffer[1], floatBuffer[2], floatBuffer[3]);
                field.set(instance, updated);
            }
        }
        else if (type == Vector2f.class) {
            Vector2f vec = (Vector2f) field.get(instance);
            if (vec == null) {
                vec = new Vector2f();
                field.set(instance, vec);
            }
            floatBuffer[0] = vec.x;
            floatBuffer[1] = vec.y;
            if (ImGui.dragFloat2("##val", floatBuffer, 0.1f)) {
                vec.set(floatBuffer[0], floatBuffer[1]);
            }
        }
        else if (type == Vector3f.class) {
            Vector3f vec = (Vector3f) field.get(instance);
            if (vec == null) {
                vec = new Vector3f();
                field.set(instance, vec);
            }
            floatBuffer[0] = vec.x;
            floatBuffer[1] = vec.y;
            floatBuffer[2] = vec.z;
            if (ImGui.dragFloat3("##val", floatBuffer, 0.1f)) {
                vec.set(floatBuffer[0], floatBuffer[1], floatBuffer[2]);
            }
        }
        else if (type == Vector4f.class) {
            Vector4f vec = (Vector4f) field.get(instance);
            if (vec == null) {
                vec = new Vector4f();
                field.set(instance, vec);
            }
            floatBuffer[0] = vec.x;
            floatBuffer[1] = vec.y;
            floatBuffer[2] = vec.z;
            floatBuffer[3] = vec.w;
            if (ImGui.dragFloat4("##val", floatBuffer, 0.1f)) {
                vec.set(floatBuffer[0], floatBuffer[1], floatBuffer[2], floatBuffer[3]);
            }
        }
        else if (type == Vector2i.class) {
            Vector2i vec = (Vector2i) field.get(instance);
            if (vec == null) {
                vec = new Vector2i();
                field.set(instance, vec);
            }
            intBuffer[0] = vec.x;
            intBuffer[1] = vec.y;
            if (ImGui.dragInt2("##val", intBuffer)) {
                vec.set(intBuffer[0], intBuffer[1]);
            }
        }
        else if (type == Vector3i.class) {
            Vector3i vec = (Vector3i) field.get(instance);
            if (vec == null) {
                vec = new Vector3i();
                field.set(instance, vec);
            }
            intBuffer[0] = vec.x;
            intBuffer[1] = vec.y;
            intBuffer[2] = vec.z;
            if (ImGui.dragInt3("##val", intBuffer)) {
                vec.set(intBuffer[0], intBuffer[1], intBuffer[2]);
            }
        }
        else if (type == Vector4i.class) {
            Vector4i vec = (Vector4i) field.get(instance);
            if (vec == null) {
                vec = new Vector4i();
                field.set(instance, vec);
            }
            intBuffer[0] = vec.x;
            intBuffer[1] = vec.y;
            intBuffer[2] = vec.z;
            intBuffer[3] = vec.w;
            if (ImGui.dragInt4("##val", intBuffer)) {
                vec.set(intBuffer[0], intBuffer[1], intBuffer[2], intBuffer[3]);
            }
        }
        else if (type.isEnum() || Enum.class.isAssignableFrom(type)) {
            @SuppressWarnings("unchecked")
            Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) (type.isEnum() ? type : type.getSuperclass());
            EnumCache cache = enumCache.computeIfAbsent(enumClass, EnumCache::new);
            if (cache.constants.length > 0) {
                Object currentVal = field.get(instance);
                int currentIndex = currentVal != null ? ((Enum<?>) currentVal).ordinal() : 0;
                if (currentIndex < 0 || currentIndex >= cache.constants.length) {
                    currentIndex = 0;
                }
                imIntBuffer.set(currentIndex);
                if (ImGui.combo("##val", imIntBuffer, cache.names)) {
                    int selected = imIntBuffer.get();
                    if (selected >= 0 && selected < cache.constants.length) {
                        field.set(instance, cache.constants[selected]);
                    }
                }
            }
        }

        ImGui.popID();
    }

    public static GameObject getSelectedGameObject() {
        return selectedGameObject;
    }

    public static void setSelectedGameObject(GameObject gameObject) {
        selectedGameObject = gameObject;
    }

    public static void hierarchy(PeachLevel level) {
        if (level == null) return;
        window("Hierarchy", () -> {
            List<GameObject> gameObjects = level.getGameObjects();
            if (gameObjects == null || gameObjects.isEmpty()) {
                ImGui.textDisabled("No GameObjects in level");
                return;
            }

            for (GameObject gameObject : gameObjects) {
                if (gameObject == null) continue;
                ImGui.pushID(gameObject.hashCode());

                boolean isSelected = (gameObject == selectedGameObject);
                String displayName = gameObject.name != null && !gameObject.name.isEmpty()
                        ? gameObject.name
                        : "GameObject";

                if (ImGui.selectable(displayName, isSelected)) {
                    selectedGameObject = gameObject;
                }

                ImGui.popID();
            }
        });
    }

    public static void inspector(GameObject gameObject) {
        window("Inspector", () -> {
            if (gameObject == null) {
                ImGui.textDisabled("No GameObject selected");
                return;
            }

            ImGui.pushID(gameObject.hashCode());

            ImGui.textColored(0.4f, 0.8f, 1.0f, 1.0f, "GameObject:");
            ImGui.sameLine();
            ImString nameStr = new ImString(gameObject.name != null ? gameObject.name : "", 128);
            ImGui.setNextItemWidth(180.0f);
            if (ImGui.inputText("##GOName", nameStr)) {
                gameObject.name = nameStr.get();
            }

            boolean active = gameObject.isActive();
            if (ImGui.checkbox("Active", active)) {
                gameObject.setActive(!active);
            }

            ImGui.separator();
            ImGui.textColored(1.0f, 1.0f, 1.0f, 1.0f, "Components:");
            ImGui.spacing();

            List<Component> components = gameObject.getComponents();
            if (components != null) {
                for (Component component : components) {
                    if (component == null) continue;
                    ImGui.pushID(component.hashCode());

                    ImGui.textColored(0.9f, 0.9f, 0.4f, 1.0f, "• " + component.getName());

                    Field[] fields = getEditorFields(component.getClass());
                    if (fields.length == 0) {
                        ImGui.textDisabled("  No editable fields");
                    } else {
                        for (Field field : fields) {
                            try {
                                drawFieldEditor(field, component);
                            } catch (IllegalAccessException e) {
                                throw new PeachException(e);
                            }
                        }
                    }

                    ImGui.spacing();
                    ImGui.separator();
                    ImGui.popID();
                }
            }

            ImGui.popID();
        });
    }

    public static void inspector() {
        inspector(selectedGameObject);
    }

    public static void inspector(PeachLevel level) {
        inspector(selectedGameObject);
    }


    public static void showStats() {
        window("Engine Stats", () -> {
            text("FPS: %.1f", Time.getFPS());
            text("Frame Time: %.2f ms", Time.getDeltaTime() * 1000.0f);
            long totalMem = Runtime.getRuntime().totalMemory() / (1024 * 1024);
            long freeMem = Runtime.getRuntime().freeMemory() / (1024 * 1024);
            long usedMem = totalMem - freeMem;
            text("Memory: %d MB / %d MB", usedMem, totalMem);
        });
    }
}