package net.meowsers;

import imgui.ImGui;
import imgui.ImGuiIO;
import net.meowsers.Peach.ECS.Component;
import net.meowsers.Peach.ECS.Components.CameraComponent;
import net.meowsers.Peach.ECS.Components.LightComponent;
import net.meowsers.Peach.ECS.Components.TransformComponent;
import net.meowsers.Peach.GUI.Editor;
import net.meowsers.Peach.GUI.PeachGui;
import net.meowsers.Peach.Structures.Color;
import net.meowsers.Peach.Structures.Key;
import net.meowsers.Peach.Structures.LightType;
import org.joml.*;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

public class PeachGuiTest {

    enum SampleEnum {
        OPTION_A,
        OPTION_B,
        OPTION_C
    }

    static class AllTypesComponent extends Component {
        @Editor public float primitiveFloat = 1.23f;
        @Editor public Float boxedFloat = 4.56f;
        @Editor public double primitiveDouble = 7.89;
        @Editor public Double boxedDouble = 10.11;
        @Editor public int primitiveInt = 42;
        @Editor public Integer boxedInt = 100;
        @Editor public long primitiveLong = 12345L;
        @Editor public Long boxedLong = 67890L;
        @Editor public short primitiveShort = 12;
        @Editor public Short boxedShort = 34;
        @Editor public byte primitiveByte = 5;
        @Editor public Byte boxedByte = 6;
        @Editor public boolean primitiveBool = true;
        @Editor public Boolean boxedBool = false;
        @Editor public String stringVal = "Hello Peach";
        @Editor public Color colorVal = new Color(0.1f, 0.2f, 0.3f, 1.0f);
        @Editor public Vector2f vec2f = new Vector2f(1.0f, 2.0f);
        @Editor public Vector3f vec3f = new Vector3f(3.0f, 4.0f, 5.0f);
        @Editor public Vector4f vec4f = new Vector4f(6.0f, 7.0f, 8.0f, 9.0f);
        @Editor public Vector2i vec2i = new Vector2i(10, 20);
        @Editor public Vector3i vec3i = new Vector3i(30, 40, 50);
        @Editor public Vector4i vec4i = new Vector4i(60, 70, 80, 90);
        @Editor public LightType lightType = LightType.SPOT;
        @Editor public Key key = Key.SPACE;
        @Editor public SampleEnum sampleEnum = SampleEnum.OPTION_B;

        public String nonEditorField = "hidden";
    }

    @BeforeAll
    public static void setupImGui() {
        ImGui.createContext();
        ImGuiIO io = ImGui.getIO();
        io.setDisplaySize(800, 600);
        io.getFonts().build();
    }

    @AfterAll
    public static void tearDownImGui() {
        ImGui.destroyContext();
    }

    @Test
    public void testEditorFieldsCaching() {
        Field[] lightFields = PeachGui.getEditorFields(LightComponent.class);
        assertNotNull(lightFields);
        assertTrue(lightFields.length > 0);

        List<String> fieldNames = Arrays.stream(lightFields).map(Field::getName).collect(Collectors.toList());
        assertTrue(fieldNames.contains("lightType"));
        assertTrue(fieldNames.contains("lightPosition"));
        assertTrue(fieldNames.contains("lightDirection"));
        assertTrue(fieldNames.contains("lightColor"));
        assertTrue(fieldNames.contains("ambientColor"));
        assertTrue(fieldNames.contains("lightIntensity"));
        assertTrue(fieldNames.contains("isCurrent"));

        Field[] cameraFields = PeachGui.getEditorFields(CameraComponent.class);
        assertNotNull(cameraFields);
        List<String> cameraFieldNames = Arrays.stream(cameraFields).map(Field::getName).collect(Collectors.toList());
        assertTrue(cameraFieldNames.contains("cameraPosition"));
        assertTrue(cameraFieldNames.contains("cameraYaw"));
        assertTrue(cameraFieldNames.contains("cameraPitch"));
        assertTrue(cameraFieldNames.contains("cameraFov"));
        assertTrue(cameraFieldNames.contains("isCurrent"));

        Field[] transformFields = PeachGui.getEditorFields(TransformComponent.class);
        assertNotNull(transformFields);
        List<String> transformFieldNames = Arrays.stream(transformFields).map(Field::getName).collect(Collectors.toList());
        assertTrue(transformFieldNames.contains("position"));
        assertTrue(transformFieldNames.contains("rotation"));
        assertTrue(transformFieldNames.contains("scale"));

        // Second call should return the exact same cached array
        assertSame(lightFields, PeachGui.getEditorFields(LightComponent.class));
        assertSame(cameraFields, PeachGui.getEditorFields(CameraComponent.class));
        assertSame(transformFields, PeachGui.getEditorFields(TransformComponent.class));
    }

