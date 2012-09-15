package net.jrbudda.builder;

//import java.util.HashMap;
import java.io.File;
import java.rmi.activation.ActivationException;
import java.util.ArrayList;
import java.util.List;
//import java.util.Map;
import java.util.logging.Level;

import net.citizensnpcs.Citizens;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.api.trait.trait.Owner;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;


public class Builder extends JavaPlugin {

	public boolean debug = false;;

	public String schematicsFolder = "";
	private List<Integer> MarkMats = new ArrayList<Integer>();
	public String StartedMessage = "";
	public String  CompleteMessage = "";
	public String  CancelMessage = "";
	public String  MarkMessage = "";
	public boolean HoldItems = true;


	@Override
	public void onEnable() {

		if(getServer().getPluginManager().getPlugin("Citizens") == null || getServer().getPluginManager().getPlugin("Citizens").isEnabled() == false) {
			getLogger().log(Level.SEVERE, "Citizens 2.0 not found or not enabled");
			getServer().getPluginManager().disablePlugin(this);	
			return;
		}	
		try {
			setupDenizenHook();
		} catch (ActivationException e) {

		}

		if (denizen != null)	getLogger().log(Level.INFO,"Builder registered sucessfully with Denizen");
		else getLogger().log(Level.INFO,"Builder could not register with Denizen");


		CitizensAPI.getTraitFactory().registerTrait(TraitInfo.create(BuilderTrait.class).withName("builder"));
		this.getServer().getPluginManager().registerEvents(new BuilderListener(this), this);


		this.saveDefaultConfig();
		reloadMyConfig();
	}

	public BuilderTrait getBuilder(Entity ent){
		if( ent == null) return null;
		NPC npc = net.citizensnpcs.api.CitizensAPI.getNPCRegistry().getNPC(ent);
		if (npc !=null && npc.hasTrait(BuilderTrait.class)){
			return npc.getTrait(BuilderTrait.class);
		}

		return null;
	}

	public  BuilderTrait getBuilder(NPC npc){

		if (npc !=null && npc.hasTrait(BuilderTrait.class)){
			return npc.getTrait(BuilderTrait.class);
		}

		return null;

	}

	//***Denizen Hook
	private Plugin denizen = null;
	private void setupDenizenHook() throws ActivationException {
		denizen = this.getServer().getPluginManager().getPlugin("Denizen");
		if (denizen != null) {
			if (denizen.isEnabled()) {
			}
			else denizen =null;
		}
	}
	public String runTask(String taskname, NPC npc){
		if(denizen==null) return "Denizen plugin not found!";
		net.aufdemrand.denizen.npc.DenizenNPC dnpc = ((net.aufdemrand.denizen.Denizen)denizen).getDenizenNPCRegistry().getDenizen(npc);
		if (dnpc ==null) return "NPC is not a Denizen!";
		net.aufdemrand.denizen.scripts.ScriptHelper sE = ((net.aufdemrand.denizen.Denizen)denizen).getScriptEngine().helper;
		List<String> theScript = sE.getScript(taskname + ".Script");
		if (theScript.isEmpty()) return "Empty Script!";
		sE.queueScriptEntries(dnpc, sE.buildScriptEntries(dnpc, theScript, taskname), net.aufdemrand.denizen.scripts.ScriptEngine.QueueType.ACTIVITY);
		return null;
	}
	//



	@Override
	public void onDisable() {

		getLogger().log(Level.INFO, " v" + getDescription().getVersion() + " disabled.");
		Bukkit.getServer().getScheduler().cancelTasks(this);

	}


