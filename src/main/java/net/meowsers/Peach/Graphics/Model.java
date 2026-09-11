package net.meowsers.Peach.Graphics;

import net.meowsers.Peach.Utils.Disposable;
import net.meowsers.Peach.Structures.Color;
import net.meowsers.Peach.Structures.Vertex;
import net.meowsers.Peach.Utils.PeachException;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.PointerBuffer;
import org.lwjgl.assimp.*;

import java.io.File;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import static org.lwjgl.assimp.Assimp.*;

public class Model implements Disposable {
    private String filepath;
    private String name = "Model";
    private List<Mesh> meshes = new ArrayList<>();
    private List<Texture> textures = new ArrayList<>();

    private Matrix4f transform = new Matrix4f().identity();
    private transient Matrix4f modelMatrix = new Matrix4f();
    private Vector3f position = new Vector3f(0, 0, 0);
    private Vector3f rotation = new Vector3f(0, 0, 0); // In degrees or radians
    private Vector3f scale = new Vector3f(1, 1, 1);

    public static final int DEFAULT_FLAGS =
            aiProcess_Triangulate |
                    aiProcess_GenSmoothNormals |
                    aiProcess_FlipUVs |
                    aiProcess_JoinIdenticalVertices |
                    aiProcess_CalcTangentSpace |
                    aiProcess_ImproveCacheLocality |
                    aiProcess_SortByPType;

    public Model() {
    }

    public Model(String filepath) {
        this(filepath, DEFAULT_FLAGS);
    }

    public Model(String filepath, int flags) {
        this.filepath = filepath;
        loadModel(filepath, flags);
    }

    public Model(List<Mesh> meshes) {
        if (meshes != null) {
            for (Mesh m : meshes) {
                addMesh(m);
            }
        }
    }

    public Model(Mesh... meshes) {
        if (meshes != null) {
            for (Mesh m : meshes) {
                addMesh(m);
            }
        }
    }

    public static Model load(String filepath) {
        return new Model(filepath);
    }

    public static Model load(String filepath, int flags) {
        return new Model(filepath, flags);
    }

    private void loadModel(String path, int flags) {
        String resolvedPath = resolveFilePath(path);
        File file = new File(resolvedPath);
        this.name = file.getName().replaceFirst("\\.[^.]+$", "");

        AIScene scene = aiImportFile(resolvedPath, flags);
        if (scene == null || scene.mRootNode() == null || (scene.mFlags() & AI_SCENE_FLAGS_INCOMPLETE) != 0) {
            int fallbackFlags = aiProcess_Triangulate | aiProcess_SortByPType | aiProcess_FlipUVs | aiProcess_GenSmoothNormals;
            if (flags != fallbackFlags) {
                scene = aiImportFile(resolvedPath, fallbackFlags);
            }
            if (scene == null || scene.mRootNode() == null || (scene.mFlags() & AI_SCENE_FLAGS_INCOMPLETE) != 0) {
                scene = aiImportFile(resolvedPath, aiProcess_Triangulate | aiProcess_SortByPType);
            }
            if (scene == null || scene.mRootNode() == null || (scene.mFlags() & AI_SCENE_FLAGS_INCOMPLETE) != 0) {
                String error = aiGetErrorString();
                throw new PeachException("Failed to load 3D model with Assimp: " + path + " (resolved to: " + resolvedPath + ")\nReason: " + error);
            }
        }

        File parentDir = file.getParentFile();
        List<Texture> materialTextures = loadMaterials(scene, parentDir);

        processNode(scene.mRootNode(), scene, new Matrix4f().identity(), materialTextures);

        aiFreeScene(scene);
    }

