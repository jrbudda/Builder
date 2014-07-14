package net.jrbudda.builder;

import java.util.Map;

import net.citizensnpcs.api.jnbt.Tag;
import org.bukkit.material.MaterialData;

//Todo, add extended data.
class EmptyBuildBlock{
	public int X, Y, Z;
	EmptyBuildBlock(){
	}
	EmptyBuildBlock(int x, int y, int z){
		this.X = x;
		this.Y = y;
		this.Z = z;
	}
	public MaterialData getMat(){
		return Util.Air;
	}

}

class DataBuildBlock extends EmptyBuildBlock{
	private MaterialData mat;
	
	DataBuildBlock(int x, int y, int z, int id, byte data){
		this.X = x;
		this.Y = y;
		this.Z = z;
		this.mat = new MaterialData(id,data);
	}
	
	@Override
	public MaterialData getMat(){
		return mat;
	}
}

class TileBuildBlock extends DataBuildBlock{
	
	TileBuildBlock(int x, int y, int z, int id, byte data) {
		super(x, y, z, id, data);
	}

	public  Map<String, Tag> tiles = null;
}

