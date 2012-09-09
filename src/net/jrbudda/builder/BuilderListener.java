package net.jrbudda.builder;


import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;


public class BuilderListener implements Listener {

	public Builder plugin; 

	public BuilderListener(Builder builderplugin) {
		plugin = builderplugin;
	}


	@EventHandler(priority = org.bukkit.event.EventPriority.HIGHEST)
	public void place(org.bukkit.event.block.BlockPlaceEvent event){
		BuilderTrait inst = plugin.getBuilder(event.getPlayer());
		if (inst!=null) event.setCancelled(false);
	}


	@EventHandler
	public void NavCom(net.citizensnpcs.api.ai.event.NavigationCompleteEvent event){
		NPC npc =null;
		for(NPC n : CitizensAPI.getNPCRegistry()){
			if (n.getNavigator() == event.getNavigator()){
				npc = n;
				break;
			}

		}

	//	plugin.getLogger().info("nav complete " + npc);

		BuilderTrait inst = plugin.getBuilder(npc);

		if(inst==null) return;

		if(inst.State == net.jrbudda.builder.BuilderTrait.BuilderState.building )inst.PlaceNextBlock();

	}


	@EventHandler
	public void NavCan(net.citizensnpcs.api.ai.event.NavigationCancelEvent event){
		NPC npc =null;
		for(NPC n : CitizensAPI.getNPCRegistry()){
			if (n.getNavigator() == event.getNavigator()){
				npc = n;
				break;
			}		
		}
		BuilderTrait inst = plugin.getBuilder(npc);

	//	plugin.getLogger().info("nav cancel " + npc);

		if(inst==null) return;

		if(inst.State == net.jrbudda.builder.BuilderTrait.BuilderState.building )inst.PlaceNextBlock();

	}





}
