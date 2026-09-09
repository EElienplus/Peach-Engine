package net.meowsers.Peach.Graphics;

import net.meowsers.Peach.Utils.PeachException;
import net.meowsers.Peach.Utils.SlangManager;
import org.joml.Matrix4f;
import org.lwjgl.BufferUtils;
import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.FloatBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.lwjgl.opengl.GL20.*;

public class Shader {
    private final int shaderProgramId;
    private boolean beingUsed = false;
    private final FloatBuffer matBuffer = BufferUtils.createFloatBuffer(16);
    private final java.util.Map<String, Integer> uniformLocationCache = new java.util.HashMap<>();

    /**
     * Single Slang file constructor. Compiles vertex and fragment stages
     * from one .slang file by entry point name.
     */
    public Shader(String slangFilePath, String vertEntryPoint, String fragEntryPoint) {
        String vertexSource = compileSlangToGLSL(slangFilePath, vertEntryPoint, "vertex");
        String fragmentSource = compileSlangToGLSL(slangFilePath, fragEntryPoint, "fragment");

        this.shaderProgramId = createAndLinkProgram(vertexSource, fragmentSource, slangFilePath, slangFilePath);
    }

    /**
     * Two-file constructor for distinct vertex/fragment Slang files.
     */
    public Shader(String vertexFilePath, String fragmentFilePath) {
        String vertexSource = compileSlangToGLSL(vertexFilePath, "vertMain", "vertex");
        String fragmentSource = compileSlangToGLSL(fragmentFilePath, "fragMain", "fragment");

        this.shaderProgramId = createAndLinkProgram(vertexSource, fragmentSource, vertexFilePath, fragmentFilePath);
    }

    private int createAndLinkProgram(String vertexSource, String fragmentSource, String vertPath, String fragPath) {
        int vertexID = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexID, vertexSource);
        glCompileShader(vertexID);
        if (glGetShaderi(vertexID, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new PeachException("Vertex shader compilation failed (" + vertPath + "):\n" + glGetShaderInfoLog(vertexID) + "\nSource:\n" + vertexSource);
        }

        int fragmentID = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentID, fragmentSource);
        glCompileShader(fragmentID);
        if (glGetShaderi(fragmentID, GL_COMPILE_STATUS) == GL_FALSE) {
            throw new PeachException("Fragment shader compilation failed (" + fragPath + "):\n" + glGetShaderInfoLog(fragmentID) + "\nSource:\n" + fragmentSource);
        }

        int programId = glCreateProgram();
        glAttachShader(programId, vertexID);
        glAttachShader(programId, fragmentID);
        glLinkProgram(programId);

        if (glGetProgrami(programId, GL_LINK_STATUS) == GL_FALSE) {
            throw new PeachException("Shader program linking failed:\n" + glGetProgramInfoLog(programId));
        }

        glDeleteShader(vertexID);
        glDeleteShader(fragmentID);

