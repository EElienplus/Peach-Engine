package net.meowsers.Peach.GUI;

import imgui.ImGui;
import imgui.ImGuiIO;
import imgui.extension.imguizmo.ImGuizmo;
import imgui.extension.imguizmo.flag.Mode;
import imgui.extension.imguizmo.flag.Operation;
import imgui.flag.ImGuiConfigFlags;
import imgui.flag.ImGuiSelectableFlags;
import imgui.gl3.ImGuiImplGl3;
import imgui.glfw.ImGuiImplGlfw;
import imgui.type.ImBoolean;
import imgui.type.ImFloat;
import imgui.type.ImInt;
import imgui.type.ImString;
import net.meowsers.Peach.ECS.Component;
import net.meowsers.Peach.ECS.Components.*;
import net.meowsers.Peach.ECS.GameObject;
import net.meowsers.Peach.GameEngine.Peach;
import net.meowsers.Peach.GameEngine.PeachLevel;
import net.meowsers.Peach.Graphics.Camera;
import net.meowsers.Peach.Graphics.Renderer;
import net.meowsers.Peach.Structures.Color;
import net.meowsers.Peach.Structures.Components;
import net.meowsers.Peach.Structures.Key;
import net.meowsers.Peach.Structures.MouseButton;
import net.meowsers.Peach.Utils.Input;
import net.meowsers.Peach.Utils.Log;
import net.meowsers.Peach.Utils.PeachException;
import net.meowsers.Peach.Utils.Time;
import org.joml.Vector2f;
import org.joml.Vector2i;
import org.joml.Vector3f;
import org.joml.Vector3i;
import org.joml.Vector4f;
import org.joml.Vector4i;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
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

        EnumCache(Class<?> rawClass) {
            Class<?> enumClass = (rawClass != null && !rawClass.isEnum() && rawClass.getSuperclass() != null && rawClass.getSuperclass().isEnum())
                    ? rawClass.getSuperclass()
                    : rawClass;
            this.constants = (enumClass != null) ? enumClass.getEnumConstants() : null;
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
    private static final ImBoolean imBoolBuffer = new ImBoolean();
    private static final ImString imStringBuffer = new ImString(512);
    private static final ImString newGameObjectNameBuffer = new ImString(256);
    private static final ImString goNameBuffer = new ImString(256);
    private static GameObject selectedGameObject = null;

    // ImGuizmo state
    private static final float MIN_SCALE = 0.001f;
    private static int gizmoOperation = Operation.TRANSLATE;
    private static int gizmoMode = Mode.LOCAL;
    private static boolean gizmoSnap = true;
    private static float gizmoSnapValue = 1.0f;
    private static final float[] gizmoSnapValues = new float[]{1.0f, 1.0f, 1.0f};
    private static final float[] gizmoMatrixBuffer = new float[16];
    private static final float[] gizmoViewBuffer = new float[16];
    private static final float[] gizmoProjBuffer = new float[16];
    private static final float[] gizmoPosBuffer = new float[3];
    private static final float[] gizmoRotBuffer = new float[3];
    private static final float[] gizmoScaleBuffer = new float[3];


    public static void init(long windowHandle) {
        if (initialized || windowHandle == 0) return;

        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.addConfigFlags(ImGuiConfigFlags.NavEnableKeyboard);
        io.setConfigMacOSXBehaviors(System.getProperty("os.name", "").toLowerCase().contains("mac"));

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
        ImGuizmo.beginFrame();

        handleGizmoHotkeys();

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
        return initialized && (ImGui.getIO().getWantCaptureMouse() || ImGuizmo.isUsing() || ImGuizmo.isOver());
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
        imBoolBuffer.set(state[0]);
        boolean changed = ImGui.checkbox(label, imBoolBuffer);
        if (changed) {
            state[0] = imBoolBuffer.get();
        }
        return changed;
    }

    public static int dropdown(String label, int selectedIndex, List<String> items) {
        if (items == null || items.isEmpty()) return selectedIndex;
        int idx = (selectedIndex >= 0 && selectedIndex < items.size()) ? selectedIndex : 0;
        imIntBuffer.set(idx);
        if (ImGui.combo(label, imIntBuffer, items.toArray(new String[0]))) {
            return imIntBuffer.get();
        }
        return selectedIndex;
    }
    public static int dropdown(String label, int selectedIndex, String... items) {
        if (items == null || items.length == 0) return selectedIndex;
        int idx = (selectedIndex >= 0 && selectedIndex < items.length) ? selectedIndex : 0;
        imIntBuffer.set(idx);
        if (ImGui.combo(label, imIntBuffer, items)) {
            return imIntBuffer.get();
        }
        return selectedIndex;
    }

    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> E dropdown(String label, Class<E> enumClass, E selected) {
        if (enumClass == null) return selected;
        EnumCache cache = enumCache.computeIfAbsent(enumClass, EnumCache::new);
        if (cache.constants == null || cache.constants.length == 0) return selected;
        int currentIndex = selected != null ? selected.ordinal() : 0;
        if (currentIndex < 0 || currentIndex >= cache.constants.length) currentIndex = 0;
        imIntBuffer.set(currentIndex);
        if (ImGui.combo(label, imIntBuffer, cache.names)) {
            int newIdx = imIntBuffer.get();
            if (newIdx >= 0 && newIdx < cache.constants.length) {
                return (E) cache.constants[newIdx];
            }
        }
        return selected;
    }

    public static <E extends Enum<E>> E dropdown(String label, E selected) {
        if (selected == null) return null;
        return dropdown(label, selected.getDeclaringClass(), selected);
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
        floatBuffer[0] = vec.x;
        floatBuffer[1] = vec.y;
        boolean changed = ImGui.dragFloat2(label, floatBuffer, speed);
        if (changed) {
            vec.set(floatBuffer[0], floatBuffer[1]);
        }
        return changed;
    }
    public static boolean dragFloat2(String label, Vector2f vec) {
        return dragFloat2(label, vec, 0.1f);
    }
    public static boolean dragFloat3(String label, Vector3f vec, float speed) {
        if (vec == null) return false;
        floatBuffer[0] = vec.x;
        floatBuffer[1] = vec.y;
        floatBuffer[2] = vec.z;
        boolean changed = ImGui.dragFloat3(label, floatBuffer, speed);
        if (changed) {
            vec.set(floatBuffer[0], floatBuffer[1], floatBuffer[2]);
        }
        return changed;
    }
    public static boolean dragFloat3(String label, Vector3f vec) {
        return dragFloat3(label, vec, 0.1f);
    }
    public static boolean dragFloat4(String label, Vector4f vec, float speed) {
        if (vec == null) return false;
        floatBuffer[0] = vec.x;
        floatBuffer[1] = vec.y;
        floatBuffer[2] = vec.z;
        floatBuffer[3] = vec.w;
        boolean changed = ImGui.dragFloat4(label, floatBuffer, speed);
        if (changed) {
            vec.set(floatBuffer[0], floatBuffer[1], floatBuffer[2], floatBuffer[3]);
        }
        return changed;
    }
    public static boolean dragFloat4(String label, Vector4f vec) {
        return dragFloat4(label, vec, 0.1f);
    }
    public static boolean colorEdit3(String label, Color color) {
        if (color == null) return false;
        floatBuffer[0] = color.r;
        floatBuffer[1] = color.g;
        floatBuffer[2] = color.b;
        boolean changed = ImGui.colorEdit3(label, floatBuffer);
        if (changed) {
            color.r = floatBuffer[0];
            color.g = floatBuffer[1];
            color.b = floatBuffer[2];
        }
        return changed;
    }
    public static boolean colorEdit4(String label, Color color) {
        if (color == null) return false;
        floatBuffer[0] = color.r;
        floatBuffer[1] = color.g;
        floatBuffer[2] = color.b;
        floatBuffer[3] = color.a;
        boolean changed = ImGui.colorEdit4(label, floatBuffer);
        if (changed) {
            color.r = floatBuffer[0];
            color.g = floatBuffer[1];
            color.b = floatBuffer[2];
            color.a = floatBuffer[3];
        }
        return changed;
    }
    public static boolean colorEdit(String label, Color color) {
        return colorEdit4(label, color);
    }

    public static float dragFloat(String label, float val) {
        floatBuffer[0] = val;
        ImGui.dragFloat(label, floatBuffer);
        return floatBuffer[0];
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

        Editor editorAnnot = field.getAnnotation(Editor.class);
        boolean requirePositive = editorAnnot != null && "Positive".equalsIgnoreCase(editorAnnot.argument());
        boolean readOnly = editorAnnot != null && "Read-Only".equalsIgnoreCase(editorAnnot.argument());
        boolean isBooleanButton = editorAnnot != null && "Boolean-Button".equalsIgnoreCase(editorAnnot.argument());
        boolean isVolume = editorAnnot != null && "Volume".equalsIgnoreCase(editorAnnot.argument()) || label.equalsIgnoreCase("volume");

        // 1. Boolean-Button Special Case (Renders as a standalone action button)
        if (isBooleanButton && (type == boolean.class || type == Boolean.class)) {
            boolean val = type == boolean.class ? field.getBoolean(instance) : Boolean.TRUE.equals(field.get(instance));
            if (ImGui.button(label + "##val")) {
                val = !val;
                if (type == boolean.class) field.setBoolean(instance, val);
                else field.set(instance, val);
                invokeCallback(editorAnnot, instance);
            }
            ImGui.popID();
            return;
        }

        // Standard Inspector Row Layout Prefix
        text(label + ":");
        sameLine();
        ImGui.setNextItemWidth(180.0f);

        if (type == float.class || type == Float.class) {
            Object obj = field.get(instance);
            floatBuffer[0] = obj != null ? ((Number) obj).floatValue() : 0.0f;

            if (readOnly) {
                text(Float.toString(floatBuffer[0]));
            } else if (isVolume) {
                if (ImGui.sliderFloat("##val", floatBuffer, 0.0f, 1.0f)) {
                    setFieldFloat(field, instance, type, floatBuffer[0]);
                    invokeCallback(editorAnnot, instance);
                }
            } else {
                float vMin = requirePositive ? 0.0f : -Float.MAX_VALUE;
                if (ImGui.dragFloat("##val", floatBuffer, 0.1f, vMin, Float.MAX_VALUE)) {
                    if (requirePositive && floatBuffer[0] < 0.0f) floatBuffer[0] = 0.0f;
                    setFieldFloat(field, instance, type, floatBuffer[0]);
                    invokeCallback(editorAnnot, instance);
                }
            }
        }
        else if (type == double.class || type == Double.class) {
            Object obj = field.get(instance);
            floatBuffer[0] = obj != null ? ((Number) obj).floatValue() : 0.0f;

            if (readOnly) {
                text(Double.toString(obj != null ? (Double) obj : 0.0));
            } else if (ImGui.dragFloat("##val", floatBuffer, 0.1f)) {
                setFieldDouble(field, instance, type, (double) floatBuffer[0]);
                invokeCallback(editorAnnot, instance);
            }
        }
        else if (type == int.class || type == Integer.class ||
                type == long.class || type == Long.class ||
                type == short.class || type == Short.class ||
                type == byte.class || type == Byte.class) {
            Object obj = field.get(instance);
            intBuffer[0] = obj != null ? ((Number) obj).intValue() : 0;

            if (readOnly) {
                text(Integer.toString(intBuffer[0]));
            } else if (ImGui.dragInt("##val", intBuffer)) {
                setFieldWholeNumber(field, instance, type, intBuffer[0]);
                invokeCallback(editorAnnot, instance);
            }
        }
        else if (type == boolean.class || type == Boolean.class) {
            boolean val = type == boolean.class ? field.getBoolean(instance) : Boolean.TRUE.equals(field.get(instance));
            if (ImGui.checkbox("##val", val)) {
                val = !val;
                if (type == boolean.class) field.setBoolean(instance, val);
                else field.set(instance, val);
                invokeCallback(editorAnnot, instance);
            }
        }
        else if (type == String.class) {
            Object strVal = field.get(instance);
            ImString fieldBuf = new ImString(strVal != null ? (String) strVal : "", 512);
            if (ImGui.inputText("##val", fieldBuf)) {
                field.set(instance, fieldBuf.get());
                invokeCallback(editorAnnot, instance);
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
                field.set(instance, new Color(floatBuffer[0], floatBuffer[1], floatBuffer[2], floatBuffer[3]));
                invokeCallback(editorAnnot, instance);
            }
        }
        else if (type == Vector2f.class || type == Vector3f.class || type == Vector4f.class) {
            Vector2f vec2 = type == Vector2f.class ? (Vector2f) field.get(instance) : null;
            Vector3f vec3 = type == Vector3f.class ? (Vector3f) field.get(instance) : null;
            Vector4f vec4 = type == Vector4f.class ? (Vector4f) field.get(instance) : null;

            if (vec2 == null && type == Vector2f.class) { vec2 = new Vector2f(); field.set(instance, vec2); }
            if (vec3 == null && type == Vector3f.class) { vec3 = new Vector3f(); field.set(instance, vec3); }
            if (vec4 == null && type == Vector4f.class) { vec4 = new Vector4f(); field.set(instance, vec4); }

            if (vec2 != null) {
                floatBuffer[0] = vec2.x; floatBuffer[1] = vec2.y;
                if (ImGui.dragFloat2("##val", floatBuffer, 0.1f)) {
                    vec2.set(floatBuffer[0], floatBuffer[1]);
                    invokeCallback(editorAnnot, instance);
                }
            } else if (vec3 != null) {
                floatBuffer[0] = vec3.x; floatBuffer[1] = vec3.y; floatBuffer[2] = vec3.z;
                if (ImGui.dragFloat3("##val", floatBuffer, 0.1f)) {
                    if (label.equalsIgnoreCase("scale")) {
                        floatBuffer[0] = Math.max(MIN_SCALE, floatBuffer[0]);
                        floatBuffer[1] = Math.max(MIN_SCALE, floatBuffer[1]);
                        floatBuffer[2] = Math.max(MIN_SCALE, floatBuffer[2]);
                    }
                    vec3.set(floatBuffer[0], floatBuffer[1], floatBuffer[2]);
                    invokeCallback(editorAnnot, instance);
                }
            } else if (vec4 != null) {
                floatBuffer[0] = vec4.x; floatBuffer[1] = vec4.y; floatBuffer[2] = vec4.z; floatBuffer[3] = vec4.w;
                if (ImGui.dragFloat4("##val", floatBuffer, 0.1f)) {
                    vec4.set(floatBuffer[0], floatBuffer[1], floatBuffer[2], floatBuffer[3]);
                    invokeCallback(editorAnnot, instance);
                }
            }
        }
        else if (type == Vector2i.class || type == Vector3i.class || type == Vector4i.class) {
            Vector2i vec2i = type == Vector2i.class ? (Vector2i) field.get(instance) : null;
            Vector3i vec3i = type == Vector3i.class ? (Vector3i) field.get(instance) : null;
            Vector4i vec4i = type == Vector4i.class ? (Vector4i) field.get(instance) : null;

            if (vec2i == null && type == Vector2i.class) { vec2i = new Vector2i(); field.set(instance, vec2i); }
            if (vec3i == null && type == Vector3i.class) { vec3i = new Vector3i(); field.set(instance, vec3i); }
            if (vec4i == null && type == Vector4i.class) { vec4i = new Vector4i(); field.set(instance, vec4i); }

            if (vec2i != null) {
                intBuffer[0] = vec2i.x; intBuffer[1] = vec2i.y;
                if (ImGui.dragInt2("##val", intBuffer)) {
                    vec2i.set(intBuffer[0], intBuffer[1]);
                    invokeCallback(editorAnnot, instance);
                }
            } else if (vec3i != null) {
                intBuffer[0] = vec3i.x; intBuffer[1] = vec3i.y; intBuffer[2] = vec3i.z;
                if (ImGui.dragInt3("##val", intBuffer)) {
                    vec3i.set(intBuffer[0], intBuffer[1], intBuffer[2]);
                    invokeCallback(editorAnnot, instance);
                }
            } else if (vec4i != null) {
                intBuffer[0] = vec4i.x; intBuffer[1] = vec4i.y; intBuffer[2] = vec4i.z; intBuffer[3] = vec4i.w;
                if (ImGui.dragInt4("##val", intBuffer)) {
                    vec4i.set(intBuffer[0]);
                    invokeCallback(editorAnnot, instance);
                }
            }
        }
        // 10. Enums
        else if (type.isEnum() || Enum.class.isAssignableFrom(type)) {
            @SuppressWarnings("unchecked")
            Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) (type.isEnum() ? type : type.getSuperclass());
            EnumCache cache = enumCache.computeIfAbsent(enumClass, EnumCache::new);
            if (cache.constants.length > 0) {
                Object currentVal = field.get(instance);
                int currentIndex = currentVal != null ? ((Enum<?>) currentVal).ordinal() : 0;
                if (currentIndex < 0 || currentIndex >= cache.constants.length) currentIndex = 0;

                imIntBuffer.set(currentIndex);
                if (ImGui.combo("##val", imIntBuffer, cache.names)) {
                    int selected = imIntBuffer.get();
                    if (selected >= 0 && selected < cache.constants.length) {
                        field.set(instance, cache.constants[selected]);
                        invokeCallback(editorAnnot, instance);
                    }
                }
            }
        }

        ImGui.popID();
    }

    private static void setFieldFloat(Field field, Object instance, Class<?> type, float val) throws IllegalAccessException {
        if (type == float.class) field.setFloat(instance, val);
        else field.set(instance, val);
    }
    private static void setFieldDouble(Field field, Object instance, Class<?> type, double val) throws IllegalAccessException {
        if (type == double.class) field.setDouble(instance, val);
        else field.set(instance, val);
    }
    private static void setFieldWholeNumber(Field field, Object instance, Class<?> type, int val) throws IllegalAccessException {
        if (type == int.class) field.setInt(instance, val);
        else if (type == long.class) field.setLong(instance, val);
        else if (type == short.class) field.setShort(instance, (short) val);
        else if (type == byte.class) field.setByte(instance, (byte) val);
        else field.set(instance, val);
    }
    private static void invokeCallback(Editor editorAnnot, Object instance) {
        if (editorAnnot == null) return;
        String callbackName = editorAnnot.callbackMethodName();
        if (callbackName != null && !callbackName.isEmpty()) {
            try {
                Method method = instance.getClass().getDeclaredMethod(callbackName);
                method.setAccessible(true);
                method.invoke(instance);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static GameObject getSelectedGameObject() {
        return selectedGameObject;
    }
    public static void setSelectedGameObject(GameObject gameObject) {
        selectedGameObject = gameObject;
    }

    public static String inputField(String placeHolder, String label) {
        ImString imString = new ImString();
        ImGui.inputText(label, imString);
        return imString.get();
    }
    public static boolean inputField(String label, ImString buffer) {
        return ImGui.inputText(label, buffer);
    }

    public static void hierarchy(PeachLevel level) {
        if (level == null) return;
        window("Hierarchy", () -> {
            List<GameObject> gameObjects = level.getGameObjects();

            // 1. Draw input text using dedicated buffer
            ImGui.inputText("##NewGameObjectName", newGameObjectNameBuffer);

            sameLine();

            // 2. Read from buffer only when the button is pressed
            if (button("Add")) {
                String objectName = newGameObjectNameBuffer.get();
                if (objectName.isEmpty()) {
                    objectName = "GameObject"; // Fallback default
                }
                level.addGameObject(new GameObject(objectName));

                // Clear the input box after creation
                newGameObjectNameBuffer.set("");
            }

            if (gameObjects == null || gameObjects.isEmpty()) {
                ImGui.textDisabled("No GameObjects in level");
                return;
            }

            GameObject toDelete = null;
            for (GameObject gameObject : gameObjects) {
                if (gameObject == null) continue;
                ImGui.pushID(gameObject.hashCode());

                boolean isSelected = (gameObject == selectedGameObject);
                String displayName = gameObject.name != null && !gameObject.name.isEmpty()
                        ? gameObject.name
                        : "GameObject";

                float deleteBtnWidth = 55.0f;
                float availWidth = ImGui.getContentRegionAvailX();
                float spacing = ImGui.getStyle().getItemSpacingX();
                float selectableWidth = Math.max(20.0f, availWidth - deleteBtnWidth - spacing);

                if (ImGui.selectable(displayName, isSelected, ImGuiSelectableFlags.AllowOverlap, selectableWidth, 0.0f)) {
                    selectedGameObject = gameObject;
                }

                sameLine();
                if (ImGui.button("Delete##" + gameObject.hashCode(), deleteBtnWidth, 0.0f)) {
                    toDelete = gameObject;
                }

                ImGui.popID();
            }

            if (toDelete != null) {
                if (selectedGameObject == toDelete) {
                    selectedGameObject = null;
                }
                level.removeGameObject(toDelete);
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
            goNameBuffer.set(gameObject.name != null ? gameObject.name : "");
            ImGui.setNextItemWidth(180.0f);
            if (ImGui.inputText("##GOName", goNameBuffer)) {
                gameObject.name = goNameBuffer.get();
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
                Component toRemove = null;
                for (int i = 0; i < components.size(); i++) {
                    Component component = components.get(i);
                    if (component == null) continue;
                    ImGui.pushID(component.hashCode());

                    ImGui.textColored(0.9f, 0.9f, 0.4f, 1.0f, "• " + component.getName());
                    sameLine();
                    if (button("Remove##" + component.hashCode())) {
                        toRemove = component;
                    }

                    Field[] fields = getEditorFields(component.getClass());
                    if (fields.length == 0) {
                        ImGui.textDisabled("  No editable fields");
                    } else {
                        for (int fi = 0; fi < fields.length; fi++) {
                            Field field = fields[fi];
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

                if (toRemove != null) {
                    gameObject.removeComponent(toRemove);
                }
            }

            Components selectedComponent = dropdown("Add Component", Components.class, Components.Empty);
            if(selectedComponent != Components.Empty && !gameObject.hasComponent(selectedComponent.getComponentString())) {
                switch (selectedComponent) {
                    case Components.Transform -> gameObject.addComponent(new TransformComponent());
                    case Components.MeshRenderer -> gameObject.addComponent(new MeshRendererComponent());
                    case Components.Camera -> gameObject.addComponent(new CameraComponent());
                    case Components.Light -> gameObject.addComponent(new LightComponent());
                    case Components.CubeCollider -> gameObject.addComponent(new CubeColliderComponent());
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

    private static void handleGizmoHotkeys() {
        if (!initialized) return;
        ImGuiIO io = ImGui.getIO();
        if (io.getWantTextInput()) return;
        if (Input.isMouseButtonDown(MouseButton.RIGHT)) return;

        if (Input.isKeyPressed(Key.NUM_1) || Input.isKeyPressed(Key.W)) {
            gizmoOperation = Operation.TRANSLATE;
        } else if (Input.isKeyPressed(Key.NUM_2) || Input.isKeyPressed(Key.E)) {
            gizmoOperation = Operation.ROTATE;
        } else if (Input.isKeyPressed(Key.NUM_3) || Input.isKeyPressed(Key.R)) {
            gizmoOperation = Operation.SCALE;
        } else if (Input.isKeyPressed(Key.NUM_4) || Input.isKeyPressed(Key.T)) {
            gizmoOperation = Operation.UNIVERSAL;
        }
    }
    public static void gizmoToolbar() {
        window("Gizmo Controls", () -> {
            ImGui.text("Operation:");
            if (ImGui.radioButton("Translate (1)", gizmoOperation == Operation.TRANSLATE)) {
                gizmoOperation = Operation.TRANSLATE;
            }
            ImGui.sameLine();
            if (ImGui.radioButton("Rotate (2)", gizmoOperation == Operation.ROTATE)) {
                gizmoOperation = Operation.ROTATE;
            }
            ImGui.sameLine();
            if (ImGui.radioButton("Scale (3)", gizmoOperation == Operation.SCALE)) {
                gizmoOperation = Operation.SCALE;
            }
            ImGui.sameLine();
            if (ImGui.radioButton("Universal (4)", gizmoOperation == Operation.UNIVERSAL)) {
                gizmoOperation = Operation.UNIVERSAL;
            }

            ImGui.separator();
            ImGui.text("Mode:");
            if (ImGui.radioButton("Local", gizmoMode == Mode.LOCAL)) {
                gizmoMode = Mode.LOCAL;
            }
            ImGui.sameLine();
            if (ImGui.radioButton("World", gizmoMode == Mode.WORLD)) {
                gizmoMode = Mode.WORLD;
            }

            ImGui.separator();
            if (ImGui.checkbox("Snap", gizmoSnap)) {
                gizmoSnap = !gizmoSnap;
            }
            if (gizmoSnap) {
                ImGui.sameLine();
                floatBuffer[0] = gizmoSnapValue;
                ImGui.setNextItemWidth(80.0f);
                if (ImGui.dragFloat("##SnapVal", floatBuffer, 0.1f, 0.01f, 100.0f)) {
                    gizmoSnapValue = floatBuffer[0];
                    gizmoSnapValues[0] = floatBuffer[0];
                    gizmoSnapValues[1] = floatBuffer[0];
                    gizmoSnapValues[2] = floatBuffer[0];
                }
            }
        });
    }
    public static void gizmo(GameObject gameObject) {
        gizmo(gameObject, Renderer.getCamera());
    }
    public static void gizmo(GameObject gameObject, Camera camera) {
        if (gameObject == null || !gameObject.isActive()) return;
        TransformComponent transform = gameObject.transform;
        if (transform == null && gameObject.hasComponent(TransformComponent.class)) {
            transform = gameObject.getComponent(TransformComponent.class);
        }
        if (transform != null) {
            gizmo(transform, camera);
        }
    }
    public static void gizmo(TransformComponent transform) {
        gizmo(transform, Renderer.getCamera());
    }
    public static void gizmo(TransformComponent transform, Camera camera) {
        drawGizmo(transform, camera, gizmoOperation, gizmoMode, gizmoSnap, gizmoSnapValues);
    }
    public static void drawGizmo(TransformComponent transform, Camera camera, int operation, int mode, boolean snap, float[] snapValues) {
        if (transform == null || camera == null || !initialized) return;
        if (transform.position == null || transform.rotation == null || transform.scale == null) return;

        ImGuizmo.setOrthographic(false);
        ImGuizmo.enable(true);
        ImGuizmo.setDrawList(ImGui.getForegroundDrawList());

        float displayW = ImGui.getIO().getDisplaySizeX();
        float displayH = ImGui.getIO().getDisplaySizeY();
        ImGuizmo.setRect(0.0f, 0.0f, displayW, displayH);

        camera.getViewMatrix().get(gizmoViewBuffer);
        camera.getProjectionMatrix().get(gizmoProjBuffer);

        gizmoPosBuffer[0] = transform.position.x;
        gizmoPosBuffer[1] = transform.position.y;
        gizmoPosBuffer[2] = transform.position.z;

        gizmoRotBuffer[0] = transform.rotation.x;
        gizmoRotBuffer[1] = transform.rotation.y;
        gizmoRotBuffer[2] = transform.rotation.z;

        gizmoScaleBuffer[0] = Math.max(MIN_SCALE, transform.scale.x);
        gizmoScaleBuffer[1] = Math.max(MIN_SCALE, transform.scale.y);
        gizmoScaleBuffer[2] = Math.max(MIN_SCALE, transform.scale.z);

        ImGuizmo.recomposeMatrixFromComponents(gizmoPosBuffer, gizmoRotBuffer, gizmoScaleBuffer, gizmoMatrixBuffer);

        if (snap && snapValues != null && snapValues.length >= 3) {
            ImGuizmo.manipulate(gizmoViewBuffer, gizmoProjBuffer, operation, mode, gizmoMatrixBuffer, null, snapValues);
        } else {
            ImGuizmo.manipulate(gizmoViewBuffer, gizmoProjBuffer, operation, mode, gizmoMatrixBuffer);
        }

        if (ImGuizmo.isUsing()) {
            ImGuizmo.decomposeMatrixToComponents(gizmoMatrixBuffer, gizmoPosBuffer, gizmoRotBuffer, gizmoScaleBuffer);
            gizmoScaleBuffer[0] = Math.max(MIN_SCALE, gizmoScaleBuffer[0]);
            gizmoScaleBuffer[1] = Math.max(MIN_SCALE, gizmoScaleBuffer[1]);
            gizmoScaleBuffer[2] = Math.max(MIN_SCALE, gizmoScaleBuffer[2]);
            transform.position.set(gizmoPosBuffer[0], gizmoPosBuffer[1], gizmoPosBuffer[2]);
            transform.rotation.set(gizmoRotBuffer[0], gizmoRotBuffer[1], gizmoRotBuffer[2]);
            transform.scale.set(gizmoScaleBuffer[0], gizmoScaleBuffer[1], gizmoScaleBuffer[2]);
        }
    }
    public static int getGizmoOperation() {
        return gizmoOperation;
    }
    public static void setGizmoOperation(int operation) {
        gizmoOperation = operation;
    }
    public static int getGizmoMode() {
        return gizmoMode;
    }
    public static void setGizmoMode(int mode) {
        gizmoMode = mode;
    }
    public static boolean isGizmoSnap() {
        return gizmoSnap;
    }
    public static void setGizmoSnap(boolean snap) {
        gizmoSnap = snap;
    }
    public static float getGizmoSnapValue() {
        return gizmoSnapValue;
    }
    public static void setGizmoSnapValue(float snapValue) {
        gizmoSnapValue = snapValue;
        gizmoSnapValues[0] = snapValue;
        gizmoSnapValues[1] = snapValue;
        gizmoSnapValues[2] = snapValue;
    }
    public static float[] getGizmoSnapValues() {
        return gizmoSnapValues;
    }
    public static boolean isGizmoUsing() {
        return initialized && ImGuizmo.isUsing();
    }
    public static boolean isGizmoOver() {
        return initialized && ImGuizmo.isOver();
    }

    public static void debugEditor(PeachLevel level) {
        debugEditor(level, Renderer.getCamera());
    }
    public static void debugEditor(PeachLevel level, Camera camera) {
        hierarchy(level);
        inspector();
        gizmoToolbar();
        showStats();
        gizmo(selectedGameObject, camera);
        saveTest(level);
    }

    public static void saveTest(PeachLevel level) {
        PeachGui.window("Save Test", () -> {
            if(PeachGui.button("Save")) {
                level.save("saves/level1.json");
            } else if(PeachGui.button("Load")) {
                level.load("saves/level1.json");
            }
        });
    }
}