package net.jrbudda.builder;

import java.util.LinkedList;
import java.util.Queue;

import net.citizensnpcs.api.npc.NPC;

import org.bukkit.Location;
import org.bukkit.material.MaterialData;



public class BuilderSchematic {
	public MaterialData[][][] Blocks = new MaterialData[1][1][1]; 

	public String Name = ""; 



	public class BuildBlock{
		public MaterialData mat;
		public Location loc;
	}

	public Queue<BuildBlock> Q = new LinkedList<BuildBlock>();


	public void CreateMarks(NPC Builder){
		Q.clear();
		
		BuildBlock a = new BuildBlock();
		a.loc = Builder.getBukkitEntity().getLocation().clone().add(-this.width()/2,0, -this.length()/2);
		a.mat = new MaterialData(50);
		Q.add(a);
		BuildBlock b = new BuildBlock();
		b.loc = Builder.getBukkitEntity().getLocation().clone().add(this.width()/2,0, -this.length()/2);
		b.mat = new MaterialData(50);
		Q.add(b);
		BuildBlock c = new BuildBlock();
		c.loc = Builder.getBukkitEntity().getLocation().clone().add(-this.width()/2,0, this.length()/2);
		c.mat = new MaterialData(50);
		Q.add(c);
		BuildBlock d = new BuildBlock();
		d.loc = Builder.getBukkitEntity().getLocation().clone().add(this.width()/2,0, this.length()/2);
		d.mat = new MaterialData(50);
		Q.add(d);
	}

	public void Reset(NPC Builder, boolean ignoreLiquids, boolean ignoreAir){

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
					if (this.Blocks[tmpx][tmpy][tmpz].getItemTypeId() > 0) {
						ok = true;
					}
				}
			}		
			if (ok) break;
			else yoffset++;
		}

		Queue<BuildBlock> deferr = new LinkedList<BuildBlock>();

		//well this is ugly, lol. Back and forth in x and z, linear in y.
		do {
			i+=di;
			if(i >= this.width() || i < 0) {
				di*=-1;
				i+=di;
				k+=dk;
				if (k >= this.length()||k<0) {
					dk*=-1;
					k +=dk;
					if (++j + yoffset >= this.height()) {
						break;		
					}
				}
			}

			BuildBlock b = new BuildBlock();
			b.loc = Builder.getBukkitEntity().getLocation().clone().add(i - this.width()/2,j-yoffset, k - this.length()/2);
			b.mat = this.Blocks[i][j+yoffset][k];

			switch (b.mat.getItemTypeId()) {
			case 0:
				if(!ignoreAir) Q.add(b);
				break;
			case 8:	case 9:	case 10:	case 11:
				if (!ignoreLiquids) deferr.add(b);
			case 50:	case 75:	case 76:	case 321:	case 69: 	case 131: 	case 77: 	case 389:
				deferr.add(b);
				break;
			default:
				Q.add(b);
				break;
			} 

		}while(true);


		Q.addAll(deferr);

	}


	public BuildBlock getNext(){
		return Q.poll();
	}


	BuilderSchematic(int w, int h, int l){
		Blocks = new MaterialData[w][h][l]; 
	}

	public int width(){
		return Blocks.length;
	}

	public int height(){
		return Blocks[0].length;
	}


	public int length(){
		return Blocks[0][0].length;
	}


}
