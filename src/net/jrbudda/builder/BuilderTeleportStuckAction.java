package net.jrbudda.builder;

import org.bukkit.Location;

import net.citizensnpcs.api.ai.Navigator;
import net.citizensnpcs.api.ai.StuckAction;
import net.citizensnpcs.api.npc.NPC;

public class BuilderTeleportStuckAction implements StuckAction{

	@Override
	public boolean run(NPC npc, Navigator navigator) {
		if (!npc.isSpawned())
			return false;
		Location base = navigator.getTargetAsLocation();
		npc.getBukkitEntity().teleport(base);
		// 	inst.plugin.getServer().broadcastMessage("bgtp stuck action");
		return false;
	}

	public static BuilderTeleportStuckAction INSTANCE = new BuilderTeleportStuckAction(); 

}