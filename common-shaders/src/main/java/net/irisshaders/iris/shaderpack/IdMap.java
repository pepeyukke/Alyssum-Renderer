package net.irisshaders.iris.shaderpack;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2IntFunction;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntMaps;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectArrayMap;
import net.irisshaders.iris.IrisCommon;
import net.irisshaders.iris.helpers.StringPair;
import net.irisshaders.iris.pipeline.transform.ShaderPrinter;
import net.irisshaders.iris.shaderpack.materialmap.BlockRenderType;
import net.irisshaders.iris.shaderpack.materialmap.LegacyIdMap;
import net.irisshaders.iris.shaderpack.materialmap.NamespacedId;
import net.irisshaders.iris.shaderpack.option.OrderBackedProperties;
import net.irisshaders.iris.shaderpack.option.ShaderPackOptions;
import net.irisshaders.iris.shaderpack.preprocessor.PropertiesPreprocessor;
import org.embeddedt.embeddium.compat.iris.IBlockEntry;
import org.taumc.celeritas.CeleritasShaderVersionService;


import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.*;

import static net.irisshaders.iris.IrisLogging.IRIS_LOGGER;
import static org.embeddedt.embeddium.compat.mc.PlatformUtilService.PLATFORM_UTIL;

/**
 * A utility class for parsing entries in item.properties, block.properties, and entities.properties files in shaderpacks
 */
public class IdMap {
	/**
	 * Maps a given item ID to an integer ID
	 */
	private final Object2IntMap<NamespacedId> itemIdMap;

	/**
	 * Maps a given entity ID to an integer ID
	 */
	private final Object2IntMap<NamespacedId> entityIdMap;

	/**
	 * Maps block states to block ids defined in block.properties
	 */
	private Int2ObjectMap<List<IBlockEntry>> blockPropertiesMap;

	/**
	 * A set of render type overrides for specific blocks. Allows shader packs to move blocks to different render types.
	 */
	private Map<NamespacedId, BlockRenderType> blockRenderTypeMap;

	IdMap(Path shaderPath, ShaderPackOptions shaderPackOptions, Iterable<StringPair> environmentDefines) {
		itemIdMap = loadProperties(shaderPath, "item.properties", shaderPackOptions, environmentDefines)
			.map(IdMap::parseItemIdMap).orElse(Object2IntMaps.emptyMap());

		entityIdMap = loadProperties(shaderPath, "entity.properties", shaderPackOptions, environmentDefines)
			.map(IdMap::parseEntityIdMap).orElse(Object2IntMaps.emptyMap());

		loadProperties(shaderPath, "block.properties", shaderPackOptions, environmentDefines).ifPresent(blockProperties -> {
			blockPropertiesMap = parseBlockMap(blockProperties, "block.", "block.properties");
			blockRenderTypeMap = parseRenderTypeMap(blockProperties, "layer.", "block.properties");
		});

		// TODO: Properly override block render layers

		if (blockPropertiesMap == null) {
			// Fill in with default values...
			blockPropertiesMap = new Int2ObjectOpenHashMap<>();
			LegacyIdMap.addLegacyValues(blockPropertiesMap);
		}

		if (blockRenderTypeMap == null) {
			blockRenderTypeMap = Collections.emptyMap();
		}
	}