    private List<Texture> loadMaterials(AIScene scene, File parentDir) {
        List<Texture> loadedMaterialTextures = new ArrayList<>();
        int numMaterials = scene.mNumMaterials();
        PointerBuffer materials = scene.mMaterials();

        if (materials != null) {
            for (int i = 0; i < numMaterials; i++) {
                AIMaterial material = AIMaterial.create(materials.get(i));
                Texture diffuseTex = loadMaterialTexture(material, aiTextureType_DIFFUSE, parentDir);
                loadedMaterialTextures.add(diffuseTex);
                if (diffuseTex != null && !textures.contains(diffuseTex)) {
                    textures.add(diffuseTex);
                }
            }
        }
        return loadedMaterialTextures;
    }

    private Texture loadMaterialTexture(AIMaterial material, int textureType, File parentDir) {
        AIString path = AIString.calloc();
        try {
            int result = aiGetMaterialTexture(material, textureType, 0, path, (IntBuffer) null, null, null, null, null, null);
            if (result == aiReturn_SUCCESS) {
                String texPath = path.dataString();
                if (texPath != null && !texPath.trim().isEmpty()) {
                    String resolvedTexPath = resolveTexturePath(texPath, parentDir);
                    if (resolvedTexPath != null) {
                        try {
                            return new Texture(resolvedTexPath);
                        } catch (Exception e) {
                            System.err.println("[Peach] Warning: Failed to load texture '" + texPath + "' for model: " + e.getMessage());
                        }
                    }
                }
            }
        } finally {
            path.free();
        }
        return null;
    }

    private String resolveTexturePath(String texPath, File parentDir) {
        String cleanPath = texPath.replace('\\', '/');

        File direct = new File(cleanPath);
        if (direct.exists()) return direct.getAbsolutePath();

        if (parentDir != null) {
            File relFile = new File(parentDir, cleanPath);
            if (relFile.exists()) return relFile.getAbsolutePath();

            File filenameOnly = new File(parentDir, new File(cleanPath).getName());
            if (filenameOnly.exists()) return filenameOnly.getAbsolutePath();
        }

        File resTex = new File("src/main/resources/Textures/" + new File(cleanPath).getName());
        if (resTex.exists()) return resTex.getAbsolutePath();

        File resDir = new File("src/main/resources/" + cleanPath);
        if (resDir.exists()) return resDir.getAbsolutePath();

        return cleanPath;
    }

    private void processNode(AINode node, AIScene scene, Matrix4f parentTransform, List<Texture> materialTextures) {
        Matrix4f nodeTransform = toMatrix4f(node.mTransformation());
        Matrix4f currentTransform = new Matrix4f(parentTransform).mul(nodeTransform);

        int numMeshes = node.mNumMeshes();
        IntBuffer meshIndices = node.mMeshes();
        PointerBuffer sceneMeshes = scene.mMeshes();

        if (meshIndices != null && sceneMeshes != null) {
            for (int i = 0; i < numMeshes; i++) {
                int meshIndex = meshIndices.get(i);
                AIMesh aiMesh = AIMesh.create(sceneMeshes.get(meshIndex));
                Mesh mesh = processMesh(aiMesh, currentTransform, materialTextures);
                this.meshes.add(mesh);
            }
        }

        int numChildren = node.mNumChildren();
        PointerBuffer children = node.mChildren();
        if (children != null) {
            for (int i = 0; i < numChildren; i++) {
                AINode childNode = AINode.create(children.get(i));
                processNode(childNode, scene, currentTransform, materialTextures);
            }
        }
    }

