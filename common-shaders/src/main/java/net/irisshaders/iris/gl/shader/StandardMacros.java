package net.irisshaders.iris.gl.shader;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.mitchej123.glsm.GLStateManagerService.GL_STATE_MANAGER;
import static net.irisshaders.iris.IrisLogging.IRIS_LOGGER;
import static org.embeddedt.embeddium.compat.mc.MinecraftVersionShimService.MINECRAFT_SHIM;
import static org.embeddedt.embeddium.compat.dh.DHCompatService.DH_COMPAT;

import com.google.common.collect.ImmutableList;
import net.irisshaders.iris.IrisConstants;
import net.irisshaders.iris.helpers.StringPair;
import net.irisshaders.iris.pipeline.WorldRenderingPhase;
import net.irisshaders.iris.texture.format.TextureFormat;
import net.irisshaders.iris.texture.format.TextureFormatLoader;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20C;
import org.lwjgl.opengl.GL30C;

public class StandardMacros {
	private static final Pattern SEMVER_PATTERN = Pattern.compile("(?<major>\\d+)\\.(?<minor>\\d+)\\.*(?<bugfix>\\d*)(.*)");

	private static void define(List<StringPair> defines, String key) {
		defines.add(new StringPair(key, ""));
	}

	private static void define(List<StringPair> defines, String key, String value) {
		defines.add(new StringPair(key, value));
	}

	public static ImmutableList<StringPair> createStandardEnvironmentDefines() {
		ArrayList<StringPair> standardDefines = new ArrayList<>();

		define(standardDefines, "MC_VERSION", getMcVersion());
        define(standardDefines, "MC_MIPMAP_LEVEL", String.valueOf(MINECRAFT_SHIM.getMipmapLevels()));
		define(standardDefines, "MC_GL_VERSION", getGlVersion(GL20C.GL_VERSION));
		define(standardDefines, "MC_GLSL_VERSION", getGlVersion(GL20C.GL_SHADING_LANGUAGE_VERSION));
		define(standardDefines, getOsString());
		define(standardDefines, getVendor());
		define(standardDefines, getRenderer());
		define(standardDefines, "IS_IRIS");
        define(standardDefines, "IRIS_HAS_TRANSLUCENCY_SORTING");
        define(standardDefines, "IRIS_TAG_SUPPORT", "2");


		if (MINECRAFT_SHIM.isModLoaded("distanthorizons") && DH_COMPAT.hasRenderingEnabled()) {
			define(standardDefines, "DISTANT_HORIZONS");
		}

		define(standardDefines, "DH_BLOCK_UNKNOWN", String.valueOf(0));
		define(standardDefines, "DH_BLOCK_LEAVES", String.valueOf(1));
		define(standardDefines, "DH_BLOCK_STONE", String.valueOf(2));
		define(standardDefines, "DH_BLOCK_WOOD", String.valueOf(3));
		define(standardDefines, "DH_BLOCK_METAL", String.valueOf(4));
		define(standardDefines, "DH_BLOCK_DIRT", String.valueOf(5));
		define(standardDefines, "DH_BLOCK_LAVA", String.valueOf(6));
		define(standardDefines, "DH_BLOCK_DEEPSLATE", String.valueOf(7));
		define(standardDefines, "DH_BLOCK_SNOW", String.valueOf(8));
		define(standardDefines, "DH_BLOCK_SAND", String.valueOf(9));
		define(standardDefines, "DH_BLOCK_TERRACOTTA", String.valueOf(10));
		define(standardDefines, "DH_BLOCK_NETHER_STONE", String.valueOf(11));
		define(standardDefines, "DH_BLOCK_WATER", String.valueOf(12));
		define(standardDefines, "DH_BLOCK_GRASS", String.valueOf(13));
		define(standardDefines, "DH_BLOCK_AIR", String.valueOf(14));
		define(standardDefines, "DH_BLOCK_ILLUMINATED", String.valueOf(15));

		for (String glExtension : getGlExtensions()) {
			define(standardDefines, glExtension);
		}

		define(standardDefines, "MC_NORMAL_MAP");
		define(standardDefines, "MC_SPECULAR_MAP");
		define(standardDefines, "MC_RENDER_QUALITY", "1.0");
		define(standardDefines, "MC_SHADOW_QUALITY", "1.0");
		define(standardDefines, "MC_HAND_DEPTH", Float.toString(IrisConstants.DEPTH));

		TextureFormat textureFormat = TextureFormatLoader.getFormat();
		if (textureFormat != null) {
			for (String define : textureFormat.getDefines()) {
				define(standardDefines, define);
			}
		}

		getRenderStages().forEach((stage, index) -> define(standardDefines, stage, index));

		for (String irisDefine : getIrisDefines()) {
			define(standardDefines, irisDefine);
		}

		return ImmutableList.copyOf(standardDefines);
	}

