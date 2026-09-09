package net.meowsers.Peach.ECS;

import net.meowsers.Peach.ECS.Components.LightComponent;
import net.meowsers.Peach.ECS.Components.TransformComponent;
import net.meowsers.Peach.GameEngine.PeachLevel;
import net.meowsers.Peach.Graphics.Light;
import net.meowsers.Peach.Graphics.Renderer;
import net.meowsers.Peach.Structures.Color;
import net.meowsers.Peach.Structures.LightType;
import org.joml.Vector3f;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class LightSystemTest {

    private static class TestLevel extends PeachLevel {
    }

    @BeforeEach
    public void setup() {
        Renderer.clearLights();
    }

    @Test
    public void testMultipleLightsIndependence() {
        TestLevel level = new TestLevel();

        GameObject go1 = new GameObject("Light1");
        LightComponent lightComp1 = new LightComponent();
        go1.addComponent(lightComp1);
        level.addGameObject(go1);

        GameObject go2 = new GameObject("Light2");
        LightComponent lightComp2 = new LightComponent();
        go2.addComponent(lightComp2);
        level.addGameObject(go2);

        assertEquals(2, Renderer.getLights().size(), "Both lights should be registered in Renderer");
        assertNotSame(lightComp1.getLight(), lightComp2.getLight(), "Each LightComponent should have its own Light instance");

        // Set different positions and colors
        lightComp1.setPosition(new Vector3f(1.0f, 2.0f, 3.0f));
        lightComp1.setColor(Color.Red);

        lightComp2.setPosition(new Vector3f(10.0f, 20.0f, 30.0f));
        lightComp2.setColor(Color.Blue);

        // Verify that lightComp1 was not modified by lightComp2
        assertEquals(new Vector3f(1.0f, 2.0f, 3.0f), lightComp1.getPosition());
        assertEquals(new Vector3f(10.0f, 20.0f, 30.0f), lightComp2.getPosition());

        assertEquals(new Color(1.0f, 0.0f, 0.0f, 1.0f), lightComp1.getColor());
        assertEquals(new Color(0.0f, 0.0f, 1.0f, 1.0f), lightComp2.getColor());

        // Verify that Color.White and Color.Red singletons were not mutated
        assertEquals(1.0f, Color.White.r);
        assertEquals(1.0f, Color.White.g);
        assertEquals(1.0f, Color.White.b);

        // Verify Renderer sees both independent lights
        assertEquals(new Vector3f(1.0f, 2.0f, 3.0f), Renderer.getLights().get(0).getPosition());
        assertEquals(new Vector3f(10.0f, 20.0f, 30.0f), Renderer.getLights().get(1).getPosition());
    }

    @Test
    public void testLightRemovalOnComponentRemove() {
        TestLevel level = new TestLevel();

        GameObject go = new GameObject("LightGo");
        LightComponent lightComp = new LightComponent();
        go.addComponent(lightComp);
        level.addGameObject(go);

        assertEquals(1, Renderer.getLights().size());
        assertTrue(Renderer.getLights().contains(lightComp.getLight()));

        go.removeComponent(LightComponent.class);

        assertEquals(0, Renderer.getLights().size(), "Light should be removed from Renderer when component is removed");
    }

    @Test
    public void testLightRemovalOnGameObjectDestroy() {
        TestLevel level = new TestLevel();

        GameObject go = new GameObject("LightGo");
        LightComponent lightComp = new LightComponent();
        go.addComponent(lightComp);
        level.addGameObject(go);

        assertEquals(1, Renderer.getLights().size());

        level.removeGameObject(go);

        assertEquals(0, Renderer.getLights().size(), "Light should be removed from Renderer when GameObject is destroyed/removed");
    }

    @Test
    public void testTransformAndLightSync() {
        TestLevel level = new TestLevel();

        GameObject go = new GameObject("SyncedLight");
        go.transform.position.set(5.0f, 15.0f, -7.0f);

        LightComponent lightComp = new LightComponent();
        go.addComponent(lightComp);
        level.addGameObject(go);

        go.update(0.016f);

        // Light position should have synced from GameObject's transform
        assertEquals(new Vector3f(5.0f, 15.0f, -7.0f), lightComp.getPosition());
        assertEquals(new Vector3f(5.0f, 15.0f, -7.0f), lightComp.getLight().getPosition());

        // Changing transform changes light
        go.transform.position.set(2.0f, 4.0f, 6.0f);
        go.update(0.016f);

        assertEquals(new Vector3f(2.0f, 4.0f, 6.0f), lightComp.getPosition());
        assertEquals(new Vector3f(2.0f, 4.0f, 6.0f), lightComp.getLight().getPosition());
    }

    @Test
    public void testLightEnableDisableAndCurrentToggle() {
        TestLevel level = new TestLevel();

        GameObject go = new GameObject("ToggleLight");
        LightComponent lightComp = new LightComponent();
        go.addComponent(lightComp);
        level.addGameObject(go);

        assertEquals(1, Renderer.getLights().size());

        // Disable component
        lightComp.setEnabled(false);
        assertEquals(0, Renderer.getLights().size(), "Disabled light component should remove its light from Renderer");

        // Re-enable component
        lightComp.setEnabled(true);
        assertEquals(1, Renderer.getLights().size(), "Re-enabled light component should re-add its light to Renderer");

        // Toggle isCurrent
        lightComp.setCurrent(false);
        assertEquals(0, Renderer.getLights().size(), "isCurrent=false should remove light from Renderer");

        lightComp.setCurrent(true);
        assertEquals(1, Renderer.getLights().size(), "isCurrent=true should re-add light to Renderer");
    }

    @Test
    public void testMultipleLightTypesAndProperties() {
        TestLevel level = new TestLevel();

        GameObject pLight = new GameObject("PointLight");
        LightComponent pComp = new LightComponent(LightType.POINT);
        pComp.setIntensity(2.5f);
        pLight.addComponent(pComp);
        level.addGameObject(pLight);

        GameObject dLight = new GameObject("DirLight");
        LightComponent dComp = new LightComponent(LightType.DIRECTIONAL);
        dComp.setDirection(new Vector3f(0, -1, 0));
        dComp.setIntensity(1.0f);
        dLight.addComponent(dComp);
        level.addGameObject(dLight);

        GameObject sLight = new GameObject("SpotLight");
        LightComponent sComp = new LightComponent(LightType.SPOT);
        sComp.setCutOffDegrees(15.0f);
        sComp.setOuterCutOffDegrees(20.0f);
        sLight.addComponent(sComp);
        level.addGameObject(sLight);

        assertEquals(3, Renderer.getLights().size());
        assertEquals(LightType.POINT, pComp.getLight().getType());
        assertEquals(LightType.DIRECTIONAL, dComp.getLight().getType());
        assertEquals(LightType.SPOT, sComp.getLight().getType());

        assertEquals(2.5f, pComp.getLight().getIntensity());
        assertEquals(1.0f, dComp.getLight().getIntensity());
    }
}