	/**
	 * Loads properties from a properties file in a shaderpack path
	 */
	private static Optional<Properties> loadProperties(Path shaderPath, String name, ShaderPackOptions shaderPackOptions,
													   Iterable<StringPair> environmentDefines) {
		String fileContents = readProperties(shaderPath, name);
		if (fileContents == null) {
			return Optional.empty();
		}

		// TODO: This is the worst code I have ever made. Do not do this.
		String processed = PropertiesPreprocessor.preprocessSource(fileContents, shaderPackOptions, environmentDefines).replaceAll("\\\\\\n\\s*\\n", " ").replaceAll("\\S\s*block\\.", "\nblock.");

		StringReader propertiesReader = new StringReader(processed);

		// Note: ordering of properties is significant
		// See https://github.com/IrisShaders/Iris/issues/1327 and the relevant putIfAbsent calls in
		// BlockMaterialMapping
		Properties properties = new OrderBackedProperties();
		try {
			properties.load(propertiesReader);
		} catch (IOException e) {
			IRIS_LOGGER.error("Error loading " + name + " at " + shaderPath, e);

			return Optional.empty();
		}

		if (IrisCommon.getIrisConfig().areDebugOptionsEnabled()) {
			ShaderPrinter.deleteIfClearing();
			try (OutputStream os = Files.newOutputStream(PLATFORM_UTIL.getGameDir().resolve("patched_shaders").resolve(name))) {
				properties.store(new OutputStreamWriter(os, StandardCharsets.UTF_8), "Patched version of properties");
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}

		return Optional.of(properties);
	}

	private static String readProperties(Path shaderPath, String name) {
		try {
			// ID maps should be encoded in ISO_8859_1.
			return Files.readString(shaderPath.resolve(name), StandardCharsets.ISO_8859_1);
		} catch (NoSuchFileException e) {
			IRIS_LOGGER.debug("An " + name + " file was not found in the current shaderpack");

			return null;
		} catch (IOException e) {
			IRIS_LOGGER.error("An IOException occurred reading " + name + " from the current shaderpack", e);

			return null;
		}
	}

	private static Object2IntMap<NamespacedId> parseItemIdMap(Properties properties) {
		return parseIdMap(properties, "item.", "item.properties");
	}

	private static Object2IntMap<NamespacedId> parseEntityIdMap(Properties properties) {
		return parseIdMap(properties, "entity.", "entity.properties");
	}

	/**
	 * Parses a NamespacedId map in OptiFine format
	 */
	private static Object2IntMap<NamespacedId> parseIdMap(Properties properties, String keyPrefix, String fileName) {
		Object2IntMap<NamespacedId> idMap = new Object2IntOpenHashMap<>();
		idMap.defaultReturnValue(-1);

		properties.forEach((keyObject, valueObject) -> {
			String key = (String) keyObject;
			String value = (String) valueObject;

			if (!key.startsWith(keyPrefix)) {
				// Not a valid line, ignore it
				return;
			}

			int intId;

			try {
				intId = Integer.parseInt(key.substring(keyPrefix.length()));
			} catch (NumberFormatException e) {
				// Not a valid property line
				IRIS_LOGGER.warn("Failed to parse line in " + fileName + ": invalid key " + key);
				return;
			}

			// Split on any whitespace
			for (String part : value.split("\\s+")) {
				if (part.contains("=")) {
					// Avoid tons of logspam for now
					IRIS_LOGGER.warn("Failed to parse an ResourceLocation in " + fileName + " for the key " + key + ": state properties are currently not supported: " + part);
					continue;
				}

				// Note: NamespacedId performs no validation on the content. That will need to be done by whatever is
				//       converting these things to ResourceLocations.
				idMap.put(new NamespacedId(part), intId);
			}
		});

		return Object2IntMaps.unmodifiable(idMap);
	}

	private static Int2ObjectMap<List<IBlockEntry>> parseBlockMap(Properties properties, String keyPrefix, String fileName) {
		Int2ObjectMap<List<IBlockEntry>> entriesById = new Int2ObjectOpenHashMap<>();

		properties.forEach((keyObject, valueObject) -> {
			String key = (String) keyObject;
			String value = (String) valueObject;

			if (!key.startsWith(keyPrefix)) {
				// Not a valid line, ignore it
				return;
			}

			int intId;

			try {
				intId = Integer.parseInt(key.substring(keyPrefix.length()));
			} catch (NumberFormatException e) {
				// Not a valid property line
				IRIS_LOGGER.warn("Failed to parse line in " + fileName + ": invalid key " + key);
				return;
			}

			List<IBlockEntry> entries = new ArrayList<>();

			// Split on whitespace groups, not just single spaces
			for (String part : value.split("\\s+")) {
				if (part.isEmpty()) {
					continue;
				}

				try {
					entries.add(CeleritasShaderVersionService.INSTANCE.parseBlockEntry(part));
				} catch (Exception e) {
					IRIS_LOGGER.warn("Unexpected error while parsing an entry from " + fileName + " for the key " + key + ":", e);
				}
			}

            // Place tag entries after regular entries
            entries.sort(Comparator.comparingInt(e -> e.isTag() ? 1 : 0));

			entriesById.put(intId, Collections.unmodifiableList(entries));
		});

		return Int2ObjectMaps.unmodifiable(entriesById);
	}

	/**
	 * Parses a render layer map.
	 * <p>
	 * This feature is used by Chocapic v9 and Wisdom Shaders. Otherwise, it is a rarely-used feature.
	 */
	private static Map<NamespacedId, BlockRenderType> parseRenderTypeMap(Properties properties, String keyPrefix, String fileName) {
		Map<NamespacedId, BlockRenderType> overrides = new HashMap<>();

		properties.forEach((keyObject, valueObject) -> {
			String key = (String) keyObject;
			String value = (String) valueObject;

			if (!key.startsWith(keyPrefix)) {
				// Not a valid line, ignore it
				return;
			}

			// Note: We have to remove the prefix "layer." because fromString expects "cutout", not "layer.cutout".
			String keyWithoutPrefix = key.substring(keyPrefix.length());

			BlockRenderType renderType = BlockRenderType.fromString(keyWithoutPrefix).orElse(null);

			if (renderType == null) {
				IRIS_LOGGER.warn("Failed to parse line in " + fileName + ": invalid block render type: " + key);
				return;
			}

			for (String part : value.split("\\s+")) {
				// Note: NamespacedId performs no validation on the content. That will need to be done by whatever is
				//       converting these things to ResourceLocations.
				overrides.put(new NamespacedId(part), renderType);
			}
		});

		return overrides;
	}


	private static Map<NamespacedId, String> parseDimensionMap(Properties properties, String keyPrefix, String fileName) {
		Map<NamespacedId, String> overrides = new Object2ObjectArrayMap<>();

		properties.forEach((keyObject, valueObject) -> {
			String key = (String) keyObject;
			String value = (String) valueObject;

			if (!key.startsWith(keyPrefix)) {
				// Not a valid line, ignore it
				return;
			}

			key = key.substring(keyPrefix.length());

			for (String part : value.split("\\s+")) {
				overrides.put(new NamespacedId(part), key);
			}
		});

		return overrides;
	}

	public Int2ObjectMap<List<IBlockEntry>> getBlockProperties() {
		return blockPropertiesMap;
	}

	public Object2IntFunction<NamespacedId> getItemIdMap() {
		return itemIdMap;
	}

	public Object2IntFunction<NamespacedId> getEntityIdMap() {
		return entityIdMap;
	}

	public Map<NamespacedId, BlockRenderType> getBlockRenderTypeMap() {
		return blockRenderTypeMap;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) {
			return true;
		}

		if (o == null || getClass() != o.getClass()) {
			return false;
		}

		IdMap idMap = (IdMap) o;

		return Objects.equals(itemIdMap, idMap.itemIdMap)
			&& Objects.equals(entityIdMap, idMap.entityIdMap)
			&& Objects.equals(blockPropertiesMap, idMap.blockPropertiesMap)
			&& Objects.equals(blockRenderTypeMap, idMap.blockRenderTypeMap);
	}

	@Override
	public int hashCode() {
		return Objects.hash(itemIdMap, entityIdMap, blockPropertiesMap, blockRenderTypeMap);
	}
}