	private	boolean tryParseInt(String value)  
	{  
		try  
		{  
			Integer.parseInt(value);  
			return true;  
		} catch(NumberFormatException nfe)  
		{  
			return false;  
		}  
	}


	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String cmdLabel, String[] inargs) {

		if (inargs.length < 1) {
			sender.sendMessage(ChatColor.RED + "Use /builder help for command reference.");
			return true;
		}

		CommandSender player = (CommandSender) sender;

		int npcid = -1;
		int i = 0;

		//did player specify a id?
		if (tryParseInt(inargs[0])) {
			npcid = Integer.parseInt(inargs[0]);
			i = 1;
		}

		String[] args = new String[inargs.length-i];

		for (int j = i; j < inargs.length; j++) {
			args[j-i] = inargs[j];
		}


		if (args.length < 1) {
			sender.sendMessage(ChatColor.RED + "Use /builder help for command reference.");
			return true;
		}


		if (args[0].equalsIgnoreCase("help")) {
			player.sendMessage(ChatColor.GOLD + "------- Builder Commands -------");
			player.sendMessage(ChatColor.GOLD + "You can use /builder (id) [command] [args] to perform any of these commands on a builder  without having it selected.");			
			player.sendMessage(ChatColor.GOLD + "");
			player.sendMessage(ChatColor.GOLD + "/builder reload");
			player.sendMessage(ChatColor.GOLD + "  reload the config.yml");
			player.sendMessage(ChatColor.GOLD + "/builder load [schematic]");
			player.sendMessage(ChatColor.GOLD + "  Loads a schematic file");
			player.sendMessage(ChatColor.GOLD + "/builder origin");
			player.sendMessage(ChatColor.GOLD + "  Sets the build origin to the Bulder's current location");
			player.sendMessage(ChatColor.GOLD + "/builder origin clear");
			player.sendMessage(ChatColor.GOLD + "  Clears the build origin.");
			player.sendMessage(ChatColor.GOLD + "/builder origin schematic");
			player.sendMessage(ChatColor.GOLD + "  Sets the build origin to the center of the loaded schematic's original position");
			player.sendMessage(ChatColor.GOLD + "/builder mark (item)");
			player.sendMessage(ChatColor.GOLD + "  marks the 4 corners of the footprint. Optionally specify the material name or id.");
			player.sendMessage(ChatColor.GOLD + "/builder build (ignoreair) (ignorewater) (excavate)");
			player.sendMessage(ChatColor.GOLD + "  Begin building with the selected options.");
			player.sendMessage(ChatColor.GOLD + "/builder cancel");
			player.sendMessage(ChatColor.GOLD + "  Cancel building");
			return true;
		}


		NPC ThisNPC;

		if (npcid == -1){

			ThisNPC =	((Citizens)	this.getServer().getPluginManager().getPlugin("Citizens")).getNPCSelector().getSelected(sender);

			if(ThisNPC != null ){
				// Gets NPC Selected
				npcid = ThisNPC.getId();
			}

			else{
				player.sendMessage(ChatColor.RED + "You must have a NPC selected to use this command");
				return true;
			}			
		}


		ThisNPC = CitizensAPI.getNPCRegistry().getById(npcid); 

		if (ThisNPC == null) {
			player.sendMessage(ChatColor.RED + "NPC with id " + npcid + " not found");
			return true;
		}


		if (!ThisNPC.hasTrait(BuilderTrait.class)) {
			player.sendMessage(ChatColor.RED + "That command must be performed on a Builder!");
			return true;
		}


		if (sender instanceof Player){

			if (ThisNPC.getTrait(Owner.class).getOwner().equalsIgnoreCase(player.getName())) {
				//OK!

			}

			else {
				//not player is owner
				if (((Player)sender).hasPermission("citizens.admin") == false){
					//no c2 admin.
					player.sendMessage(ChatColor.RED + "You must be the owner of this Sentry to execute commands.");
					return true;
				}
				else{
					//has citizens.admin
					if (!ThisNPC.getTrait(Owner.class).getOwner().equalsIgnoreCase("server")) {
						//not server-owned NPC
						player.sendMessage(ChatColor.RED + "You, or the server, must be the owner of this NPC to execute commands.");
						return true;
					}
				}


			}

		}

		BuilderTrait inst = getBuilder(ThisNPC);


		// Commands

		if (args[0].equalsIgnoreCase("build")) {
			if(!player.hasPermission("builder.build")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			inst.oncancel = null;
			inst.oncomplete = null;
			boolean air = false, water = false, excavate = false;

			for (int a = 0; a< args.length; a++){
				if (args[a].toLowerCase().contains("oncomplete:")){
					inst.oncomplete = args[a].split(":")[1];
					player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will run task " + inst.oncomplete + " on build completion");
				}
				else if (args[a].toLowerCase().contains("oncancel:")){
					inst.oncancel = args[a].split(":")[1];
					player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will run task " + inst.oncancel + " on build cancelation");
				}
				else if (args[a].toLowerCase().contains("ignoreair")){
					air = true;
				}
				else if (args[a].toLowerCase().contains("ignorewater")){
					water = true;
				}
				else if (args[a].toLowerCase().contains("excavate")){
					excavate = true;
					player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will excavate first");
				}
			}

			if (inst.StartBuild(player, air, water, excavate)){

			}
			else {
				player.sendMessage(ChatColor.RED + ThisNPC.getName() + " could not build. Already building or no schematic loaded?.");   // Talk to the player.
			}
			return true;

		}
		else if (args[0].equalsIgnoreCase("cancel")) {
			if(!player.hasPermission("builder.cancel")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}

			if (inst.State ==net.jrbudda.builder.BuilderTrait.BuilderState.building){
				sender.sendMessage(format(CancelMessage, ThisNPC,inst.schematic));
			}
			else {
				player.sendMessage(ChatColor.RED + ThisNPC.getName() + " is not building.");   // Talk to the player.
			}

			inst.CancelBuild();
			return true;

		}
		else if (args[0].equalsIgnoreCase("reload")) {
			if(!player.hasPermission("builder.reload")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}

			this.reloadMyConfig();
			player.sendMessage(ChatColor.GREEN + "reloaded Builder/config.yml");
			return true;
		}
		else if (args[0].equalsIgnoreCase("check")) {
			if(!player.hasPermission("builder.check")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if( inst.schematic == null) {
				player.sendMessage(ChatColor.RED + "No Schematic Loaded!");   // Talk to the player.
			}
			else	player.sendMessage(inst.GetMatsList());   // Talk to the player.


			return true;

		}
		else if (args[0].equalsIgnoreCase("origin")) {
			if(!player.hasPermission("builder.origin")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}


			if(inst.State != BuilderTrait.BuilderState.idle){
				player.sendMessage(ChatColor.RED + "Cannot change origin while building!");
				return true;
			}

			if (args.length <= 1) {
				if(inst.getNPC().isSpawned()){
					inst.Origin = inst.getNPC().getBukkitEntity().getLocation();
					player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " build origin has been set to its current location.");   // Talk to the player.
				}
				else		player.sendMessage(ChatColor.RED + ThisNPC.getName() + " not spawned."); 

			}
			else {
				if(args[1].equalsIgnoreCase("clear")){
					inst.Origin = null;
					player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " build origin has been cleared");   // Talk to the player.
				}
				else if(args[1].equalsIgnoreCase("schematic")){
					if(inst.schematic == null) {
						player.sendMessage(ChatColor.RED + ThisNPC.getName() + " has no schematic loaded!");   // Talk to the player.
						return true;
					}
					if (inst.schematic.SchematicOrigin ==null){
						player.sendMessage(ChatColor.RED + inst.schematic.Name + " has no origin data!");   // Talk to the player.
						return true;
					}
					inst.Origin = inst.schematic.getSchematicOrigin(inst);
					player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " build origin has been set to:." + inst.Origin);   // Talk to the player.
				}
			}
			return true;
		}
		else if (args[0].equalsIgnoreCase("compare")) {
			if(!player.hasPermission("builder.compare")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if( inst.schematic == null) {
				player.sendMessage(ChatColor.RED + "No Schematic Loaded!");   // Talk to the player.
			}
			else	player.sendMessage(inst.GetComparisonMatsList((Player)player));   // Talk to the player.

			return true;

		}
		else if (args[0].equalsIgnoreCase("mark")) {
			if(!player.hasPermission("builder.mark")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}

			int mat = -1;

			if(args.length > 1){
				mat = getMat(args[1]);
				if(!this.MarkMats.contains(mat)) {
					mat = -1;
					player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + " can not mark with + " + args[1]+ ".The specified item is not allowed. Using default.");   // Talk to the player.	
				}
			}

			if(mat <= 0)  mat = this.MarkMats.get(0);

			if (inst.StartMark(mat)){
				sender.sendMessage(format(MarkMessage, ThisNPC, inst.schematic));
			}
			else {
				player.sendMessage(ChatColor.RED + ThisNPC.getName() + " could not mark. Already building or no schematic loaded?.");   // Talk to the player.
			}

			return true;

		}
		else if (args[0].equalsIgnoreCase("load")) {
			if(!player.hasPermission("builder.load")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}		

			if (inst.State != net.jrbudda.builder.BuilderTrait.BuilderState.idle) {
				player.sendMessage(ChatColor.RED + "Please cancel current build before loading new schematic.");
			}

			if (args.length > 1) {

				String arg = "";
				for (i=1;i<args.length;i++){
					arg += " " + args[i];
				}
				arg = arg.trim();

				arg = arg.replace(".schematic", "");
				String msg = "";
				File dir= new File(schematicsFolder);
				File file = new File(dir,arg+".schematic");
				try {
					inst.schematic = MCEditSchematicFormat.load(dir, arg);
				} catch (Exception e) {
					msg = ChatColor.YELLOW +  e.getMessage();   // Talk to the player.
					inst.schematic = null;
					if (!(e instanceof java.io.FileNotFoundException)){
						this.getLogger().log(Level.WARNING, "Builder encountered an error attempting to load: " + file.toString());
						e.printStackTrace();
					}
				}

				if (inst.schematic != null) {
					inst.SchematicName = inst.schematic.Name;
					player.sendMessage(ChatColor.GREEN +  "Loaded Sucessfully");   // Talk to the player.
					player.sendMessage(inst.schematic.GetInfo());

				}
				else {
					player.sendMessage(ChatColor.RED +  ThisNPC.getName() + " could not load " + file  + " " + msg );   // Talk to the player.
				}
			}
			else
			{
				player.sendMessage(ChatColor.RED +"You must specify a schematic" );   // Talk to the player.
			}
			return true;
		}		
		else if (args[0].equalsIgnoreCase("info")) {
			if(!player.hasPermission("builder.info")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			player.sendMessage(ChatColor.GOLD + "------- Builder Info for " + ThisNPC.getName() + "------");

			if (inst.schematic !=null)			player.sendMessage(ChatColor.GREEN + "Schematic: " + inst.schematic.GetInfo());
			else player.sendMessage(ChatColor.YELLOW + "No schematic loaded.");

			player.sendMessage(ChatColor.GREEN + "Ignore Air: " + inst.IgnoreAir + "  Ignore Liquid: " + inst.IgnoreLiquid);

			if(inst.Origin ==null)player.sendMessage(ChatColor.GREEN + "Origin: My Location");
			else player.sendMessage(ChatColor.GREEN + "Origin: " + inst.Origin.toString());

			player.sendMessage(ChatColor.GREEN + "Status: " + inst.State);

			return true;
		}

		return false;
	}

	private int getMat(String S){
		int item = -1;

		if (S == null) return item;

		org.bukkit.Material M = org.bukkit.Material.getMaterial(S.toUpperCase().split(":")[0]);

		if (item == -1) {	
			try {
				item = Integer.parseInt(S.split(":")[0]);
			} catch (Exception e) {
			}
		}

		if (M!=null) item=M.getId();


		return item;
	}

	private void reloadMyConfig(){
		this.reloadConfig();
		schematicsFolder = getConfig().getString("SchematicsFolder",this.getDataFolder() + File.separator + "schematics" + File.separator);
		CompleteMessage = getConfig().getString("DefaultTexts.BuildComplete","");
		CancelMessage = getConfig().getString("DefaultTexts.BuildCanceled","");
		StartedMessage = getConfig().getString("DefaultTexts.BuildStarted","");
		MarkMessage = getConfig().getString("DefaultTexts.Mark","");
		HoldItems = getConfig().getBoolean("DefaultOptions.HoldItems",true);
		
		for (String M:getConfig().getStringList("MarkMaterials")){
			if (getMat(M) > 0) this.MarkMats.add(getMat(M));
		}

		if (this.MarkMats.isEmpty()) this.MarkMats.add(Material.GLASS.getId());

	}

	public String format(String input, NPC npc, BuilderSchematic schem){
		return input.replace("<NPC>",npc.getName()).replace("<SCHEMATIC>",schem.Name);
	}

}
