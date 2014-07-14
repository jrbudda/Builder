package net.jrbudda.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.Random;

import net.citizensnpcs.api.jnbt.ByteArrayTag;
import net.citizensnpcs.api.jnbt.ByteTag;
import net.citizensnpcs.api.jnbt.CompoundTag;
import net.citizensnpcs.api.jnbt.DoubleTag;
import net.citizensnpcs.api.jnbt.EndTag;
import net.citizensnpcs.api.jnbt.FloatTag;
import net.citizensnpcs.api.jnbt.IntArrayTag;
import net.citizensnpcs.api.jnbt.IntTag;
import net.citizensnpcs.api.jnbt.ListTag;
import net.citizensnpcs.api.jnbt.LongTag;
import net.citizensnpcs.api.jnbt.ShortTag;
import net.citizensnpcs.api.jnbt.StringTag;
import net.citizensnpcs.api.jnbt.Tag;
import net.jrbudda.builder.Builder.supplymap;
////
import net.minecraft.server.v1_7_R4.Block;
import net.minecraft.server.v1_7_R4.Item;
import net.minecraft.server.v1_7_R4.LocaleI18n;
import net.minecraft.server.v1_7_R4.NBTBase;
import net.minecraft.server.v1_7_R4.NBTTagByte;
import net.minecraft.server.v1_7_R4.NBTTagByteArray;
import net.minecraft.server.v1_7_R4.NBTTagCompound;
import net.minecraft.server.v1_7_R4.NBTTagDouble;
import net.minecraft.server.v1_7_R4.NBTTagEnd;
import net.minecraft.server.v1_7_R4.NBTTagFloat;
import net.minecraft.server.v1_7_R4.NBTTagInt;
import net.minecraft.server.v1_7_R4.NBTTagIntArray;
import net.minecraft.server.v1_7_R4.NBTTagList;
import net.minecraft.server.v1_7_R4.NBTTagLong;
import net.minecraft.server.v1_7_R4.NBTTagShort;
import net.minecraft.server.v1_7_R4.NBTTagString;
////


import org.bukkit.ChatColor;
import org.bukkit.material.MaterialData;


public class Util {

	static MaterialData Air = new MaterialData(0,(byte) 0);
	
	public static String printList(Map<Integer, Double> map){
		StringBuilder sb = new StringBuilder();

		java.util.Iterator<Entry<Integer, Double>> it = map.entrySet().iterator();

		while (it.hasNext()){
			Entry<Integer, Double> i = it.next();
			if(i.getValue() > 0){
				sb.append(ChatColor.GREEN + getLocalItemName(i.getKey()) + ":" +ChatColor.WHITE+ i.getValue().intValue());
				if(it.hasNext())sb.append(", ");
			}	
		}
		return sb.toString();
	}


	public static String getLocalItemName(int MatId){
		try {
			if (MatId==0) return  "Air";
			if(MatId < 256){
				Block b =Block.getById(MatId);
				if ( MatId == 3)  return LocaleI18n.get("tile.dirt.default.name");
				if ( MatId == 12)  return LocaleI18n.get("tile.sand.default.name");
				if ( MatId == 37)  return LocaleI18n.get("tile.flower1.dandelion.name");
				if ( MatId == 38)  return LocaleI18n.get("tile.flower2.poppy.name");
				if ( MatId == 6)  return LocaleI18n.get("tile.sapling.oak.name");
				if ( MatId == 21)  return LocaleI18n.get("item.dyePowder.blue.name");
				if ( MatId == 44)  return LocaleI18n.get("tile.stoneSlab.stone.name");
				if ( MatId == 126)  return LocaleI18n.get("tile.stoneSlab.wood.name");
				if ( MatId == 126)  return LocaleI18n.get("tile.stoneSlab.wood.name");
				return	b.getName();
			}
			else{
				Item b =Item.getById(MatId);
				return LocaleI18n.get(b.getName() + ".name");
			}
		} catch (Exception e) {
			return ((Integer)MatId).toString();
		}
	}

