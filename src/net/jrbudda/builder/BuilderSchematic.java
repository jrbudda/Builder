package net.jrbudda.builder;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;


public class BuilderSchematic {
	//todo... redo.. multi-dimensional arrays have a lot of overhead apparently.
	public EmptyBuildBlock[][][] Blocks = new EmptyBuildBlock[1][1][1]; 

	public String Name = ""; 
	public Vector SchematicOrigin = null;

	public Location getSchematicOrigin(BuilderTrait Builder){	
		if (SchematicOrigin == null)return null;

		World W = Builder.getNPC().getEntity().getWorld();

		return	SchematicOrigin.clone().toLocation(W).add(dwidth/2,0,dlength/2);
	}

	public Queue<EmptyBuildBlock> CreateMarks(double i, double j, double k, int mat){
		dwidth = i;
		dlength = k;
		Queue<EmptyBuildBlock> Q = new LinkedList<EmptyBuildBlock>();
		Q.clear();
		Q.add(new DataBuildBlock(0,0,0,mat,(byte) 0));
		Q.add(new DataBuildBlock((int) (i-1),0,0,mat,(byte) 0));
		Q.add(new DataBuildBlock(0,0,(int)k-1,mat,(byte) 0));
		Q.add(new DataBuildBlock((int)i-1,0,(int)k-1,mat,(byte) 0));
		return Q;
	}


	public Location offset(EmptyBuildBlock block, Location origin){

		return new Location(origin.getWorld(),block.X - this.dwidth/2 + origin.getBlockX() + 1,block.Y - yoffset +useryoffset + origin.getBlockY()+.5,block.Z - this.dlength/2 + origin.getBlockZ() + 1 );
	}


	int yoffset = 0;
	int useryoffset = 0;

