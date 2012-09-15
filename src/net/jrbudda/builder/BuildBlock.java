package net.jrbudda.builder;

import org.bukkit.material.MaterialData;

//Todo, add extended data.
public class BuildBlock{
	public MaterialData mat;
	public int X, Y, Z;
	
	BuildBlock(int id, byte data, int X, int Y, int Z){
		mat = new MaterialData(id, data);
		this.X = X;
		this.Y = Y;
		this.Z = Z;	
	}
	
	BuildBlock(){
		
	}
	
}