    @Test
    public void testDrawFieldEditorWithAllTypes() throws Exception {
        AllTypesComponent comp = new AllTypesComponent();
        Field[] fields = PeachGui.getEditorFields(AllTypesComponent.class);

        assertEquals(25, fields.length);

        ImGui.newFrame();
        for (Field field : fields) {
            PeachGui.drawFieldEditor(field, comp);
        }
        ImGui.render();
    }

    @Test
    public void testDrawFieldEditorWithLightAndCamera() throws Exception {
        LightComponent light = new LightComponent();
        Field[] lightFields = PeachGui.getEditorFields(LightComponent.class);

        CameraComponent camera = new CameraComponent();
        Field[] cameraFields = PeachGui.getEditorFields(CameraComponent.class);

        TransformComponent transform = new TransformComponent();
        Field[] transformFields = PeachGui.getEditorFields(TransformComponent.class);

        ImGui.newFrame();
        for (Field f : lightFields) {
            PeachGui.drawFieldEditor(f, light);
        }
        for (Field f : cameraFields) {
            PeachGui.drawFieldEditor(f, camera);
        }
        for (Field f : transformFields) {
            PeachGui.drawFieldEditor(f, transform);
        }
        ImGui.render();
    }

    @Test
    public void testDrawFieldEditorWithNullObjects() throws Exception {
        AllTypesComponent comp = new AllTypesComponent();
        comp.colorVal = null;
        comp.vec2f = null;
        comp.vec3f = null;
        comp.vec4f = null;
        comp.vec2i = null;
        comp.vec3i = null;
        comp.vec4i = null;
        comp.stringVal = null;
        comp.boxedFloat = null;
        comp.boxedDouble = null;
        comp.boxedInt = null;
        comp.boxedLong = null;
        comp.boxedShort = null;
        comp.boxedByte = null;
        comp.boxedBool = null;
        comp.lightType = null;

        Field[] fields = PeachGui.getEditorFields(AllTypesComponent.class);

        ImGui.newFrame();
        for (Field field : fields) {
            PeachGui.drawFieldEditor(field, comp);
        }
        ImGui.render();

        assertNotNull(comp.colorVal);
        assertNotNull(comp.vec2f);
        assertNotNull(comp.vec3f);
        assertNotNull(comp.vec4f);
        assertNotNull(comp.vec2i);
        assertNotNull(comp.vec3i);
        assertNotNull(comp.vec4i);
    }

    @Test
    public void testHierarchyAndInspector() {
        net.meowsers.Peach.GameEngine.PeachLevel level = new net.meowsers.Peach.GameEngine.PeachLevel() {};
        net.meowsers.Peach.ECS.GameObject go1 = new net.meowsers.Peach.ECS.GameObject("TestGo1");
        go1.addComponent(new LightComponent());
        go1.addComponent(new CameraComponent());
        go1.addComponent(new TransformComponent());
        level.addGameObject(go1);

        net.meowsers.Peach.ECS.GameObject go2 = new net.meowsers.Peach.ECS.GameObject("TestGo2");
        level.addGameObject(go2);

        // Test with no selection
        PeachGui.setSelectedGameObject(null);
        assertNull(PeachGui.getSelectedGameObject());

        ImGui.newFrame();
        PeachGui.hierarchy(level);
        PeachGui.inspector();
        ImGui.render();

        // Test with selection
        PeachGui.setSelectedGameObject(go1);
        assertSame(go1, PeachGui.getSelectedGameObject());

        ImGui.newFrame();
        PeachGui.hierarchy(level);
        PeachGui.inspector();
        PeachGui.inspector(go2);
        ImGui.render();
    }

