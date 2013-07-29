package net.jrbudda.builder;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import net.citizensnpcs.api.exception.NPCLoadException;
import net.citizensnpcs.api.jnbt.CompoundTag;
import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.trait.Toggleable;
import net.minecraft.server.v1_6_R2.NBTTagCompound;
import net.minecraft.server.v1_6_R2.TileEntity;


import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_6_R2.CraftWorld;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;
import org.dynmap.DynmapCommonAPI;

public class BuilderTrait extends Trait implements Toggleable {

	private Builder plugin;

	private boolean isToggled = true;

	public BuilderTrait() {
		super("builder");
	}

	@Override
	public void load(DataKey key) throws NPCLoadException {

		plugin = (Builder) Bukkit.getServer().getPluginManager().getPlugin("Builder");

		if( key.keyExists("Origin")){
			try {
				Origin = new Location(plugin.getServer().getWorld(key.getString("Origin.world")), key.getDouble("Origin.x"),key.getDouble("Origin.y"), key.getDouble("Origin.z"), (float) key.getDouble("Origin.yaw"), (float) key.getDouble("Origin.pitch"));
			} catch (Exception e) {
				Origin = null;
			}
			if(  Origin.getWorld() == null ) Origin = null;
		}

		if( key.keyExists("ContinueLoc")){
			try {
				ContinueLoc = new Location(plugin.getServer().getWorld(key.getString("ContinueLoc.world")), key.getDouble("ContinueLoc.x"),key.getDouble("ContinueLoc.y"), key.getDouble("ContinueLoc.z"), (float) key.getDouble("ContinueLoc.yaw"), (float) key.getDouble("ContinueLoc.pitch"));
			} catch (Exception e) {
				ContinueLoc = null;
			}
			if(  ContinueLoc.getWorld() == null ) ContinueLoc = null;
		}

		IgnoreAir = key.getBoolean("IgnoreAir", false);
		IgnoreLiquid = key.getBoolean("IgnoreLiquid", false);
		Excavate = key.getBoolean("Excavate", false);
		SchematicName =  key.getString("Schematic",null);
		State = BuilderState.valueOf(key.getString("State","idle"));
		oncancel = key.getString("oncancel",null);
		oncomplete = key.getString("oncomplete",null);
		onStart = key.getString("onstart",null);
		HoldItems = key.getBoolean("HoldItems",plugin.getConfig().getBoolean("DefaultOptions.Holditems", true));
		RequireMaterials = key.getBoolean("RequireMaterials",plugin.getConfig().getBoolean("DefaultOptions.RequireMaterials", false));

		try {
			if(key.keyExists("NeededMaterials")){
				org.bukkit.configuration.MemorySection derp = (org.bukkit.configuration.MemorySection) key.getRaw("NeededMaterials");
				Set<String> keys = derp.getKeys(false);
				for (String k : keys){
					NeededMaterials.put(Integer.valueOf(k),(double) derp.getInt(k));
				}
			}
		} catch (Exception e) {
		}

		Yoffset = key.getInt("YOffset");
		BuildYLayers = key.getInt("YLayers");

		MoveTimeout = key.getDouble("MoveTimeoutSeconds", plugin.getConfig().getDouble("DefaultOptions.MoveTimeoutSeconds",2.0));
		if(MoveTimeout < .1) MoveTimeout = .1;

		try {
			BuildPatternXY = BuildPatternsXZ.valueOf( key.getString("PatternXY","spiral"));	
		} catch (Exception e) {
			// TODO: handle exception
		}


		if (SchematicName !=null){
			File dir= new File(plugin.schematicsFolder);
			try {
				schematic = MCEditSchematicFormat.load(dir, SchematicName);
				if (schematic == null) {
					plugin.getLogger().log(java.util.logging.Level.WARNING,"Error loading schematic " + SchematicName +" for " + npc.getName());
				}
			} catch (Exception e) {
				plugin.getLogger().log(java.util.logging.Level.WARNING,"Error loading schematic " + SchematicName +" for " + npc.getName() + ": " + e.getMessage());
			}			
		}

		loaded = true;

	}

