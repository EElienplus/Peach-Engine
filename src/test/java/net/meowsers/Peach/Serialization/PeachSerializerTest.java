package net.meowsers.Peach.Serialization;

import net.meowsers.Peach.ECS.Components.CameraComponent;
import net.meowsers.Peach.ECS.Components.LightComponent;
import net.meowsers.Peach.ECS.Components.MeshRendererComponent;
import net.meowsers.Peach.ECS.Components.TransformComponent;
import net.meowsers.Peach.ECS.GameObject;
import net.meowsers.Peach.GameEngine.PeachLevel;
import net.meowsers.Peach.Graphics.Model;
import net.meowsers.Peach.Structures.Color;
import net.meowsers.Peach.Structures.LightType;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class PeachSerializerTest {

    @Test
    public void testSerializeAndDeserializeSingleGameObject() {
        GameObject go = new GameObject("Player");
        go.transform.setPosition(new Vector3f(1.0f, 2.0f, 3.0f));
        go.transform.setRotation(new Vector3f(45.0f, 90.0f, 0.0f));
        go.transform.setScale(new Vector3f(2.0f, 2.0f, 2.0f));

        LightComponent light = new LightComponent(new Vector3f(5.0f, 5.0f, 5.0f), Color.Red, 2.5f, LightType.POINT);
        go.addComponent(light);

        String json = PeachSerializer.serialize(go);
        assertNotNull(json);
        assertTrue(json.contains("Player"));
        assertTrue(json.contains("LightComponent"));

        GameObject deserialized = PeachSerializer.deserializeGameObject(json);
        assertNotNull(deserialized);
        assertEquals("Player", deserialized.name);
        assertNotNull(deserialized.transform);
        assertEquals(1.0f, deserialized.transform.position.x, 0.001f);
        assertEquals(2.0f, deserialized.transform.position.y, 0.001f);
        assertEquals(3.0f, deserialized.transform.position.z, 0.001f);

        LightComponent deserializedLight = deserialized.getComponent(LightComponent.class);
        assertNotNull(deserializedLight);
        assertEquals(LightType.POINT, deserializedLight.lightType);
        assertEquals(2.5f, deserializedLight.lightIntensity, 0.001f);
        assertSame(deserialized, deserializedLight.getGameObject());
        assertSame(deserialized.transform, deserializedLight.getTransform());
    }

    @Test
    public void testSerializeAndDeserializeList() {
        GameObject go1 = new GameObject("CameraObject");
        go1.addComponent(new CameraComponent(true));

        GameObject go2 = new GameObject("MeshObject");
        MeshRendererComponent mr = new MeshRendererComponent();
        mr.setVisible(true);
        go2.addComponent(mr);

        String json = PeachSerializer.serialize(List.of(go1, go2));
        assertNotNull(json);

        List<GameObject> list = PeachSerializer.deserializeGameObjects(json);
        assertEquals(2, list.size());

        GameObject loadedCam = list.get(0);
        assertEquals("CameraObject", loadedCam.name);
        assertNotNull(loadedCam.getComponent(CameraComponent.class));
        assertSame(loadedCam, loadedCam.getComponent(CameraComponent.class).getGameObject());

        GameObject loadedMesh = list.get(1);
        assertEquals("MeshObject", loadedMesh.name);
        assertNotNull(loadedMesh.getComponent(MeshRendererComponent.class));
        assertTrue(loadedMesh.getComponent(MeshRendererComponent.class).isVisible());
    }

    @Test
    public void testFileSaveAndLevelLoad(@TempDir Path tempDir) {
        Path filePath = tempDir.resolve("level_test.json");

        PeachLevel level = new PeachLevel() {};
        GameObject go1 = level.createGameObject("Sun", new LightComponent(Color.Yellow, 10.0f, LightType.DIRECTIONAL));
        GameObject go2 = level.createGameObject("Hero");
        go2.transform.setPosition(new Vector3f(10, 0, -5));

        level.save(filePath.toString());
        assertTrue(filePath.toFile().exists());

        PeachLevel newLevel = new PeachLevel() {};
        List<GameObject> loaded = newLevel.load(filePath.toString());

        assertEquals(2, loaded.size());
        assertEquals(2, newLevel.getGameObjects().size());

        GameObject loadedSun = newLevel.findGameObject("Sun");
        assertNotNull(loadedSun);
        LightComponent sunLight = loadedSun.getComponent(LightComponent.class);
        assertNotNull(sunLight);
        assertEquals(LightType.DIRECTIONAL, sunLight.lightType);
        assertEquals(10.0f, sunLight.lightIntensity, 0.001f);

        GameObject loadedHero = newLevel.findGameObject("Hero");
        assertNotNull(loadedHero);
        assertEquals(10.0f, loadedHero.transform.position.x, 0.001f);
        assertSame(newLevel, loadedHero.getLevel());
    }

    @Test
    public void testRunningLevelLoadOverwritesGameObjects(@TempDir Path tempDir) {
        Path filePath = tempDir.resolve("overwrite_level_test.json");

        // Save a level with 2 objects: "NewObj1", "NewObj2"
        PeachLevel savedLevel = new PeachLevel() {};
        savedLevel.createGameObject("NewObj1");
        savedLevel.createGameObject("NewObj2");
        savedLevel.save(filePath.toString());

        // Create a running level with an existing object: "OldObj"
        PeachLevel runningLevel = new PeachLevel() {};
        GameObject oldObj = runningLevel.createGameObject("OldObj");
        runningLevel.internalStart();

        assertTrue(runningLevel.isStarted());
        assertTrue(oldObj.isStarted());
        assertEquals(1, runningLevel.getGameObjects().size());

        // Load saved level while running
        List<GameObject> loaded = runningLevel.load(filePath.toString());

        assertEquals(2, loaded.size());
        assertEquals(2, runningLevel.getGameObjects().size());
        assertNull(runningLevel.findGameObject("OldObj"), "Old game objects must be removed/overwritten");
        assertNotNull(runningLevel.findGameObject("NewObj1"));
        assertNotNull(runningLevel.findGameObject("NewObj2"));

        // Since runningLevel was started, the newly loaded objects must be automatically started
        assertTrue(runningLevel.findGameObject("NewObj1").isStarted());
        assertTrue(runningLevel.findGameObject("NewObj2").isStarted());
        assertSame(runningLevel, runningLevel.findGameObject("NewObj1").getLevel());
        assertSame(runningLevel, runningLevel.findGameObject("NewObj2").getLevel());
    }

    @Test
    public void testModelSerializationDoesNotSaveVertices() {
        GameObject go = new GameObject("bunny");
        MeshRendererComponent mr = new MeshRendererComponent();
        mr.setModel(new Model("src/main/resources/Models/bunny.obj"));
        mr.model.setScale(100);
        go.addComponent(mr);

        String json = PeachSerializer.serialize(go);
        assertNotNull(json);

        // Verify JSON is compact and does NOT contain the vertex array
        assertFalse(json.contains("\"vertices\""), "Serialized JSON must not contain raw vertices for file-backed models");
        assertTrue(json.contains("src/main/resources/Models/bunny.obj"));
        assertTrue(json.lines().count() < 100, "Serialized JSON should be compact, was " + json.lines().count() + " lines");

        // Verify deserialization properly restores the Model and its meshes from disk
        GameObject loaded = PeachSerializer.deserializeGameObject(json);
        assertNotNull(loaded);
        MeshRendererComponent loadedMR = loaded.getComponent(MeshRendererComponent.class);
        assertNotNull(loadedMR);
        assertNotNull(loadedMR.model);
        assertEquals("src/main/resources/Models/bunny.obj", loadedMR.model.getFilepath());
        assertEquals(100.0f, loadedMR.model.getScale().x, 0.001f);
        assertFalse(loadedMR.model.getMeshes().isEmpty(), "Model meshes should be reloaded from filepath");
    }

    @Test
    public void testMeshRendererModelPathClearAndReload() {
        GameObject go = new GameObject("bunny");
        MeshRendererComponent mr = new MeshRendererComponent();
        mr.setModel(new Model("src/main/resources/Models/bunny.obj"));
        mr.model.setScale(100);
        go.addComponent(mr);

        // Frame update saves current model properties
        mr.update(0.016f);
        assertFalse(mr.model.getMeshes().isEmpty());
        assertEquals(100.0f, mr.model.getScale().x, 0.001f);

        // Delete path (simulate user clearing input text)
        mr.setModelPath("");
        mr.update(0.016f);
        assertTrue(mr.model.getMeshes().isEmpty(), "Model should be empty when path is cleared");

        // Paste new path back in (e.g. quoted or with whitespace or relative)
        mr.setModelPath("\"src/main/resources/Models/bunny.obj\"");
        mr.update(0.016f);
        assertFalse(mr.model.getMeshes().isEmpty(), "Model should be reloaded when path is pasted back");
        assertEquals(100.0f, mr.model.getScale().x, 0.001f, "Model scale should be preserved across reload");

        // Test clearing again and reloading using just "bunny" without extension
        mr.setModelPath("");
        mr.update(0.016f);
        assertTrue(mr.model.getMeshes().isEmpty());

        mr.setModelPath("bunny");
        mr.update(0.016f);
        assertFalse(mr.model.getMeshes().isEmpty(), "Model should reload even when extension is omitted");
        assertEquals(100.0f, mr.model.getScale().x, 0.001f);

        // Test loading cube.obj
        mr.setModelPath("cube.obj");
        mr.update(0.016f);
        assertFalse(mr.model.getMeshes().isEmpty(), "Model should reload when path changes to cube.obj");
    }

    @Test
    public void testModelPathReflectionUpdateFromGui() throws Exception {
        GameObject go = new GameObject("bunny");
        MeshRendererComponent mr = new MeshRendererComponent();
        mr.setModel(new Model("src/main/resources/Models/bunny.obj"));
        mr.model.setScale(100);
        go.addComponent(mr);

        mr.update(0.016f);
        assertFalse(mr.model.getMeshes().isEmpty());

        // Simulate GUI reflection editing on private @Editor modelPath field
        java.lang.reflect.Field field = MeshRendererComponent.class.getDeclaredField("modelPath");
        field.setAccessible(true);

        // Clear field
        field.set(mr, "");
        mr.update(0.016f);
        assertTrue(mr.model.getMeshes().isEmpty(), "Model should unload when modelPath is cleared via reflection");

        // Type / paste new path
        field.set(mr, "Models/bunny.obj");
        mr.update(0.016f);
        assertFalse(mr.model.getMeshes().isEmpty(), "Model should reload when modelPath is set via reflection");
        assertEquals(100.0f, mr.model.getScale().x, 0.001f);

        // Test pasting with surrounding spaces and quotes
        field.set(mr, "  \"src/main/resources/Models/cube.obj\"  ");
        mr.update(0.016f);
        assertFalse(mr.model.getMeshes().isEmpty(), "Model should reload with quoted and padded path");

        // Test reloading extensionless path
        field.set(mr, "bunny");
        mr.update(0.016f);
        assertFalse(mr.model.getMeshes().isEmpty(), "Model should reload extensionless path 'bunny'");
        assertEquals(100.0f, mr.model.getScale().x, 0.001f);
    }
}
