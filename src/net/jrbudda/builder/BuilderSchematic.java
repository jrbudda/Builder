package net.jrbudda.builder;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.material.MaterialData;
import org.bukkit.util.Vector;


public class BuilderSchematic {
	public BuildBlock[][][] Blocks = new BuildBlock[1][1][1]; 

	public String Name = ""; 
	public Vector SchematicOrigin = null;

	public Queue<BuildBlock> Q = new LinkedList<BuildBlock>();

	public Location getSchematicOrigin(BuilderTrait Builder){	
		if (SchematicOrigin == null)return null;
		return SchematicOrigin.clone().toLocation(Builder.getNPC().getBukkitEntity().getLocation().getWorld())
				.add(dwidth()/2,0,dlength()/2);
	}

	public void CreateMarks(Location origin, double i, double j, double k, int mat){
		Q.clear();

		BuildBlock a = new BuildBlock();
		a.X =(int) (origin.getX()-i/2);
		a.Y =origin.getBlockY();
		a.Z =(int) (origin.getZ()-k/2);
		a.mat = new MaterialData(mat);
		Q.add(a);
		BuildBlock b = new BuildBlock();
		b.X =(int) (origin.getX()-i/2);
		b.Y =origin.getBlockY();
		b.Z =(int) (origin.getZ()+k/2);
		b.mat = new MaterialData(mat);
		Q.add(b);
		BuildBlock c = new BuildBlock();
		c.X =(int) (origin.getX()+i/2);
		c.Y =origin.getBlockY();	
		c.Z =(int) (origin.getZ()-k/2);
		c.mat = new MaterialData(mat);
		Q.add(c);
		BuildBlock d = new BuildBlock();
		d.X =(int) (origin.getX()+i/2);
		d.Y =origin.getBlockY();
		d.Z =(int) (origin.getZ()+k/2);
		d.mat = new MaterialData(mat);
		Q.add(d);
	}

	public Map<Material, Integer> MaterialsList(){

		Map<Material, Integer> out = new HashMap<Material, Integer>();

		for (int tmpy = 0;tmpy< this.height();tmpy++){
			for (int tmpx = 0;tmpx< this.width();tmpx++){
				for (int tmpz = 0;tmpz< this.length();tmpz++){

					if (out.containsKey(this.Blocks[tmpx][tmpy][tmpz].mat.getItemType())){
						out.put(this.Blocks[tmpx][tmpy][tmpz].mat.getItemType(),out.get(this.Blocks[tmpx][tmpy][tmpz].mat.getItemType())+1);
					}
					else	{
						out.put(this.Blocks[tmpx][tmpy][tmpz].mat.getItemType(),1);
					}
				}
			}		
		}

		return out;
	}