	/**
	 * Gets the current mc version String in a 5 digit format
	 *
	 * @return mc version string
	 * @see <a href="https://github.com/sp614x/optifine/blob/9c6a5b5326558ccc57c6490b66b3be3b2dc8cbef/OptiFineDoc/doc/shaders.txt#L696-L699">Optifine Doc</a>
	 */
	public static String getMcVersion() {
		String version = MINECRAFT_SHIM.getMcVersion();

		if (version == null) {
			throw new IllegalStateException("Could not get the current minecraft version!");
		}

		String[] splitVersion = version.split("\\.");

		if (splitVersion.length < 2) {
			IRIS_LOGGER.error("Could not parse game version \"" + version + "\"");
			splitVersion = MINECRAFT_SHIM.getBackupVersionNumber().split("\\.");
		}

		String major = splitVersion[0];
		String minor = splitVersion[1];
		String bugfix;

		if (splitVersion.length < 3) {
			bugfix = "00";
		} else {
			bugfix = splitVersion[2];
		}

		if (minor.length() == 1) {
			minor = 0 + minor;
		}
		if (bugfix.length() == 1) {
			bugfix = 0 + bugfix;
		}

		return major + minor + bugfix;
	}

	/**
	 * Returns the current GL Version using regex
	 *
	 * @param name the name of the gl attribute to parse
	 * @return current gl version stripped of semantic versioning
	 * @see <a href="https://github.com/sp614x/optifine/blob/9c6a5b5326558ccc57c6490b66b3be3b2dc8cbef/OptiFineDoc/doc/shaders.txt#L701-L703">Optifine Doc for GL Version</a>
	 * @see <a href="https://github.com/sp614x/optifine/blob/9c6a5b5326558ccc57c6490b66b3be3b2dc8cbef/OptiFineDoc/doc/shaders.txt#L705-L707">Optifine Doc for GLSL Version</a>
	 */
	public static String getGlVersion(int name) {
		String info = GL_STATE_MANAGER.glGetString(name);

		Matcher matcher = SEMVER_PATTERN.matcher(Objects.requireNonNull(info));

		if (!matcher.matches()) {
			throw new IllegalStateException("Could not parse GL version from \"" + info + "\"");
		}

		String major = group(matcher, "major");
		String minor = group(matcher, "minor");
		String bugfix = group(matcher, "bugfix");

		if (bugfix == null) {
			// if bugfix is not there, it is 0
			bugfix = "0";
		}

		if (major == null || minor == null) {
			throw new IllegalStateException("Could not parse GL version from \"" + info + "\"");
		}

		return major + minor + bugfix;
	}

	/**
	 * Expanded version of {@link Matcher#group(String)} that does not throw an exception.
	 * If the argument is incorrect (normally resulting in an exception), it returns null
	 *
	 * @param matcher matcher to check the group by
	 * @param name    name of the group
	 * @return the section of the matcher that is a group, or null, if that matcher does not contain said group
	 */
	public static String group(Matcher matcher, String name) {
		try {
			return matcher.group(name);
		} catch (IllegalArgumentException | IllegalStateException exception) {
			return null;
		}
	}

	/**
	 * Returns the current OS String
	 *
	 * @return the string based on the current OS
	 * @see <a href="https://github.com/sp614x/optifine/blob/9c6a5b5326558ccc57c6490b66b3be3b2dc8cbef/OptiFineDoc/doc/shaders.txt#L709-L714">Optifine Doc</a>
	 */
	public static String getOsString() {
        return MINECRAFT_SHIM.getOsString();
	}

	/**
	 * Returns a string indicating the graphics card being used
	 *
	 * @return the graphics card prefixed with "MC_GL_VENDOR_"
	 * @see <a href="https://github.com/sp614x/optifine/blob/9c6a5b5326558ccc57c6490b66b3be3b2dc8cbef/OptiFineDoc/doc/shaders.txt#L716-L723">Optifine Doc</a>
	 */
	public static String getVendor() {
		String vendor = Objects.requireNonNull(GL_STATE_MANAGER.glGetString(GL11.GL_VENDOR).toLowerCase(Locale.ROOT));
		if (vendor.startsWith("ati")) {
			return "MC_GL_VENDOR_ATI";
		} else if (vendor.startsWith("intel")) {
			return "MC_GL_VENDOR_INTEL";
		} else if (vendor.startsWith("nvidia")) {
			return "MC_GL_VENDOR_NVIDIA";
		} else if (vendor.startsWith("amd")) {
			return "MC_GL_VENDOR_AMD";
		} else if (vendor.startsWith("x.org")) {
			return "MC_GL_VENDOR_XORG";
		}
		return "MC_GL_VENDOR_OTHER";
	}

