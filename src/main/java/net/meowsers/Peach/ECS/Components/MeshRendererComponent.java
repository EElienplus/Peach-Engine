package net.meowsers.Peach.ECS.Components;

import net.meowsers.Peach.Graphics.Mesh;
import net.meowsers.Peach.Graphics.Model;
import net.meowsers.Peach.Graphics.Renderer;
import net.meowsers.Peach.Graphics.Texture;
import org.joml.Vector3f;

import java.util.Arrays;
import java.util.List;

public class MeshRendererComponent extends BehaviorComponent {

    public Model model;
    private boolean visible = true;

    public MeshRendererComponent() {
        this(new Model());
    }

    public MeshRendererComponent(Model model) {
        this.model = model;
    }

    public MeshRendererComponent(String modelPath) {
        this(new Model(modelPath));
    }

    public MeshRendererComponent(Mesh mesh) {
        this(new Model(mesh));
    }

    public MeshRendererComponent(Model model, Texture... textures) {
        this(model);
        if (textures != null && this.model != null) {
            for (Texture t : textures) {
                if (t != null) this.model.addTexture(t);
            }
        }
    }

    public MeshRendererComponent(String modelPath, Texture... textures) {
        this(new Model(modelPath), textures);
    }

    public MeshRendererComponent(Mesh mesh, Texture... textures) {
        this(new Model(mesh), textures);
    }

    public void setModel(Model model) {
        this.model = model;
    }

    public void setMesh(Mesh mesh) {
        this.model = new Model(mesh);
    }

    public void setModel(String modelPath) {
        this.model = new Model(modelPath);
    }

    public Model getModel() {
        return model;
    }

    public MeshRendererComponent addTexture(Texture texture) {
        if (model != null && texture != null) {
            model.addTexture(texture);
        }
        return this;
    }

    public MeshRendererComponent setTextures(List<Texture> textures) {
        if (model != null) {
            model.setTextures(textures);
        }
        return this;
    }

    public MeshRendererComponent setTextures(Texture... textures) {
        if (model != null) {
            model.setTextures(textures != null ? Arrays.asList(textures) : List.of());
        }
        return this;
    }

    public List<Texture> getTextures() {
        return model != null ? model.getTextures() : List.of();
    }

    public boolean isVisible() {
        return visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    @Override
    public void update(float dt) {
        super.update(dt);
        if (!visible || !isEnabled() || model == null) return;

        TransformComponent t = getTransform();
        Vector3f pos = (t != null && t.position != null) ? t.position : new Vector3f(0.0f);
        Vector3f rot = (t != null && t.rotation != null) ? t.rotation : new Vector3f(0.0f);
        Vector3f sca = (t != null && t.scale != null) ? t.scale : new Vector3f(1.0f);

        Renderer.drawModel(model, model.getTextures(), pos, rot, sca);
    }

    @Override
    public void onEnable() {
        super.onEnable();
        this.visible = true;
    }

    @Override
    public void onDisable() {
        super.onDisable();
        this.visible = false;
    }
}
