package net.jrbudda.builder;


import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;

import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.jrbudda.builder.BuilderTrait.BuilderState;


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


	//	@EventHandler(priority = org.bukkit.event.EventPriority.MONITOR)
	//	public void bpe(org.bukkit.event.block.BlockPlaceEvent event){
	//		plugin.getLogger().info("place " + event.getPlayer() + event.isCancelled()  + " " + Util.getLocalName(event.getBlock().getTypeId()));
	//	}

	@EventHandler
	public void clickedme(net.citizensnpcs.api.event.NPCRightClickEvent event){
		BuilderTrait inst = plugin.getBuilder(event.getNPC());
		if (inst==null) return;
		if(inst.State!=net.jrbudda.builder.BuilderTrait.BuilderState.collecting) return;

		Player player = event.getClicker();
		ItemStack is = player.getItemInHand();

		String itemname = Util.getLocalItemName(is.getTypeId());

		if(is.getTypeId() == 0){
			//list what is still needed
			player.sendMessage(plugin.format(plugin.SupplyListMessage, inst.getNPC(),inst.schematic,(CommandSender) player,null,"0"));
			player.sendMessage(Util.printList(inst.NeededMaterials));	

		}
		else{

			if (!player.hasPermission("builder.donate")) {
				player.sendMessage(ChatColor.RED + "You do not have permission to donate");
				return;
			}

			int item = is.getTypeId();

			//do i need it?
			int needed = (int) (inst.NeededMaterials.containsKey(item) ? inst.NeededMaterials.get(item) : 0);
			if(needed > 0){

				//yup, i need it
				int taking = Math.min(is.getAmount(),needed);

				if (inst.Sessions.containsKey(player) && System.currentTimeMillis() < inst.Sessions.get(player) + 5*1000){
					//take it

					//update player hand item
					ItemStack newis;

					if (is.getAmount() - taking > 0) newis= is.clone();
					else newis = new ItemStack(0);	
					newis.setAmount(is.getAmount() - taking);
					event.getClicker().setItemInHand(newis);

					//update needed

					inst.NeededMaterials.put(item,(double) (needed  - taking));
					player.sendMessage(plugin.format(plugin.SupplyTakenMessage, inst.getNPC(),inst.schematic, (CommandSender)player,itemname,taking + ""));

					//check if can start
					inst.TryBuild(null);

				}
				else{
					player.sendMessage(plugin.format(plugin.SupplyNeedMessage, inst.getNPC(),inst.schematic,(CommandSender) player,itemname,needed+""));
					inst.Sessions.put(player,System.currentTimeMillis());
				}	

			}
			else{
				player.sendMessage(plugin.format(plugin.SupplyDontNeedMessage, inst.getNPC(),inst.schematic,(CommandSender) player,itemname,"0"));
				//don't need it or already have it.
			}

		}
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

		if(inst.State!=BuilderState.idle)inst.PlaceNextBlock();

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

		if(inst.State!=BuilderState.idle)inst.PlaceNextBlock();

	}





}