	public static List<EmptyBuildBlock> spiralPrintLayer(int starty,int ylayers, EmptyBuildBlock[][][] a, boolean reverse)
	{
		int i, k = 0, l = 0;

		int m = a.length;
		int n = a[0].length;
		int o = a[0][0].length;

		List<EmptyBuildBlock> out = new ArrayList<EmptyBuildBlock>();

		/*  k - starting row index
	        m - ending row index
	        l - starting column index
	        n - ending column index
	        i - iterator
		 */

		while (k < m && l < o)
		{
			/* Print the first row from the remaining rows */
			for (i = l; i < o; ++i)
			{


				if (reverse){
					for(int y=starty;y<starty+ylayers;y++){
						if (y<n)out.add(a[k][y][i]);
					}	
				}
				else {
					for(int y=starty+ylayers-1;y>=starty;y--){
						if (y<n)out.add(a[k][y][i]);
					}	
				}

			}
			k++;

			/* Print the last column from the remaining columns */
			for (i = k; i < m; ++i)
			{

				if (reverse){
					for(int y=starty;y<starty+ylayers;y++){
						if (y<n)out.add(a[i][y][o-1]);
					}	
				}
				else {
					for(int y=starty+ylayers-1;y>=starty;y--){
						if (y<n)out.add(a[i][y][o-1]);
					}	
				}

			}
			o--;

			/* Print the last row from the remaining rows */
			if ( k < m)
			{
				for (i = o-1; i >= l; --i)
				{

					if (reverse){
						for(int y=starty;y<starty+ylayers;y++){
							if (y<n)out.add(a[m-1][y][i]);
						}	
					}
					else {
						for(int y=starty+ylayers-1;y>=starty;y--){
							if (y<n)out.add(a[m-1][y][i]);
						}	
					}


				}
				m--;
			}

			/* Print the first column from the remaining columns */
			if (l < o)
			{
				for (i = m-1; i >= k; --i)
				{

					if (reverse){
						for(int y=starty;y<starty+ylayers;y++){
							if (y<n) out.add(a[i][y][l]);
						}	
					}
					else {
						for(int y=starty+ylayers-1;y>=starty;y--){
							if (y<n) out.add(a[i][y][l]);
						}	
					}


				}
				l++;   
			}       
		}


		if (!reverse) java.util.Collections.reverse(out);
		return out;
	}



	public static List<EmptyBuildBlock> LinearPrintLayer(int starty,int ylayers, EmptyBuildBlock[][][] a, boolean reverse)
	{
		int i = 0,k = 0;
		int di = 1;
		int dk=1;

		int m = a.length;
		int n = a[0].length;
		int o = a[0][0].length;

		List<EmptyBuildBlock> out = new ArrayList<EmptyBuildBlock>();

		/*  k - starting row index
	        m - ending row index
	        l - starting column index
	        n - ending column index
	        i - iterator
		 */

		do{

			if (!reverse){
				for(int y=starty;y<starty+ylayers;y++){
					if (y<n) out.add(a[i][y][k]);
				}	
			}
			else {
				for(int y=starty+ylayers-1;y>=starty;y--){
					if (y<n) out.add(a[i][y][k]);
				}	
			}

			i+=di;
			if(i >=m || i < 0) {
				di*=-1;
				i+=di;
				k+=dk;
				if (k >= o||k<0) {
					k +=1;		
					if(k>=o) break;
				}		
			}


		}while(true);

		if (reverse) java.util.Collections.reverse(out);
		return out;
	}

	static Random R = new java.util.Random();

