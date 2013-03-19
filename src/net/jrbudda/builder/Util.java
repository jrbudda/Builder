package net.jrbudda.builder;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;
import java.util.Random;

////
import net.minecraft.server.v1_5_R1.Block;
import net.minecraft.server.v1_5_R1.Item;
import net.minecraft.server.v1_5_R1.LocaleI18n;
////


import org.bukkit.ChatColor;


public class Util {


	public static String printList(Map<Integer, Integer> map){
		StringBuilder sb = new StringBuilder();

		java.util.Iterator<Entry<Integer, Integer>> it = map.entrySet().iterator();

		while (it.hasNext()){
			Entry<Integer, Integer> i = it.next();
			if(i.getValue() > 0){
				sb.append(ChatColor.GREEN + getLocalItemName(i.getKey()) + ":" +ChatColor.WHITE+ i.getValue());
				if(it.hasNext())sb.append(", ");
			}	
		}
		return sb.toString();
	}


	public static String getLocalItemName(int MatId){
		try {
			if (MatId==0) return  "Air";
			if(MatId < 256){
				Block b =Block.byId[MatId];
				if ( MatId == 6)  return LocaleI18n.get("tile.sapling.oak.name");
				if ( MatId == 21)  return LocaleI18n.get("item.dyePowder.blue.name");
				if ( MatId == 44)  return LocaleI18n.get("tile.stoneSlab.stone.name");
				if ( MatId == 126)  return LocaleI18n.get("tile.stoneSlab.wood.name");
				if ( MatId == 126)  return LocaleI18n.get("tile.stoneSlab.wood.name");
				return	b.getName();
			}
			else{
				Item b =Item.byId[MatId];
				return LocaleI18n.get(b.getName() + ".name");
			}
		} catch (Exception e) {
			return ((Integer)MatId).toString();
		}
	}

	public static List<BuildBlock> spiralPrintLayer(int starty,int ylayers, BuildBlock[][][] a, boolean reverse)
	{
		int i, k = 0, l = 0;

		int m = a.length;
		int n = a[0].length;
		int o = a[0][0].length;

		List<BuildBlock> out = new ArrayList<BuildBlock>();

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



	public static List<BuildBlock> LinearPrintLayer(int starty,int ylayers, BuildBlock[][][] a, boolean reverse)
	{
		int i = 0,k = 0;
		int di = 1;
		int dk=1;

		int m = a.length;
		int n = a[0].length;
		int o = a[0][0].length;

		List<BuildBlock> out = new ArrayList<BuildBlock>();

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

	public static Map<Integer, Integer> MaterialsList(Queue<BuildBlock> Q) throws Exception{

		Map<Integer, Integer> out = new HashMap<Integer, Integer>();

		do{

			BuildBlock b = Q.poll();

			if (b==null) break;
			int item = b.mat.getItemTypeId();

			int addamt = 1;

			switch (item){
			case 0:
				//air
				continue;
			case 90:
				//portal
				continue;
			case 97:
				//silverfish egg
				continue;
			case 95:
				//locked chest
				item = 54;
			case 78:
				// snow
				continue;
			case 34:
				// piston head
				continue;
			case 119:
				// end portal
				continue;
			case 80:
				//snow block
				break;
			case 92:
				//cake
				item = 354;
				break;
			case 43: case 125:
				//double slabs
				item +=1;
				addamt = 2;
				break;
			case 20:
				//glass
				break;
			case 102:
				//glass pane
				break;
			case 47:
				//bookshelf
				break;
			case 103:
				//melon
				item = 362;
				break;
			case 130:
				//ender chest
				break;
			case 134: case 135: case 136:
				//wood stairs
				item = 53;
				break;
			case 128: case 109: case 108:
				//stone staitr;
				item = 67;
				break;
			case 79:
				//ice
				item = 332;
				break;
			case 51:
				//fire
				continue;
			case 59:
				//crops
				item = 295;
				break;
			case 72:
				//pressure plate
				item = 70;
				break;
			default:
				item =Block.byId[item] !=null ? Block.byId[item].getDropType(b.mat.getData(), R,-10000) : item;
				break;
			}

			if(item <=0) continue;

			if (out.containsKey(item))
			{
				int amt = out.get(item);
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
		if(!below.isEmpty() && Block.byId[below.getTypeId()].material.isSolid()){
			if(base.isEmpty() || Block.byId[base.getTypeId()].material.isSolid()==false){
				return true;
			}
		}
		return false;
	}

}