	public 	 Queue<EmptyBuildBlock> BuildQueue(Location origin, boolean ignoreLiquids, boolean ignoreAir, boolean excavate, net.jrbudda.builder.BuilderTrait.BuildPatternsXZ pattern, boolean GroupByLayer, int ylayers, int useryoffset){
		dwidth = width();
		dlength = length();
		yoffset = 0;
		this.useryoffset = useryoffset;
		Queue<EmptyBuildBlock> Q = new LinkedList<EmptyBuildBlock>();

		//clear out empty planes on the bottom.
		boolean ok =false;
		for (int tmpy = 0;tmpy< this.height();tmpy++){
			for (int tmpx = 0;tmpx< this.width();tmpx++){
				for (int tmpz = 0;tmpz< this.length();tmpz++){
					if (this.Blocks[tmpx][tmpy][tmpz].getMat().getItemTypeId() > 0) {
						ok = true;
					}
				}
			}		
			if (ok) break;
			else yoffset++;
		}

		Queue<EmptyBuildBlock> exair = new LinkedList<EmptyBuildBlock>();
		Queue<EmptyBuildBlock> air = new LinkedList<EmptyBuildBlock>();
		Queue<EmptyBuildBlock> base = new LinkedList<EmptyBuildBlock>();
		Queue<EmptyBuildBlock> furniture = new LinkedList<EmptyBuildBlock>();
		Queue<EmptyBuildBlock> redstone = new LinkedList<EmptyBuildBlock>();
		Queue<EmptyBuildBlock> Liq = new LinkedList<EmptyBuildBlock>();
		Queue<EmptyBuildBlock> Decor = new LinkedList<EmptyBuildBlock>();
		Queue<EmptyBuildBlock> buildQ = new LinkedList<EmptyBuildBlock>();

		//	long count = 0;

		for(int y = yoffset;y<height();y+=ylayers){

			List<EmptyBuildBlock> thisLayer;
			switch (pattern){
			case linear:
				thisLayer = Util.LinearPrintLayer(y,ylayers, Blocks, false);
				break;
			case reverselinear:
				thisLayer = Util.LinearPrintLayer(y,ylayers, Blocks, true);
				break;
			case reversespiral:
				thisLayer = Util.spiralPrintLayer(y,ylayers, Blocks, true);
				break;
			case spiral:
				thisLayer = Util.spiralPrintLayer(y,ylayers, Blocks, false);
				break;
			default:
				thisLayer = Util.spiralPrintLayer(y,ylayers, Blocks, false);
				break;
			}

			//	count+=thisLayer.size();

			for(EmptyBuildBlock b:thisLayer){
				//check if it needs to be placed.
				org.bukkit.block.Block pending = origin.getWorld().getBlockAt(offset(b,origin));

				if (excavate && pending.isEmpty()==false) exair.add(new EmptyBuildBlock(b.X, b.Y, b.Z));

				if(!excavate){	//wont be nuffing there, lol
					if (pending.getTypeId() == b.getMat().getItemTypeId() && pending.getData() == b.getMat().getData() ) continue;
					else if (pending.getTypeId() == 3 && b.getMat().getItemTypeId() ==2)  continue;
					else if (pending.getTypeId() == 2 && b.getMat().getItemTypeId() ==3) continue;

				}

				org.bukkit.Material m = b.getMat().getItemType();

				if (m==null) continue;

				switch (m) {
				case AIR:
					//first
					if (!ignoreAir && !excavate) air.add(b);
					break;
				case WATER:	case STATIONARY_WATER:	case LAVA:	case STATIONARY_LAVA:
					//5th
					if (!ignoreLiquids) Liq.add(b);
					break;	
				case SAND: case GRAVEL:
					Liq.add(b);
					break;
				case TORCH:	case PAINTING:	case SNOW: 	case WATER_LILY: case CACTUS: case SUGAR_CANE_BLOCK: case PUMPKIN: case PUMPKIN_STEM: case PORTAL: case CAKE_BLOCK: case VINE: case NETHER_WARTS: case LEAVES:
				case SAPLING :case DEAD_BUSH: case WEB: case LONG_GRASS: case RED_ROSE: case YELLOW_FLOWER: case RED_MUSHROOM: case BROWN_MUSHROOM: case FIRE: case CROPS: case MELON_BLOCK: case MELON_STEM: case ENDER_PORTAL:
				case JACK_O_LANTERN: case CARROT: case POTATO: case SKULL: case CARPET:
					//very last
					Decor.add(b);
					break;
				case REDSTONE_TORCH_ON:	case REDSTONE_TORCH_OFF: case REDSTONE_WIRE: case REDSTONE_LAMP_OFF: case REDSTONE_LAMP_ON: case LEVER: case TRIPWIRE_HOOK: case TRIPWIRE: case STONE_BUTTON: case DIODE_BLOCK_OFF:
				case DIODE_BLOCK_ON: case DAYLIGHT_DETECTOR: case DIODE: case RAILS: case REDSTONE_COMPARATOR_ON: case REDSTONE_COMPARATOR_OFF: case POWERED_RAIL: case DETECTOR_RAIL: case ACTIVATOR_RAIL: case PISTON_BASE: 
				case PISTON_EXTENSION: case PISTON_MOVING_PIECE: case PISTON_STICKY_BASE: case TNT: case STONE_PLATE: case WOOD_PLATE: case GLOWSTONE:	case HOPPER: case REDSTONE_BLOCK:  case GOLD_PLATE: case IRON_PLATE:
				case WOOD_BUTTON: 
					//4th
					redstone.add(b);
					break;
				case FURNACE:case BURNING_FURNACE:	case BREWING_STAND: case CHEST: case JUKEBOX: case CAULDRON: case WOOD_DOOR: case WOODEN_DOOR: case IRON_DOOR: case LOCKED_CHEST: case TRAP_DOOR: case ENCHANTMENT_TABLE:
				case DISPENSER: case WORKBENCH: case SOIL: case SIGN_POST: case WALL_SIGN: case LADDER: case FENCE: case FENCE_GATE: case IRON_FENCE: case THIN_GLASS: case NETHER_FENCE: case DRAGON_EGG: case BED_BLOCK:case GLASS:
				case BIRCH_WOOD_STAIRS: case JUNGLE_WOOD_STAIRS: case WOOD_STAIRS: case SPRUCE_WOOD_STAIRS: case QUARTZ_STAIRS: case TRAPPED_CHEST: case ANVIL: case FLOWER_POT: 
					//3rd
					furniture.add(b);
					break;
				default:
					//second
					base.add(b);
					break;
				} 	

			}

			thisLayer.clear();

			if(GroupByLayer){
				buildQ.addAll(air);
				buildQ.addAll(base);
				buildQ.addAll(furniture);
				buildQ.addAll(redstone);
				buildQ.addAll(Liq);
				buildQ.addAll(Decor);

				air.clear();
				base.clear();
				furniture.clear();
				redstone.clear();
				Liq.clear();
				Decor.clear();		
			}	

		}


		if(!GroupByLayer){
			buildQ.addAll(air);
			buildQ.addAll(base);
			buildQ.addAll(furniture);
			buildQ.addAll(redstone);
			buildQ.addAll(Liq);
			buildQ.addAll(Decor);

			air.clear();
			base.clear();
			furniture.clear();
			redstone.clear();
			Liq.clear();
			Decor.clear();		
		}	

		java.util.Collections.reverse((List<?>) exair);

		Q.addAll(exair);
		Q.addAll(buildQ);

		exair.clear();
		buildQ.clear();

		return Q;
	}

	BuilderSchematic(int w, int h, int l){
		Blocks = new EmptyBuildBlock[w][h][l]; 
		dwidth = w;
		dlength = l;
	}

	public BuilderSchematic() {

	}

	public double dwidth, dlength;

	public int width(){
		return Blocks.length;
	}

	public int height(){
		return Blocks[0].length;
	}

	public int length(){
		return Blocks[0][0].length;
	}

	public String GetInfo(){
		return ChatColor.GREEN + "Name: "+ ChatColor.WHITE + Name + ChatColor.GREEN + " size: " + ChatColor.WHITE + width() + " wide, " + length() +  " long, " + height() + " tall"; 
	}


}


