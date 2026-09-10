package net.meowsers.Peach.ECS.Components;

import net.meowsers.Peach.GUI.Editor;
import net.meowsers.Peach.Graphics.Mesh;
import net.meowsers.Peach.Graphics.Model;
import net.meowsers.Peach.Graphics.Renderer;
import net.meowsers.Peach.Graphics.Texture;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MeshRendererComponent extends BehaviorComponent {

    public Model model;
    @Editor private String modelPath;
    private boolean visible = true;
    private transient String lastLoadedModelPath;
    @Editor(argument = "Read-Only") private int vertCount;

    private transient Vector3f savedScale = new Vector3f(1.0f, 1.0f, 1.0f);
    private transient Vector3f savedPosition = new Vector3f(0.0f, 0.0f, 0.0f);
    private transient Vector3f savedRotation = new Vector3f(0.0f, 0.0f, 0.0f);
    private transient Matrix4f savedTransform = new Matrix4f().identity();
    private transient List<Texture> savedTextures = new ArrayList<>();

    public MeshRendererComponent() {
        this(new Model());
    }
    public MeshRendererComponent(Model model) {
        this.model = model;
        this.modelPath = model != null ? model.getFilepath() : null;
        this.lastLoadedModelPath = this.modelPath;
        if (model != null) {
            saveModelProperties(model);
        }
    }
    public MeshRendererComponent(String modelPath) {
        this.modelPath = modelPath;
        loadModel();
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
        this.modelPath = modelPath;
        loadModel();
        if (textures != null && this.model != null) {
            for (Texture t : textures) {
                if (t != null) this.model.addTexture(t);
            }
        }
    }
    public MeshRendererComponent(Mesh mesh, Texture... textures) {
        this(new Model(mesh), textures);
    }

    public void setModel(Model model) {
        this.model = model;
        this.modelPath = model != null ? model.getFilepath() : null;
        this.lastLoadedModelPath = this.modelPath;
        if (model != null) {
            saveModelProperties(model);
        }
    }
    public void setModel(String modelPath) {
        this.modelPath = modelPath;
        loadModel();
    }
    public Model getModel() {
        return model;
    }

    public void setModelPath(String modelPath) {
        this.modelPath = modelPath;
        loadModel();
    }
    public String getModelPath() {
        return modelPath;
    }

    public void setMesh(Mesh mesh) {
        this.model = new Model(mesh);
        this.modelPath = null;
        this.lastLoadedModelPath = null;
    }

    private void saveModelProperties(Model m) {
        if (m == null) return;
        if (savedScale == null) savedScale = new Vector3f(1.0f);
        if (savedPosition == null) savedPosition = new Vector3f(0.0f);
        if (savedRotation == null) savedRotation = new Vector3f(0.0f);
        if (savedTransform == null) savedTransform = new Matrix4f().identity();
        if (m.getScale() != null) savedScale.set(m.getScale());
        if (m.getPosition() != null) savedPosition.set(m.getPosition());
        if (m.getRotation() != null) savedRotation.set(m.getRotation());
        if (m.getTransform() != null) savedTransform.set(m.getTransform());
        if (m.getTextures() != null && !m.getTextures().isEmpty()) {
            savedTextures = new ArrayList<>(m.getTextures());
        }
        if(m.getMesh(0) != null) {
            vertCount = m.getMesh(0).getVertexCount();

        }
    }

    private void restoreModelProperties(Model m) {
        if (m == null) return;
        if (savedScale != null) m.setScale(savedScale);
        if (savedPosition != null) m.setPosition(savedPosition);
        if (savedRotation != null) m.setRotation(savedRotation);
        if (savedTransform != null) m.setTransform(savedTransform);
        if ((m.getTextures() == null || m.getTextures().isEmpty()) && savedTextures != null && !savedTextures.isEmpty()) {
            m.setTextures(savedTextures);
        }
    }

    public void loadModel() {
        if (this.modelPath == null || this.modelPath.trim().isEmpty()) {
            this.lastLoadedModelPath = this.modelPath;
            if (this.model != null && this.model.getMeshes() != null && !this.model.getMeshes().isEmpty()) {
                saveModelProperties(this.model);
            }
            this.model = new Model();
            return;
        }

        try {
            String resolved = Model.resolveFilePath(this.modelPath);
            if (resolved != null) {
                File file = new File(resolved);
                if (file.exists() && file.isFile()) {
                    if (this.model != null && this.model.getMeshes() != null && !this.model.getMeshes().isEmpty()) {
                        saveModelProperties(this.model);
                    }
                    Model newModel = new Model(resolved);
                    newModel.setFilepath(this.modelPath);
                    restoreModelProperties(newModel);
                    this.model = newModel;
                    this.lastLoadedModelPath = this.modelPath;
                }
            }
        } catch (Exception e) {
            System.err.println("Failed to load model from path: " + this.modelPath + " (" + e.getMessage() + ")");
        }
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
        if (model != null && model.getMeshes() != null && !model.getMeshes().isEmpty()) {
            saveModelProperties(model);
        }
        if (!Objects.equals(modelPath, lastLoadedModelPath)) {
            loadModel();
        }
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