	public static Map<Integer, Double> MaterialsList(Queue<EmptyBuildBlock> Q) throws Exception{

		Map<Integer, Double> out = new HashMap<Integer, Double>();

		do{

			EmptyBuildBlock b = Q.poll();

			if (b==null) break;
			int item = b.getMat().getItemTypeId();
			double addamt = 1;

			
			if(Builder.SupplyMapping.containsKey(item)){
				supplymap i = Builder.SupplyMapping.get(item);
				item = i.require;
				addamt = i.amount;				
			}		
			else{
				item =(Integer) (Block.getById(item) !=null ? Item.getId(Block.getById(item).getDropType(b.getMat().getData(), R, -10000)) : item);
			}
			
//			if (RequireUnobtainable){
//				switch (item){
//				case 0:
//					//air
//					continue;
//				case 90:
//					//portal
//					continue;
//				case 51:
//					//fire
//				continue;
//				}
//			}
//			else {
//				switch (item){
//				case 0:
//					//air
//					continue;
//				case 90:
//					//portal
//					continue;
//				case 97:
//					//silverfish egg
//					continue;
//				case 95:
//					//locked chest
//					item = 54;
//				case 78:
//					// snow
//					continue;
//				case 34:
//					// piston head
//					continue;
//				case 119:
//					// end portal
//					continue;
//				case 80:
//					//snow block
//					break;
//				case 92:
//					//cake
//					item = 354;
//					break;
//				case 43: case 125:
//					//double slabs
//					item +=1;
//					addamt = 2;
//					break;
//				case 20:
//					//glass
//					break;
//				case 102:
//					//glass pane
//					break;
//				case 47:
//					//bookshelf
//					break;
//				case 103:
//					//melon
//					item = 362;
//					break;
//				case 130:
//					//ender chest
//					break;
//				case 134: case 135: case 136:
//					//wood stairs
//					item = 53;
//					break;
//				case 128: case 109: case 108:
//					//stone staitr;
//					item = 67;
//					break;
//				case 79:
//					//ice
//					item = 332;
//					break;
//				case 51:
//					//fire
//					continue;
//				case 59:
//					//crops
//					item = 295;
//					break;
//				case 72:
//					//pressure plate
//					item = 70;
//					break;
//				default:
//					item =Block.byId[item] !=null ? Block.byId[item].getDropType(b.mat.getData(), R,-10000) : item;
//					break;
//				}
//			}


			if(item <=0) continue;

			if (out.containsKey(item))
			{
				Double amt = out.get(item);
				out.put(item,amt+addamt);
			}
			else	
			{
				out.put(item,addamt);
			}

		}while(true);

		return out;
	}


	static boolean canStand(org.bukkit.block.Block base){
		org.bukkit.block.Block below = base.getRelative(0, -1, 0);
		if(!below.isEmpty() && Block.getById(below.getTypeId()).getMaterial().isSolid()){
			if(base.isEmpty() || Block.getById(base.getTypeId()).getMaterial().isSolid()==false){
				return true;
			}
		}
		return false;
	}

	//all credit to sk89q
    public static NBTBase fromNative(Tag foreign) {
        if (foreign == null) {
            return null;
        }
        if (foreign instanceof CompoundTag) {
            NBTTagCompound tag = new NBTTagCompound();
            for (Map.Entry<String, Tag> entry : ((CompoundTag) foreign)
                    .getValue().entrySet()) {
                tag.set(entry.getKey(), fromNative(entry.getValue()));
            }
            return tag;
        } else if (foreign instanceof ByteTag) {
            return new NBTTagByte(((ByteTag) foreign).getValue());
        } else if (foreign instanceof ByteArrayTag) {
            return new NBTTagByteArray(((ByteArrayTag) foreign).getValue());
        } else if (foreign instanceof DoubleTag) {
            return new NBTTagDouble(((DoubleTag) foreign).getValue());
        } else if (foreign instanceof FloatTag) {
            return new NBTTagFloat(((FloatTag) foreign).getValue());
        } else if (foreign instanceof IntTag) {
            return new NBTTagInt(((IntTag) foreign).getValue());
        } else if (foreign instanceof IntArrayTag) {
            return new NBTTagIntArray(((IntArrayTag) foreign).getValue());
        } else if (foreign instanceof ListTag) {
            NBTTagList tag = new NBTTagList();
            ListTag foreignList = (ListTag) foreign;
            for (Tag t : foreignList.getValue()) {
                tag.add(fromNative(t));
            }
            return tag;
        } else if (foreign instanceof LongTag) {
            return new NBTTagLong(((LongTag) foreign).getValue());
        } else if (foreign instanceof ShortTag) {
            return new NBTTagShort(((ShortTag) foreign).getValue());
        } else if (foreign instanceof StringTag) {
            return new NBTTagString(foreign.getName());
        } else if (foreign instanceof EndTag) {
            throw new IllegalArgumentException("Cant make EndTag: "
                    + foreign.getName());
        } else {
            throw new IllegalArgumentException("Don't know how to make NMS "
                    + foreign.getClass().getCanonicalName());
        }
    }
	
}