    private Mesh processMesh(AIMesh aiMesh, Matrix4f transform, List<Texture> materialTextures) {
        Mesh mesh = new Mesh(aiMesh.mName().dataString());

        int numVertices = aiMesh.mNumVertices();
        AIVector3D.Buffer verticesBuffer = aiMesh.mVertices();
        AIVector3D.Buffer normalsBuffer = aiMesh.mNormals();
        AIVector3D.Buffer texCoordsBuffer = aiMesh.mTextureCoords(0);
        AIColor4D.Buffer colorsBuffer = aiMesh.mColors(0);

        List<Vertex> vertexList = new ArrayList<>(numVertices);
        for (int i = 0; i < numVertices; i++) {
            AIVector3D pos = verticesBuffer.get(i);

            float nx = 0.0f, ny = 0.0f, nz = 1.0f;
            if (normalsBuffer != null) {
                AIVector3D norm = normalsBuffer.get(i);
                nx = norm.x();
                ny = norm.y();
                nz = norm.z();
            }

            float u = 0.0f;
            float v = 0.0f;
            if (texCoordsBuffer != null) {
                AIVector3D texCoord = texCoordsBuffer.get(i);
                u = texCoord.x();
                v = texCoord.y();
            }

            Color color = Color.White;
            if (colorsBuffer != null) {
                AIColor4D col = colorsBuffer.get(i);
                color = new Color(col.r(), col.g(), col.b(), col.a());
            }

            Vertex vertex = new Vertex(pos.x(), pos.y(), pos.z(), nx, ny, nz, color.r, color.g, color.b, color.a, u, v);
            vertexList.add(vertex);
        }

        int numFaces = aiMesh.mNumFaces();
        AIFace.Buffer facesBuffer = aiMesh.mFaces();
        List<Integer> indexList = new ArrayList<>(numFaces * 3);

        for (int i = 0; i < numFaces; i++) {
            AIFace face = facesBuffer.get(i);
            int numIndices = face.mNumIndices();
            IntBuffer indices = face.mIndices();
            for (int j = 0; j < numIndices; j++) {
                indexList.add(indices.get(j));
            }
        }

        mesh.add(vertexList, indexList);
        if (normalsBuffer == null) {
            mesh.calculateNormals();
        }

        if (transform != null) {
            mesh.transform(transform);
        }

        int materialIndex = aiMesh.mMaterialIndex();
        if (materialTextures != null && materialIndex >= 0 && materialIndex < materialTextures.size()) {
            mesh.setTexture(materialTextures.get(materialIndex));
        }

        return mesh;
    }

    private static Matrix4f toMatrix4f(AIMatrix4x4 aiMat) {
        return new Matrix4f(
                aiMat.a1(), aiMat.b1(), aiMat.c1(), aiMat.d1(),
                aiMat.a2(), aiMat.b2(), aiMat.c2(), aiMat.d2(),
                aiMat.a3(), aiMat.b3(), aiMat.c3(), aiMat.d3(),
                aiMat.a4(), aiMat.b4(), aiMat.c4(), aiMat.d4()
        );
    }