    @Test
    public void testHelperMethods() {
        Color c = new Color(0.2f, 0.4f, 0.6f, 0.8f);
        Vector2f v2 = new Vector2f(1.0f, 2.0f);
        Vector3f v3 = new Vector3f(1.0f, 2.0f, 3.0f);
        Vector4f v4 = new Vector4f(1.0f, 2.0f, 3.0f, 4.0f);

        ImGui.newFrame();
        PeachGui.colorEdit4("Color4", c);
        PeachGui.colorEdit3("Color3", c);
        PeachGui.colorEdit("Color", c);
        PeachGui.dragFloat2("Vec2", v2);
        PeachGui.dragFloat3("Vec3", v3);
        PeachGui.dragFloat4("Vec4", v4);
        PeachGui.dragFloat("FloatVal", 5.0f);
        ImGui.render();
    }

    @Test
    public void testCameraTransformSynchronization() {
        net.meowsers.Peach.ECS.GameObject go = new net.meowsers.Peach.ECS.GameObject("CamTest");
        CameraComponent cam = new CameraComponent();
        go.addComponent(cam);

        cam.start();
        assertEquals(0.0f, cam.getPosition().x, 0.001f);
        assertEquals(0.0f, cam.getPosition().y, 0.001f);
        assertEquals(3.0f, cam.getPosition().z, 0.001f);

        // Modifying transform.position should sync to camera
        go.transform.position.set(10.0f, 20.0f, 30.0f);
        cam.update(0.016f);
        assertEquals(10.0f, cam.cameraPosition.x, 0.001f);
        assertEquals(20.0f, cam.cameraPosition.y, 0.001f);
        assertEquals(30.0f, cam.cameraPosition.z, 0.001f);
        assertEquals(10.0f, cam.getCamera().getPosition().x, 0.001f);

        // Modifying cameraPosition directly (as in inspector) should sync to transform
        cam.cameraPosition.set(5.0f, 6.0f, 7.0f);
        cam.update(0.016f);
        assertEquals(5.0f, go.transform.position.x, 0.001f);
        assertEquals(6.0f, go.transform.position.y, 0.001f);
        assertEquals(7.0f, go.transform.position.z, 0.001f);

        // Modifying transform rotation should sync yaw and pitch
        go.transform.rotation.set(15.0f, 45.0f, 0.0f);
        cam.update(0.016f);
        assertEquals(15.0f, cam.cameraPitch, 0.001f);
        assertEquals(45.0f, cam.cameraYaw, 0.001f);
        assertEquals(15.0f, cam.getCamera().getPitch(), 0.001f);
        assertEquals(45.0f, cam.getCamera().getYaw(), 0.001f);
    }

    @Test
    public void testLightTransformSynchronization() {
        net.meowsers.Peach.ECS.GameObject go = new net.meowsers.Peach.ECS.GameObject("LightTest");
        LightComponent light = new LightComponent();
        go.addComponent(light);

        light.start();
        assertEquals(10.0f, light.getPosition().x, 0.001f);
        assertEquals(20.0f, light.getPosition().y, 0.001f);
        assertEquals(15.0f, light.getPosition().z, 0.001f);

        // Modifying transform.position should sync to light
        go.transform.position.set(1.0f, 2.0f, 3.0f);
        light.update(0.016f);
        assertEquals(1.0f, light.lightPosition.x, 0.001f);
        assertEquals(2.0f, light.lightPosition.y, 0.001f);
        assertEquals(3.0f, light.lightPosition.z, 0.001f);
        assertEquals(1.0f, light.getLight().getPosition().x, 0.001f);

        // Modifying lightPosition directly should sync to transform
        light.lightPosition.set(7.0f, 8.0f, 9.0f);
        light.update(0.016f);
        assertEquals(7.0f, go.transform.position.x, 0.001f);
        assertEquals(8.0f, go.transform.position.y, 0.001f);
        assertEquals(9.0f, go.transform.position.z, 0.001f);
    }

    @Test
    public void testMeshSetColorVertexUpdate() {
        net.meowsers.Peach.Graphics.Mesh sphere = net.meowsers.Peach.Graphics.Mesh.createSphere(1.0f, Color.Red);
        assertEquals(Color.Red.r, sphere.getVertices().get(0).r, 0.001f);

        sphere.setColor(Color.Blue);
        assertEquals(Color.Blue, sphere.getColor());
        assertEquals(Color.Blue.r, sphere.getVertices().get(0).r, 0.001f);
        assertEquals(Color.Blue.b, sphere.getVertices().get(0).b, 0.001f);
    }
}
