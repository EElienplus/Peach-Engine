package net.meowsers;

import net.meowsers.Peach.ECS.Component;
import net.meowsers.Peach.ECS.Components.CameraComponent;
import net.meowsers.Peach.ECS.Components.LightComponent;
import net.meowsers.Peach.ECS.Components.MeshRendererComponent;
import net.meowsers.Peach.ECS.Components.TransformComponent;
import net.meowsers.Peach.ECS.GameObject;
import net.meowsers.Peach.GameEngine.PeachLevel;
import net.meowsers.Peach.Graphics.Camera;
import net.meowsers.Peach.Graphics.Mesh;
import net.meowsers.Peach.Graphics.Renderer;
import net.meowsers.Peach.Structures.Color;
import net.meowsers.Peach.Utils.Input;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ECSTest {

    static class CustomTestComponent extends Component {
        public boolean addedCalled = false;
        public boolean removedCalled = false;
        public boolean enableCalled = false;
        public boolean disableCalled = false;
        public boolean startCalled = false;
        public int updateCount = 0;
        public String message = "default";

        public CustomTestComponent() {
        }

        public CustomTestComponent(String msg) {
            this.message = msg;
        }

        public CustomTestComponent(String msg, int count) {
            this.message = msg + count;
        }

        @Override
        public void onAdded() {
            addedCalled = true;
        }

        @Override
        public void onRemoved() {
            removedCalled = true;
        }

        @Override
        public void onEnable() {
            enableCalled = true;
        }

        @Override
        public void onDisable() {
            disableCalled = true;
        }

        @Override
        public void start() {
            startCalled = true;
        }

        @Override
        public void update(float dt) {
            updateCount++;
        }
    }

    @Test
    public void testAddComponentInstance() {
        GameObject go = new GameObject("Player");
        CustomTestComponent comp = go.addComponent(new CustomTestComponent("hello"));

        assertNotNull(comp);
        assertSame(go, comp.getGameObject());
        assertTrue(comp.addedCalled);
        assertEquals("hello", comp.message);
        assertSame(go.transform, comp.getTransform());
        assertTrue(go.hasComponent(CustomTestComponent.class));
        assertSame(comp, go.getComponent(CustomTestComponent.class));
    }

    @Test
    public void testAddComponentByClass() {
        GameObject go = new GameObject("Player");
        CustomTestComponent comp = go.addComponent(CustomTestComponent.class);

        assertNotNull(comp);
        assertSame(go, comp.getGameObject());
        assertTrue(comp.addedCalled);
        assertEquals("default", comp.message);
        assertTrue(go.hasComponent(CustomTestComponent.class));
    }

    @Test
    public void testAddComponentByClassWithArgs() {
        GameObject go = new GameObject("Player");
        CustomTestComponent comp = go.addComponent(CustomTestComponent.class, "customMsg", 42);

        assertNotNull(comp);
        assertEquals("customMsg42", comp.message);
        assertTrue(comp.addedCalled);
    }

    @Test
    public void testGetOrAddComponent() {
        GameObject go = new GameObject("Player");
        assertFalse(go.hasComponent(CustomTestComponent.class));

        CustomTestComponent comp1 = go.getOrAddComponent(CustomTestComponent.class);
        assertNotNull(comp1);

        CustomTestComponent comp2 = go.getOrAddComponent(CustomTestComponent.class);
        assertSame(comp1, comp2);
    }

    @Test
    public void testGameObjectConstructorWithComponents() {
        CustomTestComponent c1 = new CustomTestComponent("c1");
        CameraComponent cam = new CameraComponent(false);
        GameObject go = new GameObject("Entity", c1, cam);

        assertSame(go, c1.getGameObject());
        assertSame(go, cam.getGameObject());
        assertTrue(go.hasComponent(CustomTestComponent.class));
        assertTrue(go.hasComponent(CameraComponent.class));
    }

    @Test
    public void testFluentBuilderPattern() {
        GameObject go = GameObject.create("Hero")
                .with(new CustomTestComponent("hero1"))
                .with(CameraComponent.class, false)
                .with(TransformComponent.class);

        assertTrue(go.hasComponent(CustomTestComponent.class));
        assertTrue(go.hasComponent(CameraComponent.class));
        assertTrue(go.hasComponent(TransformComponent.class));
    }

    @Test
    public void testComponentLifecycleAndHooks() {
        GameObject go = new GameObject("LifecycleEntity");
        CustomTestComponent comp = go.addComponent(new CustomTestComponent());

        assertTrue(comp.addedCalled);
        assertFalse(comp.startCalled);

        go.start();
        assertTrue(comp.startCalled);

        go.update(0.016f);
        assertEquals(1, comp.updateCount);

        comp.setEnabled(false);
        assertTrue(comp.disableCalled);
        go.update(0.016f);
        assertEquals(1, comp.updateCount); // update not called when disabled

        comp.setEnabled(true);
        assertTrue(comp.enableCalled);
        go.update(0.016f);
        assertEquals(2, comp.updateCount);

        go.removeComponent(comp);
        assertTrue(comp.removedCalled);
        assertNull(comp.getGameObject());
    }

    @Test
    public void testComponentShortcutMethods() {
        GameObject go = new GameObject("TestShortcuts");
        CustomTestComponent comp1 = go.addComponent(new CustomTestComponent());
        CameraComponent comp2 = comp1.addComponent(CameraComponent.class);

        assertNotNull(comp2);
        assertSame(comp2, comp1.getComponent(CameraComponent.class));
        assertTrue(comp1.hasComponent(CameraComponent.class));
        assertSame(go.transform, comp1.getTransform());

        List<CameraComponent> cams = comp1.getComponents(CameraComponent.class);
        assertEquals(1, cams.size());

        comp1.removeComponent(CameraComponent.class);
        assertFalse(comp1.hasComponent(CameraComponent.class));
    }

    @Test
    public void testLightComponent() {
        GameObject lightGo = new GameObject("LightGO");
        LightComponent lightComp = lightGo.addComponent(new LightComponent(new Vector3f(5.0f, 10.0f, 15.0f), Color.White, 2.0f, net.meowsers.Peach.Structures.LightType.SPOT));

        assertNotNull(lightComp.getLight());
        assertEquals(2.0f, lightComp.getIntensity());
        assertEquals(5.0f, lightComp.getPosition().x, 0.001f);
        assertEquals(net.meowsers.Peach.Structures.LightType.SPOT, lightComp.getType());
        assertTrue(lightComp.isSpot());

        lightGo.start();
        assertEquals(5.0f, lightGo.transform.position.x, 0.001f);

        lightComp.setPosition(new Vector3f(1.0f, 2.0f, 3.0f));
        assertEquals(1.0f, lightGo.transform.position.x, 0.001f);
        assertEquals(2.0f, lightGo.transform.position.y, 0.001f);
        assertEquals(3.0f, lightGo.transform.position.z, 0.001f);

        lightComp.setType(net.meowsers.Peach.Structures.LightType.DIRECTIONAL);
        assertTrue(lightComp.isDirectional());
        assertFalse(lightComp.isPoint());

        lightGo.update(0.016f);
    }

    @Test
    public void testLightTypesEnumAndProperties() {
        net.meowsers.Peach.Graphics.Light pointLight = new net.meowsers.Peach.Graphics.Light(net.meowsers.Peach.Structures.LightType.POINT);
        assertTrue(pointLight.isPoint());
        assertEquals(0, pointLight.getType().getId());

        net.meowsers.Peach.Graphics.Light dirLight = new net.meowsers.Peach.Graphics.Light(net.meowsers.Peach.Structures.LightType.DIRECTIONAL);
        assertTrue(dirLight.isDirectional());
        assertEquals(1, dirLight.getType().getId());

        net.meowsers.Peach.Graphics.Light spotLight = new net.meowsers.Peach.Graphics.Light(net.meowsers.Peach.Structures.LightType.SPOT);
        assertTrue(spotLight.isSpot());
        assertEquals(2, spotLight.getType().getId());
        spotLight.setCutOffDegrees(30.0f);
        spotLight.setOuterCutOffDegrees(45.0f);
        assertTrue(spotLight.getCutOff() > spotLight.getOuterCutOff());
    }

    @Test
    public void testSimpleCustomComponentAuthoring() {
        // Demonstrating how easy it is to code a brand new custom component in 4 lines
        class SpinnerComponent extends Component {
            public float speed = 90.0f;
            @Override
            public void update(float dt) {
                transform.rotation.y += speed * dt;
            }
        }

        GameObject spinnerObj = new GameObject("Spinner");
        SpinnerComponent spinner = spinnerObj.addComponent(new SpinnerComponent());
        spinnerObj.start();
        spinnerObj.update(1.0f);

        assertEquals(90.0f, spinnerObj.transform.rotation.y, 0.001f);
    }

    @Test
    public void testFunctionalBehaviorComponents() {
        // Test lambda-based component without creating any classes
        float[] updatedDt = new float[1];
        boolean[] started = new boolean[1];
        boolean[] destroyed = new boolean[1];

        GameObject entity = new GameObject("LambdaEntity")
                .onStart(() -> started[0] = true)
                .onUpdate(dt -> updatedDt[0] += dt)
                .onDestroy(() -> destroyed[0] = true);

        assertFalse(started[0]);
        entity.start();
        assertTrue(started[0]);

        entity.update(0.5f);
        assertEquals(0.5f, updatedDt[0], 0.001f);

        entity.update(0.5f);
        assertEquals(1.0f, updatedDt[0], 0.001f);

        entity.destroy();
        assertTrue(destroyed[0]);
    }

    @Test
    public void testComponentBehaviorWithGameObjectAccess() {
        GameObject entity = new GameObject("Mover");
        entity.addBehavior((go, dt) -> go.transform.position.x += 10.0f * dt);

        entity.start();
        entity.update(2.0f);

        assertEquals(20.0f, entity.transform.position.x, 0.001f);
    }

    @Test
    public void testComponentCreateFactory() {
        float[] val = new float[1];
        Component c = Component.create(dt -> val[0] += 5.0f * dt);

        GameObject go = new GameObject("FactoryGO", c);
        go.start();
        go.update(1.0f);

        assertEquals(5.0f, val[0], 0.001f);
    }

    @Test
    public void testComponentSelfDestroy() {
        GameObject go = new GameObject("DestroyEntity");
        CustomTestComponent comp = go.addComponent(new CustomTestComponent());
        assertTrue(go.hasComponent(CustomTestComponent.class));

        comp.destroy();
        assertFalse(go.hasComponent(CustomTestComponent.class));
        assertTrue(comp.removedCalled);
    }

    @Test
    public void testMeshRendererComponentConstructors() {
        Mesh quad = Mesh.createQuad(0, 0, 10, 10, Color.Green);
        MeshRendererComponent meshComp = new MeshRendererComponent(quad);
        assertNotNull(meshComp.getModel());
        assertEquals(1, meshComp.getModel().getMeshes().size());

        MeshRendererComponent pathComp = new MeshRendererComponent("src/main/resources/Models/cube.obj");
        assertNotNull(pathComp.getModel());
    }

    @Test
    public void testExistingComponentsUseNewStyle() {
        // CameraComponent direct transform synchronization
        GameObject camGo = new GameObject("CameraGo");
        camGo.transform.position.set(10.0f, 20.0f, 30.0f);
        CameraComponent camComp = camGo.addComponent(new CameraComponent(false, false));
        camGo.start();
        assertEquals(10.0f, camComp.getPosition().x, 0.001f);
        assertEquals(20.0f, camComp.getPosition().y, 0.001f);
        assertEquals(30.0f, camComp.getPosition().z, 0.001f);

        camComp.setPosition(new Vector3f(5.0f, 15.0f, 25.0f));
        assertEquals(5.0f, camGo.transform.position.x, 0.001f);
        assertEquals(15.0f, camGo.transform.position.y, 0.001f);
        assertEquals(25.0f, camGo.transform.position.z, 0.001f);

        // CameraComponent lifecycle hooks
        camComp.setCurrent();
        assertSame(camComp.getCamera(), Renderer.getCamera());
        camComp.setEnabled(false);
        assertNull(Renderer.getCamera());
        camComp.setEnabled(true);
        assertSame(camComp.getCamera(), Renderer.getCamera());
        camGo.destroy();
        assertNull(Renderer.getCamera());

        // LightComponent direct transform synchronization
        GameObject lightGo = new GameObject("LightGo");
        lightGo.transform.position.set(1.0f, 2.0f, 3.0f);
        LightComponent lightComp = lightGo.addComponent(new LightComponent());
        lightGo.start();
        assertEquals(1.0f, lightComp.getPosition().x, 0.001f);
        assertEquals(2.0f, lightComp.getPosition().y, 0.001f);
        assertEquals(3.0f, lightComp.getPosition().z, 0.001f);

        lightComp.setPosition(new Vector3f(4.0f, 5.0f, 6.0f));
        assertEquals(4.0f, lightGo.transform.position.x, 0.001f);

        // LightComponent lifecycle hooks
        lightComp.setCurrent();
        assertSame(lightComp.getLight(), Renderer.getLight());
        lightComp.setEnabled(false);
        assertNull(Renderer.getLight());
        lightComp.setEnabled(true);
        assertSame(lightComp.getLight(), Renderer.getLight());
        lightGo.destroy();
        assertNull(Renderer.getLight());

        // MeshRendererComponent direct transform access and lifecycle
        GameObject meshGo = new GameObject("MeshGo");
        MeshRendererComponent meshComp = meshGo.addComponent(new MeshRendererComponent());
        assertSame(meshGo.transform, meshComp.getTransform());
        assertSame(meshGo.transform, meshComp.transform);
        assertTrue(meshComp.isVisible());
        meshComp.setEnabled(false);
        assertFalse(meshComp.isVisible());
        meshComp.setEnabled(true);
        assertTrue(meshComp.isVisible());
    }

    @Test
    public void testPeachLevelHelpers() {
        PeachLevel level = new PeachLevel() {};
        GameObject go1 = level.createGameObject("Obj1", new CustomTestComponent("levelComp"));
        GameObject go2 = level.createGameObject("Obj2", new CameraComponent(false));

        assertSame(go1, level.findGameObject("Obj1"));
        assertSame(go2, level.findGameObject("Obj2"));
        assertNotNull(level.findComponentOfType(CustomTestComponent.class));
        assertEquals(1, level.findComponentsOfType(CustomTestComponent.class).size());
        assertEquals(1, level.findComponentsOfType(CameraComponent.class).size());
    }

    @Test
    public void testCursorLockingAndCamera() {
        Input.setCursorLocked(false);
        assertFalse(Input.isCursorLocked());

        Input.lockCursor();
        assertTrue(Input.isCursorLocked());

        Input.unlockCursor();
        assertFalse(Input.isCursorLocked());

        Camera cam = new Camera();
        assertTrue(cam.isRightClickToLook());

        cam.setRightClickToLook(false);
        assertFalse(cam.isRightClickToLook());
        assertTrue(Input.isCursorLocked());

        cam.setRightClickToLook(true);
        assertTrue(cam.isRightClickToLook());
        assertFalse(Input.isCursorLocked());
    }
}
