package net.jrbudda.builder;

//import java.util.HashMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.activation.ActivationException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.logging.Level;

import net.aufdemrand.denizen.objects.dNPC;
import net.citizensnpcs.Citizens;
import net.citizensnpcs.api.CitizensAPI;
import net.citizensnpcs.api.npc.NPC;
import net.citizensnpcs.api.trait.TraitInfo;
import net.citizensnpcs.api.trait.trait.Owner;

import net.minecraft.server.v1_6_R2.Block;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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
	public String SurveyMessage = "";
	public String SupplyListMessage = "";
	public String SupplyNeedMessage = "";
	public String SupplyDontNeedMessage = "";
	public String SupplyTakenMessage = "";
	public String CollectingMessage = "";

	public class supplymap{
		public int original;
		public int require;
		public double amount =1;
	}

	public static java.util.HashMap<Integer, supplymap> SupplyMapping;


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
				String vers = denizen.getDescription().getVersion();
				if(vers.startsWith("0.7")) {
					//	net.aufdemrand.sentry.denizen.v7.Util.setupDenizenHook(DieLikePlayers);
					getLogger().log(Level.WARNING, "Builder is no longer compatible with Denizen .7");
					denizen =null;
				}
				else if(vers.startsWith("0.8") || vers.startsWith("0.9")){
					//ok
				}
			}
			else denizen =null;
		}
	}


	public String runTask(String taskname, NPC npc){
		return runTaskv9(taskname, npc);
	}

	private String runTaskv9(String taskname, NPC npc){
		try {
			if(denizen==null) return "Denizen plugin not found!";	
			dNPC dnpc = net.aufdemrand.denizen.objects.dNPC.mirrorCitizensNPC(npc);
			net.aufdemrand.denizen.scripts.containers.core.TaskScriptContainer task = net.aufdemrand.denizen.scripts.ScriptRegistry.getScriptContainerAs(taskname, net.aufdemrand.denizen.scripts.containers.core.TaskScriptContainer.class);
			if (task !=null){
				task.runTaskScript(null, dnpc, null);
			}
			else return "Task: " + taskname + " was not found!";
			return null;
		} catch (Exception e) {
			e.printStackTrace();
			return "Error while executing task: " + e.getMessage();
		}
	}

	public void DenizenAction(NPC npc, String action){
		if(denizen!=null){
			try {
				if(npc.hasTrait(net.aufdemrand.denizen.npc.traits.AssignmentTrait.class)){
					dNPC dnpc = dNPC.mirrorCitizensNPC(npc);
					dnpc.action(action, null);			
				}
			} catch (Exception e) {
				getLogger().log(Level.WARNING, "Error running action!");
				e.printStackTrace();
			}		
		}
	}

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
			player.sendMessage(ChatColor.GOLD + "  Sets the build origin to the loaded schematic's original position");
			player.sendMessage(ChatColor.GOLD + "/builder origin me");
			player.sendMessage(ChatColor.GOLD + "  Sets the build origin to your current location");
			player.sendMessage(ChatColor.GOLD + "/builder origin current");
			player.sendMessage(ChatColor.GOLD + "  If the builder is currently building, sets the origin to the starting position of the current project.");
			player.sendMessage(ChatColor.GOLD + "/builder origin x,y,z");
			player.sendMessage(ChatColor.GOLD + "  Sets the builder's origin to x,y,z of the current world.");
			player.sendMessage(ChatColor.GOLD + "/builder mark (item)");
			player.sendMessage(ChatColor.GOLD + "  marks the 4 corners of the footprint. Optionally specify the material name or id.");
			player.sendMessage(ChatColor.GOLD + "/builder build (ignoreair) (ignorewater) (excavate) (layers:#) (groupall) (reversespiral) (linear) (reverselinear) (yoffset:#)");
			player.sendMessage(ChatColor.GOLD + "  Begin building with the selected options.");
			player.sendMessage(ChatColor.GOLD + "/builder cancel");
			player.sendMessage(ChatColor.GOLD + "  Cancel building");
			player.sendMessage(ChatColor.GOLD + "/builder survey (excavate)");
			player.sendMessage(ChatColor.GOLD + "  View the list of materials required to build the loaded scheamtic at the current origin with the specified options.");
			player.sendMessage(ChatColor.GOLD + "/builder timeout [0.1 - 2000000.0]");
			player.sendMessage(ChatColor.GOLD + "  Sets the maximum number of seconds between blocks");
			player.sendMessage(ChatColor.GOLD + "/builder supply [true/false]");
			player.sendMessage(ChatColor.GOLD + "  set whether the Builder needs to be supplied with materials before building.");	
			player.sendMessage(ChatColor.GOLD + "/builder hold [true/false]");
			player.sendMessage(ChatColor.GOLD + "  set whether the Builder holds blocks while building.");	
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
		else if (args[0].equalsIgnoreCase("testmats")) {
			StringBuilder sb = new StringBuilder();

			for (int j = 1; j < 137; j++) {
				sb.append( j+":"+ Util.getLocalItemName(j) +" > " +  (Block.byId[j].getDropType(j, Util.R,-10000)) +":" + Util.getLocalItemName(Block.byId[j].getDropType(j, Util.R,-10000))+ "\n" );
			}

			java.io.File f = new File("mats.txt");
			java.io.FileWriter fw;
			try {
				fw = new java.io.FileWriter(f);					
				fw.write(sb.toString());
				fw.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
			return true;
		}
		else if (args[0].equalsIgnoreCase("list")) {
			if(!player.hasPermission("builder.list")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}

			// Directory path here
			String path =schematicsFolder; 

			File folder = new File(path);
			File[] listOfFiles = folder.listFiles(); 
			StringBuilder out = new StringBuilder();
			int i1= 0;

			if (listOfFiles.length ==0){
				player.sendMessage(ChatColor.RED+"No schematics found.");
				return true;
			}

			for ( i1 = 0; i1 < listOfFiles.length-1; i1++) 
			{

				if (listOfFiles[i1].isFile()) 
				{
					String   file = listOfFiles[i1].getName();
					if (file.endsWith(".schematic") )
					{
						out.append(file.replace(".schematic",", "));
					}
				}
			}


			if (listOfFiles[i1].isFile()) 
			{
				String   file = listOfFiles[i1].getName();
				if (file.endsWith(".schematic") )
				{
					out.append(file.replace(".schematic","."));
				}
			}


			player.sendMessage(ChatColor.GREEN + "Schematics: "+ChatColor.WHITE + out.toString());

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


		if (sender instanceof Player && !CitizensAPI.getNPCRegistry().isNPC((Entity) sender)){
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
			inst.onStart = null;
			inst.ContinueLoc =null;
			inst.IgnoreAir = false;
			inst.IgnoreLiquid = false;
			inst.Excavate = false;
			inst.GroupByLayer = true;
			inst.BuildYLayers = 1;
			inst.Silent = false;
			inst.BuildPatternXY = net.jrbudda.builder.BuilderTrait.BuildPatternsXZ.spiral;			
		
			for (int a = 0; a< args.length; a++){
				if (args[a].equalsIgnoreCase("silent")){
					if (args[a].equalsIgnoreCase("silent")){
						inst.Silent = true;
					}
				}
			}

			for (int a = 0; a< args.length; a++){
				if (args[a].toLowerCase().contains("oncomplete:")){
					inst.oncomplete = args[a].split(":")[1];
					if(!inst.Silent) player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will run task " + inst.oncomplete + " on build completion");
				}
				else if (args[a].toLowerCase().contains("oncancel:")){
					inst.oncancel = args[a].split(":")[1];
					if(!inst.Silent)player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will run task " + inst.oncancel + " on build cancelation");
				}
				else if (args[a].toLowerCase().contains("onstart:")){
					inst.onStart = args[a].split(":")[1];
					if(!inst.Silent)player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will run task " + inst.onStart + " on when building starts");
				}
				else if (args[a].toLowerCase().contains("layers:")){
					String test = args[a].split(":")[1];
					if (tryParseInt(test)){
						int layers = Integer.parseInt(test);
						if (layers < 1) layers=1;
						if (layers > Integer.MAX_VALUE) layers=Integer.MAX_VALUE;
						inst.BuildYLayers = layers;
					}
				}
				else if (args[a].toLowerCase().contains("yoffset:")){
					String test = args[a].split(":")[1];
					if (tryParseInt(test)){
						int layers = Integer.parseInt(test);
						inst.Yoffset = layers;
					}
				}
				else if (args[a].equalsIgnoreCase("groupall")){
					inst.GroupByLayer =false;
				}
				else if (args[a].equalsIgnoreCase("ignoreair")){
					inst.IgnoreAir = true;
				}
				else if (args[a].equalsIgnoreCase("ignoreliquid")){
					inst.IgnoreLiquid = true;
				}
				else if (args[a].equalsIgnoreCase("excavate")){
					inst.Excavate = true;
					if(!inst.Silent)player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " will excavate first");
				}
				else if (args[a].equalsIgnoreCase("spiral")){
					inst.BuildPatternXY = net.jrbudda.builder.BuilderTrait.BuildPatternsXZ.spiral;
				}
				else if (args[a].equalsIgnoreCase("reversespiral")){
					inst.BuildPatternXY = net.jrbudda.builder.BuilderTrait.BuildPatternsXZ.reversespiral;
				}
				else if (args[a].equalsIgnoreCase("linear")){
					inst.BuildPatternXY = net.jrbudda.builder.BuilderTrait.BuildPatternsXZ.linear;
				}
				else if (args[a].equalsIgnoreCase("reverselinear")){
					inst.BuildPatternXY = net.jrbudda.builder.BuilderTrait.BuildPatternsXZ.reverselinear;
				}
			}

			if(inst.RequireMaterials){
				inst.GetMatsList(inst.Excavate);
			}

			if (!inst.TryBuild(player)){
				if(!inst.Silent)player.sendMessage(ChatColor.RED + ThisNPC.getName() + " could not build. Already building or no schematic loaded?.");   // Talk to the player.
			}
			return true;

		}
		else if (args[0].equalsIgnoreCase("cancel")) {
			if(!player.hasPermission("builder.cancel")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}

			if (inst.State != BuilderTrait.BuilderState.idle){
				sender.sendMessage(format(CancelMessage, ThisNPC,inst.schematic, sender, null, "0"));
			}
			else {
				player.sendMessage(ChatColor.RED + ThisNPC.getName() + " is not building.");   // Talk to the player.
			}

			inst.CancelBuild();
			return true;

		}
		else if (args[0].equalsIgnoreCase("survey")) {
			if(!player.hasPermission("builder.survey")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}

			boolean ex = false;

			for (int a = 0; a< args.length; a++){
				if (args[a].toLowerCase().contains("excavate")){
					ex = true;
				}
			}
			if( inst.schematic == null) {
				player.sendMessage(ChatColor.RED + "No Schematic Loaded!");   // Talk to the player.
			}
			else{
				sender.sendMessage(format(SurveyMessage + (ex ? " (exvacate)" : ""), ThisNPC, inst.schematic, sender, null, "0"));
				player.sendMessage(inst.GetMatsList(ex));   // Talk to the player.
			}

			return true;

		}
		else if (args[0].equalsIgnoreCase("origin")) {
			if(!player.hasPermission("builder.origin")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
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
				else if(args[1].equalsIgnoreCase("me")){
					if(player instanceof Player){
						inst.Origin = ((Player)player).getLocation().clone();
						player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " build origin has been set to your location");   // Talk to the player.
					}
					else 	player.sendMessage(ChatColor.RED +  "This command can only be used in-game");  
				}
				else if(args[1].equalsIgnoreCase("current")){
					if(inst.State == net.jrbudda.builder.BuilderTrait.BuilderState.building){
						inst.Origin = inst.ContinueLoc.clone();
						player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " build origin has been set to the origin of the current build");   // Talk to the player.
					}
					else 	player.sendMessage(ChatColor.RED +  ThisNPC.getName() + " is not currently building!");  
				}
				else if(args[1].split(",").length == 3){
					try {
						int x = Integer.parseInt(args[1].split(",")[0]);
						int y = Integer.parseInt(args[1].split(",")[1]);
						int z = Integer.parseInt(args[1].split(",")[2]);

						inst.Origin = new Location(inst.getNPC().getBukkitEntity().getWorld(),x,y,z);

						player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " build origin has been set to " + inst.Origin.toString());   // Talk to the player.
					} catch (Exception e) {
						player.sendMessage(ChatColor.RED + "Invalid Coordinates");  
					}
				}
				else player.sendMessage(ChatColor.RED + "Unknown origin command"); 
			}
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
					player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + " can not mark with " + args[1]+ ".The specified item is not allowed. Using default.");   // Talk to the player.	
				}
			}


			if(mat <= 0)  mat = this.MarkMats.get(0);

			if (inst.StartMark(mat)){
				sender.sendMessage(format(MarkMessage, ThisNPC, inst.schematic, sender,null, "0"));
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
				return true;
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

				//see if this has already been loaded to another builder
				for (NPC npc : CitizensAPI.getNPCRegistry()) {
					if(npc.hasTrait(BuilderTrait.class)){
						BuilderTrait bt = npc.getTrait(BuilderTrait.class);
						if (bt.schematic!=null && bt.schematic.Name.equals(arg)){
							inst.schematic = bt.schematic;		
						}
					}					
				}

				//load it from file if not found.
				if(inst.schematic==null);
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
		else if (args[0].equalsIgnoreCase("timeout")) {
			if(!player.hasPermission("builder.timeout")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + "'s Move Timeout is " + inst.MoveTimeout);
				player.sendMessage(ChatColor.GOLD + "Usage: /builder timeout [0.1 - 2000000.0]");
			}
			else {

				Double HPs = Double.valueOf(args[1]);
				if (HPs > 2000000) HPs = 2000000.0;
				if (HPs <0.0)  HPs =0.1;

				player.sendMessage(ChatColor.GREEN + ThisNPC.getName() + " move timeout set to " + HPs + ".");   // Talk to the player.
				inst.MoveTimeout = HPs;

			}

			return true;
		}
		else if (args[0].equalsIgnoreCase("supply")) {
			if(!player.hasPermission("builder.supply")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + " currently does" + (inst.RequireMaterials ? "":" NOT") +" need to be supplied with materials." );
				player.sendMessage(ChatColor.GOLD + "Usage: /builder supply [true/false]");
			}
			else {

				Boolean HPs = Boolean.valueOf(args[1]);

				inst.RequireMaterials = HPs;
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + " now does" + (inst.RequireMaterials ? "":" NOT") +" need to be supplied with materials." );


			}
			return true;
		}
		else if (args[0].equalsIgnoreCase("hold")) {
			if(!player.hasPermission("builder.hold")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			if (args.length <= 1) {
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + " currently does" + (inst.HoldItems ? "":" NOT") +" hold blocks." );
				player.sendMessage(ChatColor.GOLD + "Usage: /builder hold [true/false]");
			}
			else {

				Boolean HPs = Boolean.valueOf(args[1]);
				inst.HoldItems = HPs;
				player.sendMessage(ChatColor.GOLD + ThisNPC.getName() + " now does" + (inst.HoldItems ? "":" NOT") +" hold blocks." );

			}
			return true;
		}
		else if (args[0].equalsIgnoreCase("info")) {
			if(!player.hasPermission("builder.info")) {
				player.sendMessage(ChatColor.RED + "You do not have permissions for that command.");
				return true;
			}
			player.sendMessage(ChatColor.GOLD + "------- Builder Info for " + ThisNPC.getName() + "------");

			//	DecimalFormat df=  new DecimalFormat("#");

			if (inst.schematic !=null)			player.sendMessage(ChatColor.GREEN + "Schematic: " + inst.schematic.GetInfo());
			else player.sendMessage(ChatColor.YELLOW + "No schematic loaded.");

			if(inst.Origin ==null)player.sendMessage(ChatColor.GREEN + "Origin: " +ChatColor.WHITE +"My Location");
			else player.sendMessage(ChatColor.GREEN + "Origin: " +ChatColor.WHITE + " x:" + inst.Origin.getBlockX()+ " y:" + inst.Origin.getBlockY()+ " z:" + inst.Origin.getBlockZ());

			player.sendMessage(ChatColor.GREEN + "Status: " + ChatColor.WHITE + inst.State + " §aTimeout:§f " + inst.MoveTimeout);
			player.sendMessage(ChatColor.GREEN + "Require Mats: " + ChatColor.WHITE + inst.RequireMaterials + " §aHold Items:§f " + inst.HoldItems);

			if (inst.State == net.jrbudda.builder.BuilderTrait.BuilderState.building){
				player.sendMessage(ChatColor.BLUE + "Location: " +ChatColor.WHITE + " x:" + inst.ContinueLoc.getBlockX()+ " y:" + inst.ContinueLoc.getBlockY()+ " z:" + inst.ContinueLoc.getBlockZ());
				player.sendMessage(ChatColor.BLUE + "Build Pattern XZ: " + ChatColor.WHITE +inst.BuildPatternXY + ChatColor.BLUE + " Build Y Layers: " + ChatColor.WHITE +inst.BuildYLayers);
				player.sendMessage(ChatColor.BLUE + "Ignore Air: " + ChatColor.WHITE +inst.IgnoreAir +ChatColor.BLUE +  "  Ignore Liquid: " + ChatColor.WHITE +inst.IgnoreLiquid);
				player.sendMessage(ChatColor.BLUE + "Hold Items: " + ChatColor.WHITE +inst.HoldItems +ChatColor.BLUE +  "  Excavte: " +ChatColor.WHITE + inst.Excavate);
				player.sendMessage(ChatColor.BLUE + "On Complete: " + ChatColor.WHITE +inst.oncomplete + ChatColor.BLUE + "  On Cancel: " +ChatColor.WHITE + inst.oncancel);
				player.sendMessage(ChatColor.BLUE + "On Start: " + ChatColor.WHITE +inst.onStart);
				long c = inst.startingcount;
				player.sendMessage(ChatColor.BLUE + "Blocks: Total: " + ChatColor.WHITE + c + ChatColor.BLUE + "  Remaining: " + ChatColor.WHITE + inst.Q.size());
				double percent = ((double)(c-inst.Q.size()) / (double)c)* 100;
				player.sendMessage(ChatColor.BLUE + "Complete: " +ChatColor.WHITE + String.format("%1$.1f", percent)+ "%");
			}	
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
		this.saveDefaultConfig();
		this.reloadConfig();
		schematicsFolder = getConfig().getString("SchematicsFolder",this.getDataFolder() + File.separator + "schematics" + File.separator);
		CompleteMessage = getConfig().getString("DefaultTexts.BuildComplete","");
		CancelMessage = getConfig().getString("DefaultTexts.BuildCanceled","");
		StartedMessage = getConfig().getString("DefaultTexts.BuildStarted","");
		CollectingMessage =  getConfig().getString("DefaultTexts.BuildCollecting","");
		MarkMessage = getConfig().getString("DefaultTexts.Mark","");
		SurveyMessage = getConfig().getString("DefaultTexts.Survey","");
		SupplyListMessage = getConfig().getString("DefaultTexts.Supply_List","");
		SupplyNeedMessage = getConfig().getString("DefaultTexts.Supply_Need_Item","");
		SupplyDontNeedMessage = getConfig().getString("DefaultTexts.Supply_Dont_Need_Item","");
		SupplyTakenMessage = getConfig().getString("DefaultTexts.Supply_Item_Taken","");
		for (String M:getConfig().getStringList("MarkMaterials")){
			if (getMat(M) > 0) this.MarkMats.add(getMat(M));
		}

		if (this.MarkMats.isEmpty()) this.MarkMats.add(Material.GLASS.getId());

		loadSupplyMap();

	}

	public String format(String input, NPC npc, BuilderSchematic schem, CommandSender player, String item, String amount){
		input = input.replace("<NPC>",npc.getName());
		input = input.replace("<SCHEMATIC>", schem == null ? "" : schem.Name);
		input = input.replace("<PLAYER>", player == null ? "" : player.getName());
		input = input.replace("<ITEM>", item == null ? "" : item);
		input = input.replace("<AMOUNT>", amount.toString());
		input =	ChatColor.translateAlternateColorCodes('&', input);
		return input;
	}


	public void loadSupplyMap(){

		if(!(new File(this.getDataFolder() + File.separator + "supply.txt").exists()))
			saveResource("supply.txt", false);

		File items = new File(this.getDataFolder() + File.separator + "supply.txt");

		Scanner s = null;

		try {
			s = new java.util.Scanner(items);
		} catch (FileNotFoundException e) {
			return;
		}

		SupplyMapping = new java.util.HashMap<Integer, Builder.supplymap>();

		while (s.hasNext()){
			String line = s.nextLine();
			String[] parts = line.split(":");
			if (parts.length < 2) continue;

			supplymap out = new supplymap();

			if (tryParseInt(parts[0])){
				out.original = Integer.parseInt(parts[0]);
			}
			else continue;

			if (tryParseInt(parts[1])){
				out.require = Integer.parseInt(parts[1]);
			}
			else continue;

			if (parts.length > 2) {
				out.amount = Double.parseDouble(parts[2]);
			}

			//	this.getServer().getLogger().info("Loaded " + out.original + " to " + out.require + "amt " + out.amount);

			SupplyMapping.put(out.original, out);

		}


	}



}