	public void Reset(Location origin, boolean ignoreLiquids, boolean ignoreAir, boolean excavate){

		int i = 0;
		int j = 0;
		int k = 0;
		int di = 1;
		int dk = 1;
		int yoffset = 0;
		Q.clear();

		//clear out empty planes on the bottom.
		boolean ok =false;
		for (int tmpy = 0;tmpy< this.height();tmpy++){
			for (int tmpx = 0;tmpx< this.width();tmpx++){
				for (int tmpz = 0;tmpz< this.length();tmpz++){
					if (this.Blocks[tmpx][tmpy][tmpz].mat.getItemTypeId() > 0) {
						ok = true;
					}
				}
			}		
			if (ok) break;
			else yoffset++;
		}

		Queue<BuildBlock> exair = new LinkedList<BuildBlock>();
		Queue<BuildBlock> air = new LinkedList<BuildBlock>();
		Queue<BuildBlock> base = new LinkedList<BuildBlock>();
		Queue<BuildBlock> furniture = new LinkedList<BuildBlock>();
		Queue<BuildBlock> redstone = new LinkedList<BuildBlock>();
		Queue<BuildBlock> Liq = new LinkedList<BuildBlock>();
		Queue<BuildBlock> Decor = new LinkedList<BuildBlock>();
		Queue<BuildBlock> buildQ = new LinkedList<BuildBlock>();

		//well this is ugly, lol. Back and forth in x and z, linear in y.
		for(int y = yoffset;y<height();y++){

			List<BuildBlock> thisLayer = Util.spiralPrintLayer(y, Blocks);

			for(BuildBlock b:thisLayer){
				b.X += origin.getBlockX() - this.dwidth()/2;
				b.Y += origin.getBlockY() - yoffset;
				b.Z += origin.getBlockZ() - this.dheight()/2; 

				if (excavate) exair.add(new BuildBlock(0, (byte) 0, b.X, b.Y, b.Z));

				switch (b.mat.getItemType()) {
				case AIR:
					//first
					if (!ignoreAir && !excavate) air.add(b);
					break;
				case WATER:	case STATIONARY_WATER:	case LAVA:	case STATIONARY_LAVA:
					//5th
					if (!ignoreLiquids) Liq.add(b);
					break;	
				case TORCH:	case PAINTING:	case SNOW: 	case WATER_LILY: case CACTUS: case SUGAR_CANE_BLOCK: case PUMPKIN: case PUMPKIN_STEM: case PORTAL: case CAKE_BLOCK: case VINE: case NETHER_WARTS: case LEAVES:
				case SAPLING :case DEAD_BUSH: case WEB: case LONG_GRASS: case RED_ROSE: case YELLOW_FLOWER: case RED_MUSHROOM: case BROWN_MUSHROOM: case FIRE: case CROPS: case MELON_BLOCK: case MELON_STEM: case ENDER_PORTAL:
				case JACK_O_LANTERN:
					//very last
					Decor.add(b);
					break;
				case REDSTONE_TORCH_ON:	case REDSTONE_TORCH_OFF: case REDSTONE_WIRE: case REDSTONE_LAMP_OFF: case REDSTONE_LAMP_ON: case LEVER: case TRIPWIRE_HOOK: case TRIPWIRE: case STONE_BUTTON: case DIODE_BLOCK_OFF:
				case DIODE_BLOCK_ON: case DIODE: case RAILS: case POWERED_RAIL: case DETECTOR_RAIL: case PISTON_BASE: case PISTON_EXTENSION: case PISTON_MOVING_PIECE: case PISTON_STICKY_BASE: case TNT: case STONE_PLATE: case WOOD_PLATE: case GLOWSTONE:
					//4th
					redstone.add(b);
					break;
				case FURNACE:case BURNING_FURNACE:	case BREWING_STAND: case CHEST: case JUKEBOX: case CAULDRON: case WOOD_DOOR: case WOODEN_DOOR: case IRON_DOOR: case LOCKED_CHEST: case TRAP_DOOR: case ENCHANTMENT_TABLE:
				case DISPENSER: case WORKBENCH: case SOIL: case SIGN_POST: case WALL_SIGN: case LADDER: case FENCE: case FENCE_GATE: case IRON_FENCE: case THIN_GLASS: case NETHER_FENCE: case DRAGON_EGG: case BED_BLOCK:
					//3rd
					furniture.add(b);
					break;
				default:
					//second
					base.add(b);
					break;
				} 	

			}

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





		Q.addAll(exair);
		Q.addAll(buildQ);

		exair.clear();
		buildQ.clear();
	}





	public BuildBlock getNext(){
		return Q.poll();
	}


	BuilderSchematic(int w, int h, int l){
		Blocks = new BuildBlock[w][h][l]; 
	}

	public BuilderSchematic() {

	}

	public int width(){
		return Blocks.length;
	}

	public double dwidth(){
		return ((double)Blocks.length);
	}
	public double dheight(){
		return ((double)Blocks[0].length);
	}
	public double dlength(){
		return ((double)Blocks[0][0].length);
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