    public static String resolveFilePath(String filePath) {
        if (filePath == null) return null;

        String expanded = filePath.trim();
        while ((expanded.startsWith("\"") && expanded.endsWith("\"")) ||
                (expanded.startsWith("'") && expanded.endsWith("'")) ||
                (expanded.startsWith("“") && expanded.endsWith("”")) ||
                (expanded.startsWith("‘") && expanded.endsWith("’"))) {
            if (expanded.length() <= 2) return null;
            expanded = expanded.substring(1, expanded.length() - 1).trim();
        }
        if (expanded.isEmpty()) return null;

        if (expanded.startsWith("file://")) {
            expanded = expanded.substring(7);
        } else if (expanded.startsWith("file:")) {
            expanded = expanded.substring(5);
        }

        expanded = expanded.replace('\\', '/').replaceAll("/+", "/");

        String userHome = System.getProperty("user.home", "");
        if (expanded.startsWith("~")) {
            expanded = userHome + expanded.substring(1);
        }

        String fileName = new File(expanded).getName();
        String withoutLeadingSlash = expanded.startsWith("/") ? expanded.substring(1) : expanded;

        String[] candidateBases = new String[] {
                expanded,
                withoutLeadingSlash,
                fileName,
                "src/main/resources/" + withoutLeadingSlash,
                "src/main/resources/Models/" + withoutLeadingSlash,
                "src/main/resources/Models/" + fileName,
                "src/main/resources/" + fileName,
                "assets/" + withoutLeadingSlash,
                "assets/Models/" + withoutLeadingSlash,
                withoutLeadingSlash.replaceFirst("^assets/", "src/main/resources/"),
                withoutLeadingSlash.replaceFirst("^resources/", "src/main/resources/"),
                withoutLeadingSlash.replaceFirst("^main/resources/", "src/main/resources/"),
                userHome + "/Desktop/" + withoutLeadingSlash,
                userHome + "/Desktop/" + fileName,
                userHome + "/Downloads/" + withoutLeadingSlash,
                userHome + "/Downloads/" + fileName,
                userHome + "/Documents/" + withoutLeadingSlash,
                userHome + "/Documents/" + fileName
        };

        String[] extensions = new String[] { "", ".obj", ".gltf", ".glb", ".fbx", ".dae", ".stl", ".ply", ".3ds" };

        for (String base : candidateBases) {
            if (base == null || base.isEmpty()) continue;
            for (String ext : extensions) {
                String fullPath = base.endsWith(ext) ? base : base + ext;
                File f = new File(fullPath);
                if (f.exists() && f.isFile()) {
                    return f.getAbsolutePath();
                }
            }
        }

        File modelsDir = new File("src/main/resources/Models");
        if (modelsDir.exists() && modelsDir.isDirectory()) {
            File[] files = modelsDir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.isFile()) {
                        String nameWithoutExt = f.getName().replaceFirst("\\.[^.]+$", "");
                        if (f.getName().equalsIgnoreCase(fileName) ||
                                nameWithoutExt.equalsIgnoreCase(fileName) ||
                                f.getName().equalsIgnoreCase(expanded) ||
                                nameWithoutExt.equalsIgnoreCase(expanded)) {
                            return f.getAbsolutePath();
                        }
                    }
                }
            }
        }

        File resDir = new File("src/main/resources");
        if (resDir.exists() && resDir.isDirectory()) {
            File found = findFileRecursive(resDir, fileName);
            if (found != null) {
                return found.getAbsolutePath();
            }
        }

        File assetsDir = new File("assets");
        if (assetsDir.exists() && assetsDir.isDirectory()) {
            File found = findFileRecursive(assetsDir, fileName);
            if (found != null) {
                return found.getAbsolutePath();
            }
        }

        File srcDir = new File("src");
        if (srcDir.exists() && srcDir.isDirectory()) {
            File found = findFileRecursive(srcDir, fileName);
            if (found != null) {
                return found.getAbsolutePath();
            }
        }

        File rootDir = new File(".");
        File foundInRoot = findFileRecursive(rootDir, fileName);
        if (foundInRoot != null) {
            return foundInRoot.getAbsolutePath();
        }

        File f = new File(expanded);
        if (f.exists() && f.isFile()) {
            return f.getAbsolutePath();
        }

        return expanded;
    }

    private static File findFileRecursive(File dir, String targetName) {
        if (dir == null || !dir.isDirectory() || targetName == null || targetName.isEmpty()) return null;
        String dirName = dir.getName();
        if (dirName.startsWith(".") || dirName.equals("build") || dirName.equals(".gradle") ||
                dirName.equals(".git") || dirName.equals(".idea") || dirName.equals("gradle") ||
                dirName.equals("out") || dirName.equals("target")) {
            return null;
        }
        File[] files = dir.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (f.isFile()) {
                String nameWithoutExt = f.getName().replaceFirst("\\.[^.]+$", "");
                if (f.getName().equalsIgnoreCase(targetName) || nameWithoutExt.equalsIgnoreCase(targetName)) {
                    return f;
                }
            } else if (f.isDirectory()) {
                File found = findFileRecursive(f, targetName);
                if (found != null) return found;
            }
        }
        return null;
    }

    public List<Mesh> getMeshes() {
        return meshes;
    }

    public Mesh getMesh(int index) {
        if (index >= 0 && index < meshes.size()) {
            return meshes.get(index);
        }
        return null;
    }

    public int getMeshCount() {
        return meshes.size();
    }

    public void addMesh(Mesh mesh) {
        if (mesh != null) {
            meshes.add(mesh);
            if (mesh.getTextures() != null && !mesh.getTextures().isEmpty()) {
                for (Texture t : mesh.getTextures()) {
                    if (t != null && !textures.contains(t)) {
                        textures.add(t);
                    }
                }
            } else if (mesh.getTexture() != null && !textures.contains(mesh.getTexture())) {
                textures.add(mesh.getTexture());
            }
        }
    }

    public void addMeshes(Collection<Mesh> meshes) {
        if (meshes != null) {
            for (Mesh m : meshes) {
                addMesh(m);
            }
        }
    }

    public List<Texture> getTextures() {
        return textures;
    }

    public void setTextures(List<Texture> newTextures) {
        this.textures.clear();
        if (newTextures != null) {
            this.textures.addAll(newTextures);
            for (int i = 0; i < meshes.size(); i++) {
                if (i < newTextures.size()) {
                    meshes.get(i).setTexture(newTextures.get(i));
                }
            }
        }
    }

    public void setTextures(Texture... newTextures) {
        setTextures(newTextures != null ? Arrays.asList(newTextures) : null);
    }

    public void setTexture(Texture texture) {
        this.textures.clear();
        if (texture != null) {
            this.textures.add(texture);
        }
        for (Mesh mesh : meshes) {
            mesh.setTexture(texture);
        }
    }

    public void setTexture(int index, Texture texture) {
        if (index >= 0) {
            while (this.textures.size() <= index) {
                this.textures.add(null);
            }
            this.textures.set(index, texture);
            if (index < meshes.size() && meshes.get(index) != null) {
                meshes.get(index).setTexture(texture);
            }
        }
    }

    public void addTexture(Texture texture) {
        if (texture != null && !this.textures.contains(texture)) {
            this.textures.add(texture);
        }
    }

    public void addTextures(Texture... textures) {
        if (textures != null) {
            for (Texture t : textures) {
                addTexture(t);
            }
        }
    }

    public void addTextures(Collection<Texture> textures) {
        if (textures != null) {
            for (Texture t : textures) {
                addTexture(t);
            }
        }
    }

    public Texture getTexture(int index) {
        if (index >= 0 && index < textures.size()) {
            return textures.get(index);
        }
        return !textures.isEmpty() ? textures.get(0) : null;
    }

    public Texture getTexture() {
        return !textures.isEmpty() ? textures.get(0) : null;
    }

    public int getTextureCount() {
        return textures.size();
    }

    public boolean hasTextures() {
        return !textures.isEmpty();
    }

    public Mesh toMesh() {
        Mesh combined = new Mesh(name + "_Combined");
        for (Mesh m : meshes) {
            combined.add(m, getModelMatrix());
        }
        return combined;
    }

    public Mesh toCombinedMesh() {
        return toMesh();
    }

    public Matrix4f getModelMatrix() {
        modelMatrix.identity();
        modelMatrix.translate(position);
        modelMatrix.rotate((float) Math.toRadians(rotation.x), 1, 0, 0);
        modelMatrix.rotate((float) Math.toRadians(rotation.y), 0, 1, 0);
        modelMatrix.rotate((float) Math.toRadians(rotation.z), 0, 0, 1);
        modelMatrix.scale(scale);
        modelMatrix.mul(transform);
        return modelMatrix;
    }

    public String getFilepath() {
        return filepath;
    }

    public void setFilepath(String filepath) {
        this.filepath = filepath;
    }

    public void setMeshes(List<Mesh> meshes) {
        this.meshes = meshes != null ? new ArrayList<>(meshes) : new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Matrix4f getTransform() {
        return transform;
    }

    public void setTransform(Matrix4f newTransform) {
        if (newTransform != null) {
            this.transform.set(newTransform);
        }
    }

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f pos) {
        if (pos != null) this.position.set(pos);
    }

    public Vector3f getRotation() {
        return rotation;
    }

    public void setRotation(Vector3f rot) {
        if (rot != null) this.rotation.set(rot);
    }

    public Vector3f getScale() {
        return scale;
    }

    public void setScale(float s) {
        this.scale.set(s, s, s);
    }

    public void setScale(Vector3f s) {
        if (s != null) this.scale.set(s);
    }

    public Vector3f getMinBounds() {
        if (meshes.isEmpty()) return new Vector3f(0, 0, 0);
        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;
        for (Mesh mesh : meshes) {
            Vector3f min = mesh.getMinBounds();
            if (min.x < minX) minX = min.x;
            if (min.y < minY) minY = min.y;
            if (min.z < minZ) minZ = min.z;
        }
        return new Vector3f(minX, minY, minZ);
    }

    public Vector3f getMaxBounds() {
        if (meshes.isEmpty()) return new Vector3f(0, 0, 0);
        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;
        for (Mesh mesh : meshes) {
            Vector3f max = mesh.getMaxBounds();
            if (max.x > maxX) maxX = max.x;
            if (max.y > maxY) maxY = max.y;
            if (max.z > maxZ) maxZ = max.z;
        }
        return new Vector3f(maxX, maxY, maxZ);
    }

    public Vector3f getCenter() {
        if (meshes.isEmpty()) return new Vector3f(0, 0, 0);
        Vector3f min = getMinBounds();
        Vector3f max = getMaxBounds();
        return new Vector3f((min.x + max.x) * 0.5f, (min.y + max.y) * 0.5f, (min.z + max.z) * 0.5f);
    }

    public Vector3f getSize() {
        if (meshes.isEmpty()) return new Vector3f(0, 0, 0);
        Vector3f min = getMinBounds();
        Vector3f max = getMaxBounds();
        return new Vector3f(max.x - min.x, max.y - min.y, max.z - min.z);
    }

    public Model center() {
        if (meshes.isEmpty()) return this;
        Vector3f c = getCenter();
        for (Mesh m : meshes) {
            m.translate(new Vector3f(-c.x, -c.y, -c.z));
        }
        return this;
    }

    public Model normalize() {
        return fitToSize(1.0f);
    }

    public Model fitToSize(float targetSize) {
        if (meshes.isEmpty()) return this;
        center();
        Vector3f size = getSize();
        float maxDim = Math.max(size.x, Math.max(size.y, size.z));
        if (maxDim > 0.00001f) {
            float s = targetSize / maxDim;
            for (Mesh m : meshes) {
                m.scale(s);
            }
        }
        return this;
    }

    public Model cullBackfaces(Vector3f cameraPos) {
        List<Mesh> culledMeshes = new ArrayList<>();
        Matrix4f modelMat = getModelMatrix();
        for (Mesh m : meshes) {
            culledMeshes.add(m.cullBackfaces(cameraPos, modelMat));
        }
        Model culled = new Model(culledMeshes);
        culled.setPosition(this.position);
        culled.setRotation(this.rotation);
        culled.setScale(this.scale);
        culled.setTextures(this.textures);
        return culled;
    }

    public Model cullNormals(Vector3f viewDir) {
        List<Mesh> culledMeshes = new ArrayList<>();
        Matrix4f modelMat = getModelMatrix();
        for (Mesh m : meshes) {
            culledMeshes.add(m.cullNormals(viewDir, modelMat));
        }
        Model culled = new Model(culledMeshes);
        culled.setPosition(this.position);
        culled.setRotation(this.rotation);
        culled.setScale(this.scale);
        culled.setTextures(this.textures);
        return culled;
    }

    @Override
    public void dispose() {
        for (Texture t : textures) {
            if (t != null) {
                t.dispose();
            }
        }
        textures.clear();
        meshes.clear();
    }

    public void destroy() {
        dispose();
    }
}