package net.jrbudda.builder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;


import java.util.Map;

import net.citizensnpcs.api.jnbt.ByteArrayTag;
import net.citizensnpcs.api.jnbt.CompoundTag;
import net.citizensnpcs.api.jnbt.IntTag;
import net.citizensnpcs.api.jnbt.NBTInputStream;
import net.citizensnpcs.api.jnbt.ShortTag;
import net.citizensnpcs.api.jnbt.StringTag;
import net.citizensnpcs.api.jnbt.Tag;

import org.bukkit.material.MaterialData;
import org.bukkit.util.Vector;



public class MCEditSchematicFormat {
	//private static final int MAX_SIZE = Short.MAX_VALUE - Short.MIN_VALUE;

	public static  BuilderSchematic load(File path, String filename) throws IOException, Exception {

		File file = new File(path,filename+".schematic");

		if(!file.exists()) throw(new java.io.FileNotFoundException("File not found"));

		FileInputStream stream = new FileInputStream(file);
		NBTInputStream nbtStream = new NBTInputStream(new java.util.zip.GZIPInputStream(stream));

		Vector origin = new Vector();
	//	Vector offset = new Vector();

		// Schematic tag
		CompoundTag schematicTag = (CompoundTag) nbtStream.readTag();
		nbtStream.close();

		if (!schematicTag.getName().equals("Schematic")) {
			throw new Exception("Tag \"Schematic\" does not exist or is not first");
		}

		// Check
		Map<String, Tag> schematic = schematicTag.getValue();

		if (!schematic.containsKey("Blocks")) {
			throw new Exception("Schematic file is missing a \"Blocks\" tag");
		}

		// Get information
		short width = getChildTag(schematic, "Width", ShortTag.class).getValue();
		short length = getChildTag(schematic, "Length", ShortTag.class).getValue();
		short height = getChildTag(schematic, "Height", ShortTag.class).getValue();

		try {
			int originX = getChildTag(schematic, "WEOriginX", IntTag.class).getValue();
			int originY = getChildTag(schematic, "WEOriginY", IntTag.class).getValue();
			int originZ = getChildTag(schematic, "WEOriginZ", IntTag.class).getValue();
			origin = new org.bukkit.util.Vector(originX, originY, originZ);
		} catch (Exception e) {
			// No origin data
		}

		try {
		//	int offsetX = getChildTag(schematic, "WEOffsetX", IntTag.class).getValue();
		//	int offsetY = getChildTag(schematic, "WEOffsetY", IntTag.class).getValue();
		//	int offsetZ = getChildTag(schematic, "WEOffsetZ", IntTag.class).getValue();
		//	offset = new Vector(offsetX, offsetY, offsetZ);
		} catch (Exception e) {
			// No offset data
		}

		// Check type of Schematic
		String materials = getChildTag(schematic, "Materials", StringTag.class).getValue();
		if (!materials.equals("Alpha")) {
			throw new Exception("Schematic file is not an Alpha schematic");
		}

		// Get blocks
		byte[] rawBlocks = getChildTag(schematic, "Blocks", ByteArrayTag.class).getValue();
		byte[] blockData = getChildTag(schematic, "Data", ByteArrayTag.class).getValue();
		short[] blocks = new short[rawBlocks.length];

		if (schematic.containsKey("AddBlocks")) {
			byte[] addBlockIds = getChildTag(schematic, "AddBlocks", ByteArrayTag.class).getValue();
			for (int i = 0, index = 0; i < addBlockIds.length && index < blocks.length; ++i) {
				blocks[index] = (short) (((addBlockIds[i] >> 4) << 8) + (rawBlocks[index++] & 0xFF));
				if (index < blocks.length) {
					blocks[index] = (short) (((addBlockIds[i] & 0xF) << 8) + (rawBlocks[index++] & 0xFF));
				}
			}
		} else {
			for (int i = 0; i < rawBlocks.length; ++i) {
				blocks[i] = (short) (rawBlocks[i] & 0xFF);
			}
		}

		//		// Need to pull out tile entities
		//		List<Tag> tileEntities = getChildTag(schematic, "TileEntities", ListTag.class).getValue();
		//		Map<Vector, Map<String, Tag>> tileEntitiesMap =
		//				new HashMap<Vector, Map<String, Tag>>();
		//
		//		for (Tag tag : tileEntities) {
		//			if (!(tag instanceof CompoundTag)) continue;
		//			CompoundTag t = (CompoundTag) tag;
		//
		//			int x = 0;
		//			int y = 0;
		//			int z = 0;
		//
		//			Map<String, Tag> values = new HashMap<String, Tag>();
		//
		//			for (Map.Entry<String, Tag> entry : t.getValue().entrySet()) {
		//				if (entry.getKey().equals("x")) {
		//					if (entry.getValue() instanceof IntTag) {
		//						x = ((IntTag) entry.getValue()).getValue();
		//					}
		//				} else if (entry.getKey().equals("y")) {
		//					if (entry.getValue() instanceof IntTag) {
		//						y = ((IntTag) entry.getValue()).getValue();
		//					}
		//				} else if (entry.getKey().equals("z")) {
		//					if (entry.getValue() instanceof IntTag) {
		//						z = ((IntTag) entry.getValue()).getValue();
		//					}
		//				}
		//
		//				values.put(entry.getKey(), entry.getValue());
		//			}
		//
		//			Vector vec = new Vector(x, y, z);
		//			tileEntitiesMap.put(vec, values);
		//		}

		//	Vector size = new Vector(width, height, length);

		//        clipboard.setOrigin(origin);
		//        clipboard.setOffset(offset);

		BuilderSchematic out = new BuilderSchematic(width, height,length);

		for (int x = 0; x < width; ++x) {
			for (int y = 0; y < height; ++y) {
				for (int z = 0; z < length; ++z) {
					int index = y * width * length + z * width + x;
					//Vector pt = new Vector(x, y, z);

					BuildBlock M = new BuildBlock();
					M.mat  = new MaterialData(blocks[index], blockData[index]);
						M.X = x;
						M.Y = y;
						M.Z = z;
					out.Blocks[x][y][z] = M;

					//					if (block instanceof TileEntityBlock && tileEntitiesMap.containsKey(pt)) {
					//						((TileEntityBlock) block).setNbtData(new CompoundTag("", tileEntitiesMap.get(pt)));
					//					}


				}
			}
		}
		out.Name = filename;
		out.SchematicOrigin = origin;
		return out;
	}




	/**
	 * Get child tag of a NBT structure.
	 *
	 * @param items The parent tag map
	 * @param key The name of the tag to get
	 * @param expected The expected type of the tag
	 * @return child tag casted to the expected type
	 * @throws DataException if the tag does not exist or the tag is not of the expected type
	 */
	private static <T extends Tag> T getChildTag(Map<String, Tag> items, String key,
			Class<T> expected) throws Exception {

		if (!items.containsKey(key)) {
			throw new Exception("Schematic file is missing a \"" + key + "\" tag");
		}
		Tag tag = items.get(key);
		if (!expected.isInstance(tag)) {
			throw new Exception(
					key + " tag is not of tag type " + expected.getName());
		}
		return expected.cast(tag);
	}
}