        return programId;
    }

    private String resolveFilePath(String filePath) {
        File file = new File(filePath);
        if (file.exists()) {
            return file.getAbsolutePath();
        }

        File resFile = new File("src/main/resources/" + filePath);
        if (resFile.exists()) {
            return resFile.getAbsolutePath();
        }

        File altRes = new File(filePath.replaceFirst("^assets/", "src/main/resources/"));
        if (altRes.exists()) {
            return altRes.getAbsolutePath();
        }

        return filePath;
    }

    private String getSlangcExecutable() {
        return SlangManager.getSlangcPath();
    }

    private String compileSlangToGLSL(String filePath, String entryPoint, String stage) {
        String resolvedPath = resolveFilePath(filePath);
        String cacheKey = getShaderCacheKey(resolvedPath, entryPoint, stage);

        try {
            String slangcPath = getSlangcExecutable();
            File slangcFile = new File(slangcPath);
            File binDir = slangcFile.getParentFile();
            File libDir = binDir != null && binDir.getParentFile() != null ? new File(binDir.getParentFile(), "lib") : null;

            ProcessBuilder pb = new ProcessBuilder(
                    slangcPath,
                    resolvedPath,
                    "-target", "glsl",
                    "-entry", entryPoint,
                    "-stage", stage,
                    "-o", "-"
            );

            if (binDir != null && binDir.exists()) {
                java.util.Map<String, String> env = pb.environment();
                String path = env.getOrDefault("PATH", "");
                env.put("PATH", binDir.getAbsolutePath() + File.pathSeparator + path);
                if (libDir != null && libDir.exists()) {
                    String ldPath = env.getOrDefault("LD_LIBRARY_PATH", "");
                    env.put("LD_LIBRARY_PATH", libDir.getAbsolutePath() + (ldPath.isEmpty() ? "" : File.pathSeparator + ldPath));
                    String dyldPath = env.getOrDefault("DYLD_LIBRARY_PATH", "");
                    env.put("DYLD_LIBRARY_PATH", libDir.getAbsolutePath() + (dyldPath.isEmpty() ? "" : File.pathSeparator + dyldPath));
                }
            }

            Process process = pb.start();

            String glslSource;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
                glslSource = reader.lines().collect(Collectors.joining("\n"));
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                try (BufferedReader errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream(), StandardCharsets.UTF_8))) {
                    String errors = errorReader.lines().collect(Collectors.joining("\n"));
                    throw new PeachException("Slang compilation error for (" + filePath + "):\n" + errors);
                }
            }

            String postProcessed = postProcessGLSL(glslSource, entryPoint, stage);
            saveToCache(cacheKey, postProcessed);
            return postProcessed;
        } catch (Exception e) {
            String cached = loadFromCacheOrFallback(cacheKey, filePath, stage);
            if (cached != null) {
                System.out.println(net.meowsers.Peach.Utils.ConsoleColors.YELLOW + "[Peach] Using cached GLSL fallback for " + filePath + " (" + stage + ")" + net.meowsers.Peach.Utils.ConsoleColors.RESET);
                return cached;
            }
            if (e instanceof PeachException) {
                throw (PeachException) e;
            }
            throw new PeachException("Failed to compile Slang shader " + filePath + "\nEnsure Slang compiler is available or network access is enabled for auto-download.\nError: " + e.getMessage(), e);
        }
    }

    private static String getShaderCacheKey(String filePath, String entryPoint, String stage) {
        File f = new File(filePath);
        String name = f.getName().replaceFirst("\\.[^.]+$", "");
        return name + "_" + entryPoint + "_" + stage;
    }

    private static void saveToCache(String cacheKey, String glsl) {
        try {
            File cacheDir = new File(".peach" + File.separator + "shader-cache");
            if (!cacheDir.exists()) cacheDir.mkdirs();
            File cacheFile = new File(cacheDir, cacheKey + ".glsl");
            Files.writeString(cacheFile.toPath(), glsl, StandardCharsets.UTF_8);

            // Also save to resources cache if running in project development directory
            File resCacheDir = new File("src/main/resources/Shaders/cache");
            if (resCacheDir.exists() || new File("src/main/resources/Shaders").exists()) {
                resCacheDir.mkdirs();
                File resCacheFile = new File(resCacheDir, cacheKey + ".glsl");
                Files.writeString(resCacheFile.toPath(), glsl, StandardCharsets.UTF_8);
            }
        } catch (Exception ignored) {
        }
    }

    private static String loadFromCacheOrFallback(String cacheKey, String originalPath, String stage) {
        try {
            // 1. Check .peach/shader-cache/
            File cacheFile = new File(".peach" + File.separator + "shader-cache" + File.separator + cacheKey + ".glsl");
            if (cacheFile.exists() && cacheFile.isFile()) {
                return Files.readString(cacheFile.toPath(), StandardCharsets.UTF_8);
            }

            // 2. Check workspace resources
            File resCacheFile = new File("src/main/resources/Shaders/cache/" + cacheKey + ".glsl");
            if (resCacheFile.exists() && resCacheFile.isFile()) {
                return Files.readString(resCacheFile.toPath(), StandardCharsets.UTF_8);
            }

            // 3. Check bundled classpath resource fallback
            String resourcePath = "Shaders/cache/" + cacheKey + ".glsl";
            try (InputStream is = Shader.class.getClassLoader().getResourceAsStream(resourcePath)) {
                if (is != null) {
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                        return reader.lines().collect(Collectors.joining("\n"));
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String postProcessGLSL(String source, String entryPoint, String stage) {
        // Adapt shader version for OpenGL 4.1 core profile
        String glsl = source.replaceFirst("#version\\s+\\d+.*", "#version 410 core");

        // Remove row_major layout qualifiers and buffer qualifiers not supported in GLSL 410
        glsl = glsl.replaceAll("layout\\(row_major\\)\\s*uniform;", "");
        glsl = glsl.replaceAll("layout\\(row_major\\)\\s*buffer;", "");
        glsl = glsl.replaceAll("layout\\(binding\\s*=\\s*\\d+\\)", "");

        // Convert Slang uniform blocks to standard GLSL uniforms for direct glUniform access
        Pattern blockPattern = Pattern.compile("(?s)layout\\(std140\\)\\s*uniform\\s+(block_\\w+)\\s*\\{([^}]+)\\}\\s*(\\w+);");
        Matcher blockMatcher = blockPattern.matcher(glsl);
        StringBuffer sb = new StringBuffer();
        java.util.Map<String, String> uniformReplacements = new java.util.HashMap<>();

        while (blockMatcher.find()) {
            String blockBody = blockMatcher.group(2);
            String instanceName = blockMatcher.group(3);

            StringBuilder uniformsDecl = new StringBuilder();
            String[] lines = blockBody.split(";");
            for (String line : lines) {
                line = line.trim();
                if (line.isEmpty()) continue;
                String normalized = line.replaceAll("mat4x4", "mat4");
                // Remove trailing _0 on variable declaration if present (including array brackets like [16])
                Matcher varMatcher = Pattern.compile("(\\w+)\\s+(\\w+?)(_0)?(\\s*\\[\\s*\\d+\\s*\\])?$").matcher(normalized);
                if (varMatcher.find()) {
                    String type = varMatcher.group(1);
                    String rawName = varMatcher.group(2);
                    String cleanName = rawName.replaceAll("_0$", "");
                    String arrayPart = varMatcher.group(4) != null ? varMatcher.group(4).replaceAll("\\s+", "") : "";
                    uniformsDecl.append("uniform ").append(type).append(" ").append(cleanName).append(arrayPart).append(";\n");

                    // Map e.g. Uniforms_0.uProjection_0 -> uProjection
                    uniformReplacements.put(instanceName + "\\." + rawName + "(?:_0)?", cleanName);
                }
            }

            blockMatcher.appendReplacement(sb, Matcher.quoteReplacement(uniformsDecl.toString()));
        }
        blockMatcher.appendTail(sb);
        glsl = sb.toString();

        // Apply specific uniform instance replacements
        for (java.util.Map.Entry<String, String> entry : uniformReplacements.entrySet()) {
            glsl = glsl.replaceAll("\\b" + entry.getKey() + "\\b", entry.getValue());
        }

        // Remove SLANG_ParameterGroup structs if present
        glsl = glsl.replaceAll("(?s)struct\\s+SLANG_ParameterGroup_\\w+\\s*\\{[^}]+\\};", "");
        glsl = glsl.replaceAll("(?s)struct\\s+GlobalParams_\\w+\\s*\\{[^}]+\\};", "");

        // Replace texture array naming e.g. uTextures_0 -> uTextures
        glsl = glsl.replaceAll("\\buTextures_0\\b", "uTextures");

        // Match varyings between vertex stage and fragment stage
        if ("vertex".equalsIgnoreCase(stage)) {
            glsl = glsl.replaceAll("entryPointParam_" + Pattern.quote(entryPoint) + "_", "v2f_");
            glsl = glsl.replaceAll("entryPointParam_\\w+?_", "v2f_");
        } else if ("fragment".equalsIgnoreCase(stage)) {
            glsl = glsl.replaceAll("\\binput_", "v2f_");
        }

        return glsl;
    }

    public void use() {
        if (!beingUsed) {
            glUseProgram(shaderProgramId);
            beingUsed = true;
        }
    }

    public void detach() {
        glUseProgram(0);
        beingUsed = false;
    }

    private int getUniformLocation(String varName) {
        Integer cached = uniformLocationCache.get(varName);
        if (cached != null) {
            return cached;
        }
        int varLocation = glGetUniformLocation(shaderProgramId, varName);
        if (varLocation == -1) {
            varLocation = glGetUniformLocation(shaderProgramId, varName + "_0");
        }
        uniformLocationCache.put(varName, varLocation);
        return varLocation;
    }

    public void uploadMatrix4f(String varName, Matrix4f mat4) {
        int varLocation = getUniformLocation(varName);
        use();
        matBuffer.clear();
        mat4.get(matBuffer);
        glUniformMatrix4fv(varLocation, true, matBuffer);
    }

    public void uploadIntArray(String varName, int[] array) {
        int varLocation = getUniformLocation(varName);
        use();
        glUniform1iv(varLocation, array);
    }

    public void uploadVec3f(String varName, org.joml.Vector3f vec) {
        if (vec == null) return;
        uploadVec3f(varName, vec.x, vec.y, vec.z);
    }

    public void uploadVec3f(String varName, float x, float y, float z) {
        int varLocation = getUniformLocation(varName);
        use();
        glUniform3f(varLocation, x, y, z);
    }

    public void uploadVec4f(String varName, org.joml.Vector4f vec) {
        if (vec == null) return;
        uploadVec4f(varName, vec.x, vec.y, vec.z, vec.w);
    }

    public void uploadVec4f(String varName, float x, float y, float z, float w) {
        int varLocation = getUniformLocation(varName);
        use();
        glUniform4f(varLocation, x, y, z, w);
    }

    public void uploadFloat(String varName, float value) {
        int varLocation = getUniformLocation(varName);
        use();
        glUniform1f(varLocation, value);
    }

    public void uploadInt(String varName, int value) {
        int varLocation = getUniformLocation(varName);
        use();
        glUniform1i(varLocation, value);
    }

    public void uploadBoolean(String varName, boolean value) {
        uploadInt(varName, value ? 1 : 0);
    }
}