	/**
	 * Returns the graphics driver being used
	 *
	 * @return graphics driver prefixed with "MC_GL_RENDERER_"
	 * @see <a href="https://github.com/sp614x/optifine/blob/9c6a5b5326558ccc57c6490b66b3be3b2dc8cbef/OptiFineDoc/doc/shaders.txt#L725-L733">Optifine Doc</a>
	 */
	public static String getRenderer() {
		String renderer = Objects.requireNonNull(GL_STATE_MANAGER.glGetString(GL11.GL_RENDERER)).toLowerCase(Locale.ROOT);
		if (renderer.startsWith("amd")) {
			return "MC_GL_RENDERER_RADEON";
		} else if (renderer.startsWith("ati")) {
			return "MC_GL_RENDERER_RADEON";
		} else if (renderer.startsWith("radeon")) {
			return "MC_GL_RENDERER_RADEON";
		} else if (renderer.startsWith("gallium")) {
			return "MC_GL_RENDERER_GALLIUM";
		} else if (renderer.startsWith("intel")) {
			return "MC_GL_RENDERER_INTEL";
		} else if (renderer.startsWith("geforce")) {
			return "MC_GL_RENDERER_GEFORCE";
		} else if (renderer.startsWith("nvidia")) {
			return "MC_GL_RENDERER_GEFORCE";
		} else if (renderer.startsWith("quadro")) {
			return "MC_GL_RENDERER_QUADRO";
		} else if (renderer.startsWith("nvs")) {
			return "MC_GL_RENDERER_QUADRO";
		} else if (renderer.startsWith("mesa")) {
			return "MC_GL_RENDERER_MESA";
		}
		return "MC_GL_RENDERER_OTHER";
	}

	/**
	 * Returns the list of currently enabled GL extensions
	 * This is done by calling {@link GL11#glGetString} with the arg {@link GL11#GL_EXTENSIONS}
	 *
	 * @return set of activated extensions prefixed with "MC_"
	 * @see <a href="https://github.com/sp614x/optifine/blob/9c6a5b5326558ccc57c6490b66b3be3b2dc8cbef/OptiFineDoc/doc/shaders.txt#L735-L738">Optifine Doc</a>
	 */
	public static Set<String> getGlExtensions() {
		// In OpenGL Core, we must use a new way of retrieving extensions.
		int numExtensions = GL30C.glGetInteger(GL30C.GL_NUM_EXTENSIONS);

		String[] extensions = new String[numExtensions];

		for (int i = 0; i < numExtensions; i++) {
			extensions[i] = GL30C.glGetStringi(GL30C.GL_EXTENSIONS, i);
		}

		// TODO note that we do not add extensions based on if the shader uses them and if they are supported
		// see https://github.com/sp614x/optifine/blob/master/OptiFineDoc/doc/shaders.txt#L738

		// NB: Use Collectors.toSet(). In some cases, there are duplicate extensions in the extension list.
		// RenderDoc is one example - it causes the GL_KHR_debug extension to appear twice:
		//
		// https://github.com/IrisShaders/Iris/issues/971
		return Arrays.stream(extensions).map(s -> "MC_" + s).collect(Collectors.toSet());
	}


	public static Map<String, String> getRenderStages() {
		Map<String, String> stages = new HashMap<>();
		for (WorldRenderingPhase phase : WorldRenderingPhase.values()) {
			stages.put("MC_RENDER_STAGE_" + phase.name(), String.valueOf(phase.ordinal()));
		}
		return stages;
	}

	/**
	 * Returns the list of Iris-exclusive uniforms supported in the current version of Iris.
	 *
	 * @return List of definitions corresponding to the uniform names prefixed with "MC_"
	 */
	public static List<String> getIrisDefines() {
		List<String> defines = new ArrayList<>();
		// All Iris-exclusive uniforms should have a corresponding definition here. Example:
		// defines.add("MC_UNIFORM_DRAGON_DEATH_PROGRESS");

		return defines;
	}
}