	private boolean loaded = false;

	@Override
	public void onSpawn() {

		plugin = (Builder) Bukkit.getPluginManager().getPlugin("Builder");

		//plugin.getLogger().info("Builder Spawn: " + npc.getName());


		if (this.loaded ==false ) {
			try {
				load(new net.citizensnpcs.api.util.MemoryDataKey());
			} catch (NPCLoadException e) {
			}
		}

		npc.getNavigator().getDefaultParameters().avoidWater(false);

		if (State == BuilderState.building || State ==BuilderState.collecting){
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				public void run() {
					State = BuilderState.idle;
					TryBuild(plugin.getServer().getConsoleSender());
				}
			}, 20);
		}
		else State = BuilderState.idle;
	}

	@Override
	public void onRemove() {
		this.State = BuilderState.idle;
	}


	@Override
	public void save(DataKey key) {
		key.setBoolean("toggled", true);
		key.setBoolean("IgnoreAir",IgnoreAir);
		key.setBoolean("IgnoreLiquid",IgnoreLiquid);
		key.setBoolean("Excavate",Excavate);
		key.setString("State", State.toString());
		key.setString("PatternXY",BuildPatternXY.toString());
		key.setBoolean("HoldItems", HoldItems);
		key.setBoolean("RequireMaterials",RequireMaterials);
		key.setDouble("MoveTimeoutSeconds", MoveTimeout);
		key.setInt("YOffset", Yoffset);
		key.setInt("YLayers", BuildYLayers);

		if (NeededMaterials.size() > 0)  key.setRaw("NeededMaterials",NeededMaterials);
		else if (key.keyExists("NeededMaterials")) key.removeKey("NeededMaterials");

		if(oncancel!=null) key.setString("oncancel", oncancel);
		else if(key.keyExists("oncancel")) key.removeKey("oncancel");

		if(onStart!=null) key.setString("onstart", onStart);
		else if(key.keyExists("onstart")) key.removeKey("onstart");

		if(oncomplete!=null) key.setString("oncomplete",oncomplete);
		else if(key.keyExists("oncomplete")) key.removeKey("oncomplete");

		if (Origin!=null){
			key.setDouble("Origin.x", Origin.getX());
			key.setDouble("Origin.y", Origin.getY());
			key.setDouble("Origin.z", Origin.getZ());
			key.setString("Origin.world", Origin.getWorld().getName());
			key.setDouble("Origin.yaw", Origin.getYaw());
			key.setDouble("Origin.pitch", Origin.getPitch());		
		}
		else if(key.keyExists("Origin")) key.removeKey("Origin");

		if (ContinueLoc!=null){
			key.setDouble("ContinueLoc.x", ContinueLoc.getX());
			key.setDouble("ContinueLoc.y", ContinueLoc.getY());
			key.setDouble("ContinueLoc.z", ContinueLoc.getZ());
			key.setString("ContinueLoc.world", ContinueLoc.getWorld().getName());
			key.setDouble("ContinueLoc.yaw", ContinueLoc.getYaw());
			key.setDouble("ContinueLoc.pitch", ContinueLoc.getPitch());		
		}
		else if(key.keyExists("ContinueLoc")) key.removeKey("ContinueLoc");

		if(SchematicName!=null)	key.setString("Schematic",SchematicName);
		else if(key.keyExists("Schematic")) key.removeKey("Schematic");

	}

	@Override
	public boolean toggle() {
		isToggled = !isToggled;
		return isToggled;
	}

	public boolean isToggled() {
		return isToggled;
	}


	public BuilderState State = BuilderState.idle;
	public BuilderSchematic schematic = null;
	public String SchematicName = null;
	public Boolean IgnoreAir, IgnoreLiquid, Excavate;
	public Boolean RequireMaterials = false;
	public Location Origin = null;
	public Location ContinueLoc = null;
	public String onStart = null;
	public String oncomplete = null;
	public String oncancel = null;
	public Boolean HoldItems = true;
	public Boolean GroupByLayer = true;
	public Integer BuildYLayers = 1;
	public Integer Yoffset = 0;
	public BuildPatternsXZ BuildPatternXY = BuildPatternsXZ.spiral;
	public double MoveTimeout = 2.0;

	public Map<Integer, Double> NeededMaterials = new HashMap<Integer, Double>();

	public	 Queue<EmptyBuildBlock> Q = new LinkedList<EmptyBuildBlock>();

	public enum BuilderState {idle, building, marking, collecting};
	public enum BuildPatternsXZ {spiral, reversespiral, linear, reverselinear};
	private boolean clearingMarks = false;

	Map<Player, Long> Sessions = new  HashMap<Player, Long>();

	public String GetMatsList(boolean excavate){
		if(!npc.isSpawned()) return "";
		if (schematic == null) return "";
		if (this.State != BuilderState.idle) return ChatColor.RED + "Cannot survey while building";

		Location start = null;

		if (Origin !=null) start = Origin.clone(); 
		else if (ContinueLoc!=null) start = ContinueLoc.clone();
		else start = npc.getBukkitEntity().getLocation().clone();

		try {
			NeededMaterials = Util.MaterialsList(schematic.BuildQueue(start, true, true, excavate, BuildPatternsXZ.linear ,false , 1,0));

		} catch (Exception e) {
			plugin.getServer().getConsoleSender().sendMessage(e.getMessage());
		}

		return Util.printList(NeededMaterials);

	}

	public boolean TryBuild(CommandSender sender){

		if (sender == null) sender = this.sender;

		this.sender = sender;

		if (this.RequireMaterials){
			java.util.Iterator<Entry<Integer, Double>> it = NeededMaterials.entrySet().iterator();
			long c = 0;
			while (it.hasNext()){
				c+= it.next().getValue();
			}	

			if (c>0){
				sender.sendMessage(plugin.format(plugin.CollectingMessage, npc, schematic, sender, SchematicName, c+""));
				this.State =BuilderState.collecting;
				return true;
			}


		}

		return StartBuild(sender);
	}


	//	public String GetComparisonMatsList(Player player){
	//		if(!npc.isSpawned()) return "";
	//		if (schematic == null) return "";
	//		if (this.State != BuilderState.idle) return "";
	//
	//		Location start = null;
	//		if (Origin !=null) start = Origin.clone(); 
	//		else if (ContinueLoc!=null) start = ContinueLoc.clone();
	//		else start = npc.getBukkitEntity().getLocation().clone();
	//
	//		Map<Integer, Integer> derp = Util.MaterialsList(schematic.BuildQueue(start, true, true, false, BuildPatternsXZ.linear ,false , 1, 0));
	//
	//		ListIterator<ItemStack> pit = player.getInventory().iterator();
	//		Map<Material, Integer> pinv = new HashMap<Material,Integer>();
	//		while (pit.hasNext()){
	//			ItemStack p = pit.next();
	//			if (pinv.containsKey(p.getType())){
	//				Integer amt = pinv.get(p.getType())  +  (Integer)p.getAmount();
	//				pinv.put(p.getType(), amt);
	//			}
	//			else 		pinv.put(p.getType(), p.getAmount());
	//		}
	//
	//		StringBuilder sb = new StringBuilder();
	//
	//		java.util.Iterator<Entry<Integer, Integer>> it = derp.entrySet().iterator();
	//
	//		while (it.hasNext()){
	//			Entry<Integer, Integer> i = it.next();
	//			int pamt = 0;
	//			if(pinv.containsKey(i.getKey())) pamt = pinv.get(i.getKey());
	//			if (pamt >= i.getValue()) sb.append(ChatColor.GREEN);
	//			else sb.append(ChatColor.RED);	
	//			sb.append(i.getKey() + ":" + i.getValue());
	//			if(it.hasNext())sb.append(ChatColor.RESET + ", ");
	//		}
	//
	//		return sb.toString();
	//	}


	public long startingcount = 1;

	private boolean StartBuild(CommandSender player){
		if(!npc.isSpawned()) return false;
		if (schematic == null) return false;
		if (this.State == BuilderState.building) return false;

		Location start = null;

		if (Origin !=null) start = Origin.clone(); 
		else if (ContinueLoc!=null) start = ContinueLoc.clone();
		else start = npc.getBukkitEntity().getLocation().clone();

		Q = schematic.BuildQueue(start, IgnoreLiquid, IgnoreAir,Excavate, this.BuildPatternXY,this.GroupByLayer, this.BuildYLayers, this.Yoffset);

		startingcount = Q.size();
		ContinueLoc = start.clone();

		mypos = npc.getBukkitEntity().getLocation().clone();

		this.State = BuilderState.building;

		NeededMaterials.clear();

		sender.sendMessage(plugin.format(plugin.StartedMessage, npc,schematic, player, null, "0"));

		if (onStart!=null){
			String resp = plugin.runTask(onStart, npc);
			if (resp ==null) sender.sendMessage("Task " + onStart + " completed.");
			else sender.sendMessage("Task " + onStart + " could not be run: " + resp);				
		}

		plugin.DenizenAction(npc, "Build Start");
		plugin.DenizenAction(npc, "Build " + schematic.Name + " Start");

		SetupNextBlock();

		return true;
	}

	private BuilderSchematic _schematic = null;

	private Location mypos = null;

	private Queue<EmptyBuildBlock> marks = new LinkedList<EmptyBuildBlock>();
	private Queue<EmptyBuildBlock> _marks = new LinkedList<EmptyBuildBlock>();

	public boolean StartMark(int mat){
		if(!npc.isSpawned()) return false;
		if (schematic == null) return false;
		if (this.State != BuilderState.idle) return false;

		oncomplete = null;
		oncancel = null;
		onStart = null;
		_schematic = schematic;

		mypos = npc.getBukkitEntity().getLocation().clone();

		schematic = new BuilderSchematic();
		schematic.Name = _schematic.Name;

		if (Origin==null){
			ContinueLoc = this.npc.getBukkitEntity().getLocation().clone();
		}
		else{
			ContinueLoc = Origin.clone();
		}
		Q = schematic.CreateMarks(_schematic.width(), _schematic.height(), _schematic.length(), mat);
		this.State = BuilderState.marking;

		SetupNextBlock();

		return true;
	}

	private CommandSender sender =null; 

	private EmptyBuildBlock next = null;
	private Block pending = null;

	public void SetupNextBlock(){

		if(marks.isEmpty()){
			if(schematic ==null) {
				CancelBuild();
				return;
			}
			//
			//			next = Q.poll();
			//			if (next == null) {
			//				CompleteBuild();
			//				return;
			//			}
			//
			//			pending = ContinueLoc.getWorld().getBlockAt(schematic.offset(next,ContinueLoc).toLocation(ContinueLoc.getWorld()));

			boolean ok;
			int i = 0;
			do{

				i++;

				if(i>500){
					//anti lag measure.
					plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin,new Runnable(){
						public void run() {
							SetupNextBlock();
						}
					});	
					return;
				}

				next = Q.poll();

				if (next == null) {
					CompleteBuild();
					return;
				}

				pending = ContinueLoc.getWorld().getBlockAt(schematic.offset(next,ContinueLoc));

				ok= true;

				//dont replace grass with dirt, and vice versa.
				if (pending.getTypeId() == 3 && next.getMat().getItemTypeId() ==2) ok = false;
				if (pending.getTypeId() == 2 && next.getMat().getItemTypeId() ==3) ok = false;
				if (pending.getTypeId() == next.getMat().getItemTypeId() && pending.getData() == next.getMat().getData()) ok =false;
				//dont bother putting a block that already exists.
			} while(!ok);



		}
		else{
			clearingMarks = true;
			next = marks.remove();
			pending = ContinueLoc.getWorld().getBlockAt(next.X, next.Y, next.Z);

		}


		if(npc.isSpawned()){

			if((npc.getBukkitEntity() instanceof org.bukkit.entity.HumanEntity || npc.getBukkitEntity() instanceof org.bukkit.entity.Enderman) && this.HoldItems){
				int m = next.getMat().getItemTypeId();
				if (m <=0) m = 278;

				if((npc.getBukkitEntity() instanceof org.bukkit.entity.HumanEntity) && this.HoldItems)((org.bukkit.entity.HumanEntity) npc.getBukkitEntity()).getInventory().setItemInHand(new ItemStack(m));	
				else if((npc.getBukkitEntity() instanceof org.bukkit.entity.Enderman) && this.HoldItems)	((org.bukkit.entity.Enderman) npc.getBukkitEntity()).setCarriedMaterial(new MaterialData(m));
			}
		}


		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				if (npc.isSpawned()){
					npc.getNavigator().setTarget(findaspot(pending).add(.5, 1, .5));
					npc.getNavigator().getLocalParameters().stationaryTicks((int) (MoveTimeout*20));
					npc.getNavigator().getLocalParameters().stuckAction(BuilderTeleportStuckAction.INSTANCE);
				}	
			}
		});

		canceltaskid =	plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				if (npc.isSpawned()){
					if(npc.getNavigator().isNavigating()){
						npc.getBukkitEntity().teleport(npc.getNavigator().getTargetAsLocation());
						npc.getNavigator().cancelNavigation();
					}		
				}
			}
		}, (long) (MoveTimeout*20)+1);

	}

	int dirz = 1, dirx = 1;

	public void CancelBuild(){
		if (oncancel!=null) plugin.runTask(oncancel, npc);
		plugin.DenizenAction(npc, "Build Cancel");
		if(schematic !=null)  plugin.DenizenAction(npc, "Build " + schematic.Name + " Cancel");

		stop();
	}

	public void CompleteBuild(){
		if (sender == null) sender = plugin.getServer().getConsoleSender();

		if (this.State == BuilderState.building){
			sender.sendMessage(plugin.format(plugin.CompleteMessage, npc,schematic, sender,null,"0"));

			if (oncomplete!=null){
				String resp = plugin.runTask(oncomplete, npc);
				if (resp ==null) sender.sendMessage("Task " + oncomplete + " completed.");
				else sender.sendMessage("Task " + oncomplete + " could not be run: " + resp);				
			}

			plugin.DenizenAction(npc, "Build Complete");
			plugin.DenizenAction(npc, "Build " + schematic.Name + " Complete");
		}

		stop ();
	}

	private void stop(){
		boolean stop = State == BuilderState.building;
		if(canceltaskid > 0) plugin.getServer().getScheduler().cancelTask((int) canceltaskid);

		if (this.State ==BuilderState.marking){
			this.State =BuilderState.idle;
			if(Origin !=null) npc.getNavigator().setTarget(Origin);
			else npc.getBukkitEntity().teleport(mypos);
			marks.addAll(_marks);
			_marks.clear();
			if (_schematic !=null) schematic = _schematic;
			_schematic = null;
		}
		else{
			this.State =BuilderState.idle;
			if (stop && npc.isSpawned()){	
				if (npc.getNavigator().isNavigating())	npc.getNavigator().cancelNavigation();
				npc.getNavigator().setTarget(mypos);
			}
		}


		if((npc.getBukkitEntity() instanceof org.bukkit.entity.HumanEntity) && this.HoldItems)((org.bukkit.entity.HumanEntity) npc.getBukkitEntity()).getInventory().setItemInHand(new ItemStack(0));	
		else if((npc.getBukkitEntity() instanceof org.bukkit.entity.Enderman) && this.HoldItems)	((org.bukkit.entity.Enderman) npc.getBukkitEntity()).setCarriedMaterial(new MaterialData(0));

		if (stop && plugin.getServer().getPluginManager().getPlugin("dynmap") != null){
			if (plugin.getServer().getPluginManager().getPlugin("dynmap").isEnabled()) {
				org.dynmap.DynmapCommonAPI dyn  = (DynmapCommonAPI) (plugin.getServer().getPluginManager().getPlugin("dynmap"));
				dyn.triggerRenderOfVolume(npc.getBukkitEntity().getWorld().getName(), this.ContinueLoc.getBlockX() - schematic.width()/2, this.ContinueLoc.getBlockY(),  this.ContinueLoc.getBlockZ() - schematic.length()/2,  this.ContinueLoc.getBlockX() + schematic.width()/2,  this.ContinueLoc.getBlockY() + schematic.height()/2,  this.ContinueLoc.getBlockZ() + schematic.length()/2);
			}
		}


		sender = null;
		oncomplete = null;
		oncancel = null;
		onStart= null;
		ContinueLoc = null;

	}

	private long canceltaskid;
	public void PlaceNextBlock(){

		if(canceltaskid > 0) plugin.getServer().getScheduler().cancelTask((int) canceltaskid);

		if(pending != null && next != null) {

			if(State==BuilderState.marking && !clearingMarks) {
				_marks.add(new DataBuildBlock(pending.getX(), pending.getY(), pending.getZ(), pending.getTypeId(), pending.getData()));
			}

			pending.setTypeIdAndData(next.getMat().getItemTypeId(), next.getMat().getData(), false);
		
			if (next instanceof TileBuildBlock){			
				//lol what
				CraftWorld cw =(CraftWorld)pending.getWorld();			
				CompoundTag nbt = new CompoundTag("", ((TileBuildBlock) next).tiles);
				NBTTagCompound nmsnbt = (NBTTagCompound) Util.fromNative(nbt);
				nmsnbt.setInt("x", pending.getX());
				nmsnbt.setInt("y", pending.getY());
				nmsnbt.setInt("z", pending.getZ());			
				TileEntity te = cw.getHandle().getTileEntity(pending.getX(), pending.getY(), pending.getZ());		
				te.a(nmsnbt);
			}


			if(this.npc.getBukkitEntity() instanceof org.bukkit.entity.Player)	{
				//arm swing
				net.citizensnpcs.util.PlayerAnimation.ARM_SWING.play((Player) this.npc.getBukkitEntity(), 64);
			}
		}

		if (marks.size()==0) clearingMarks = false;

		//setup next
		SetupNextBlock();

	}

	//TODO: make this less... awful.
	//Given a BuildBlock to place, find a good place to stand to place it.
	private Location findaspot(Block base){
		if(base ==null ) return null;

		for (int a=3; a>=-5;a--){
			if(Util.canStand(base.getRelative(0, a, -1))) return  base.getRelative(0, a-1, -1).getLocation();
			if(Util.canStand(base.getRelative(0, a, 1))) return  base.getRelative(0, a-1, 1).getLocation();
			if(Util.canStand(base.getRelative(1, a, 0))) return  base.getRelative(1, a-1, 0).getLocation();
			if(Util.canStand(base.getRelative(-1, a, 0))) return  base.getRelative(-1, a-1, 0).getLocation();
			if(Util.canStand(base.getRelative(-1, a, -1))) return  base.getRelative(-1, a-1, -1).getLocation();
			if(Util.canStand(base.getRelative(-1, a, 1))) return  base.getRelative(-1, a-1, 1).getLocation();
			if(Util.canStand(base.getRelative(1, a, 1))) return  base.getRelative(1, a-1, 1).getLocation();
			if(Util.canStand(base.getRelative(1, a, -1))) return  base.getRelative(1, a-1, -1).getLocation();
		}


		return base.getLocation();

	}




}





