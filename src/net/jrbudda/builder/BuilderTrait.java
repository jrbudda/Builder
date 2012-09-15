package net.jrbudda.builder;

import java.io.File;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.MaterialData;

import net.citizensnpcs.api.exception.NPCLoadException;

import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.trait.Toggleable;
import net.minecraft.server.Packet;
import net.minecraft.server.Packet18ArmAnimation;

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

		plugin.getLogger().info("Builder Spawn: " + npc.getName());


		if (this.loaded ==false ) {
			try {
				load(new net.citizensnpcs.api.util.MemoryDataKey());
			} catch (NPCLoadException e) {
			}
		}

		anim = new Packet18ArmAnimation( ((CraftEntity)npc.getBukkitEntity()).getHandle(),1);

		npc.getNavigator().getDefaultParameters().avoidWater(false);

		if (State == BuilderState.building){
			plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
				public void run() {
					State = BuilderState.idle;
					StartBuild(plugin.getServer().getConsoleSender(),IgnoreAir, IgnoreLiquid, Excavate);
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
		if(oncancel!=null) key.setString("oncancel", oncancel);
		if(oncomplete!=null) key.setString("oncomplete",oncomplete);

		if (Origin!=null){
			key.setDouble("Origin.x", Origin.getX());
			key.setDouble("Origin.y", Origin.getY());
			key.setDouble("Origin.z", Origin.getZ());
			key.setString("Origin.world", Origin.getWorld().getName());
			key.setDouble("Origin.yaw", Origin.getYaw());
			key.setDouble("Origin.pitch", Origin.getPitch());		
		}

		if (ContinueLoc!=null){
			key.setDouble("ContinueLoc.x", ContinueLoc.getX());
			key.setDouble("ContinueLoc.y", ContinueLoc.getY());
			key.setDouble("ContinueLoc.z", ContinueLoc.getZ());
			key.setString("ContinueLoc.world", ContinueLoc.getWorld().getName());
			key.setDouble("ContinueLoc.yaw", ContinueLoc.getYaw());
			key.setDouble("ContinueLoc.pitch", ContinueLoc.getPitch());		
		}

		if(SchematicName!=null)	key.setString("Schematic",SchematicName);

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
	public String oncomplete = null;
	public String oncancel = null;

	Packet anim = null;
	public enum BuilderState {idle, building, marking};
	private boolean clearingMarks = false;

	public String GetMatsList(){
		if(!npc.isSpawned()) return "";
		if (schematic == null) return "";
		if (this.State != BuilderState.idle) return "";

		Map<Material, Integer> derp = schematic.MaterialsList();

		StringBuilder sb = new StringBuilder();

		java.util.Iterator<Entry<Material, Integer>> it = derp.entrySet().iterator();

		while (it.hasNext()){
			Entry<Material, Integer> i = it.next();
			sb.append(i.getKey() + ":" + i.getValue());
			if(it.hasNext())sb.append(", ");
		}
		return sb.toString();
	}


	public String GetComparisonMatsList(Player player){
		if(!npc.isSpawned()) return "";
		if (schematic == null) return "";
		if (this.State != BuilderState.idle) return "";

		Map<Material, Integer> derp = schematic.MaterialsList();

		ListIterator<ItemStack> pit = player.getInventory().iterator();
		Map<Material, Integer> pinv = new HashMap<Material,Integer>();
		while (pit.hasNext()){
			ItemStack p = pit.next();
			if (pinv.containsKey(p.getType())){
				Integer amt = pinv.get(p.getType())  +  (Integer)p.getAmount();
				pinv.put(p.getType(), amt);
			}
			else 		pinv.put(p.getType(), p.getAmount());
		}

		StringBuilder sb = new StringBuilder();

		java.util.Iterator<Entry<Material, Integer>> it = derp.entrySet().iterator();

		while (it.hasNext()){
			Entry<Material, Integer> i = it.next();
			int pamt = 0;
			if(pinv.containsKey(i.getKey())) pamt = pinv.get(i.getKey());
			if (pamt >= i.getValue()) sb.append(ChatColor.GREEN);
			else sb.append(ChatColor.RED);	
			sb.append(i.getKey() + ":" + i.getValue());
			if(it.hasNext())sb.append(ChatColor.RESET + ", ");
		}

		return sb.toString();
	}


	public boolean StartBuild(CommandSender player, boolean air, boolean liquid, boolean excavate){
		if(!npc.isSpawned()) return false;
		if (schematic == null) return false;
		if (this.State != BuilderState.idle) return false;

		Location start = null;

		if (Origin !=null) start = Origin.clone(); 
		else if (ContinueLoc!=null) start = ContinueLoc.clone();
		else start = npc.getBukkitEntity().getLocation().clone();

		schematic.Reset(start, liquid, air,excavate);

		ContinueLoc = start.clone();

		IgnoreAir  = air;
		IgnoreLiquid = liquid;
		Excavate = excavate;

		mypos = npc.getBukkitEntity().getLocation().clone();

		this.State = BuilderState.building;
		this.sender = player;
		sender.sendMessage(plugin.format(plugin.StartedMessage, npc,schematic));
		SetupNextBlock();

		return true;
	}

	private BuilderSchematic _schematic = null;

	private Location mypos = null;

	private Queue<BuildBlock> marks = new LinkedList<BuildBlock>();
	private Queue<BuildBlock> _marks = new LinkedList<BuildBlock>();

	public boolean StartMark(int mat){
		if(!npc.isSpawned()) return false;
		if (schematic == null) return false;
		if (this.State != BuilderState.idle) return false;

		oncomplete = null;
		oncancel = null;
		_schematic = schematic;

		mypos = npc.getBukkitEntity().getLocation().clone();

		schematic = new BuilderSchematic();
		schematic.Name = _schematic.Name;
		if (Origin==null){
			schematic.CreateMarks(this.npc.getBukkitEntity().getLocation(),_schematic.width(), _schematic.height(), _schematic.length(), mat);
		}
		else{
			schematic.CreateMarks(Origin,_schematic.width(), _schematic.height(), _schematic.length(), mat);
		}

		this.State = BuilderState.marking;

		SetupNextBlock();

		return true;
	}

	private CommandSender sender =null; 

	private BuildBlock next = null;
	private Block pending = null;

	public void SetupNextBlock(){

		if(marks.isEmpty()){
			if(schematic ==null) {
				CancelBuild();
				return;
			}

			int i = 0;

			do{
				i++;

				if(i>100){
					//anti lag measure.
					plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin,new Runnable(){
						public void run() {
							SetupNextBlock();
						}
					});		
					return;
				}

				next = schematic.getNext();

				if (next == null) {
					CompleteBuild();
					return;
				}

				pending = this.npc.getBukkitEntity().getWorld().getBlockAt(next.X,next.Y,next.Z);

				//dont replace grass with dirt, and vice versa.
				if (pending.getTypeId() == 3 && next.mat.getItemTypeId() ==2) continue;
				if (pending.getTypeId() == 2 && next.mat.getItemTypeId() ==3) continue;

				//dont bother putting a block that already exists.
			} while ((pending.getTypeId() == next.mat.getItemTypeId() && pending.getData() == next.mat.getData()));

		}
		else{
			clearingMarks = true;
			next = marks.remove();
			pending = this.npc.getBukkitEntity().getWorld().getBlockAt(next.X,next.Y,next.Z);
		}


		if(npc.getBukkitEntity() instanceof org.bukkit.entity.HumanEntity && plugin.HoldItems){
			int m = next.mat.getItemTypeId();
			if (m <=0) m = 278;
			((org.bukkit.entity.Player) npc.getBukkitEntity()).getInventory().setItemInHand(new ItemStack(m));

		}


		canceltaskid =	plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				if(npc.getNavigator().isNavigating()){
					npc.getBukkitEntity().teleport(npc.getNavigator().getTargetAsLocation().clone().add(0, 1, 0));
					npc.getNavigator().cancelNavigation();
				}
			}
		}, 41);

		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				npc.getNavigator().setTarget(findaspot(next));
				npc.getNavigator().getLocalParameters().stationaryTicks(40);
				npc.getNavigator().getLocalParameters().stuckAction(BuilderTeleportStuckAction.INSTANCE);
			}
		}, 1);

	}

	int dirz = 1, dirx = 1;

	public void CancelBuild(){
		if (oncancel!=null) plugin.runTask(oncancel, npc);
		stop();
	}

	public void CompleteBuild(){
		if (sender == null) sender = plugin.getServer().getConsoleSender();

		sender.sendMessage(plugin.format(plugin.CompleteMessage, npc,schematic));

		if (oncomplete!=null){
			String resp = plugin.runTask(oncomplete, npc);
			if (resp ==null) sender.sendMessage("Task " + oncomplete + "started");
			else sender.sendMessage("Task " + oncomplete + " could not be started: " + resp);				
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
		}
		else{
			this.State =BuilderState.idle;
			if (stop && npc.isSpawned()){	
				if (npc.getNavigator().isNavigating())	npc.getNavigator().cancelNavigation();
				npc.getNavigator().setTarget(mypos);
			}
		}
		sender = null;
		oncomplete = null;
		oncancel = null;
		ContinueLoc = null;
	}

	private long canceltaskid;
	public void PlaceNextBlock(){

		if(canceltaskid > 0) plugin.getServer().getScheduler().cancelTask((int) canceltaskid);

		if(pending != null && next != null) {

			if(State==BuilderState.marking && !clearingMarks) {

				BuildBlock derp = new BuildBlock();
				derp.mat = new MaterialData(pending.getTypeId(), pending.getData());
				derp.X = pending.getX();
				derp.Y = pending.getY();
				derp.Z = pending.getZ();
				_marks.add(derp);
			}

			//change block
			pending.setTypeIdAndData(next.mat.getItemTypeId(), next.mat.getData(), false);

			//arm swing
			net.citizensnpcs.util.Util.sendPacketNearby(npc.getBukkitEntity().getLocation(),anim , 64);
		}

		if (marks.size()==0) clearingMarks = false;

		//setup next
		SetupNextBlock();

	}

	//TODO: make this less... awful.
	//Given a BuildBlock to place, find a good place to stand to place it.
	private Location findaspot(BuildBlock block){
		if(block ==null ) return null;

		Block base = npc.getBukkitEntity().getLocation().getWorld().getBlockAt(block.X, block.Y, block.Z);

		for (int a=3; a>=-5;a--){
			if(canStand(base.getRelative(0, a, -1))) return  base.getRelative(0, a-1, -1).getLocation();
		}

		for (int a=3; a>=-5;a--){
			if(canStand(base.getRelative(0, a, 1))) return  base.getRelative(0, a-1, 1).getLocation();
		}

		for (int a=3; a>=-5;a--){
			if(canStand(base.getRelative(1, a, 0))) return  base.getRelative(1, a-1, 0).getLocation();
		}

		for (int a=3; a>=-5;a--){
			if(canStand(base.getRelative(-1, a, 0))) return  base.getRelative(-1, a-1, 0).getLocation();
		}

		for (int a=3; a>=-5;a--){
			if(canStand(base.getRelative(-1, a, -1))) return  base.getRelative(-1, a-1, -1).getLocation();
		}

		for (int a=3; a>=-5;a--){
			if(canStand(base.getRelative(-1, a, 1))) return  base.getRelative(-1, a-1, 1).getLocation();
		}

		for (int a=3; a>=-5;a--){
			if(canStand(base.getRelative(1, a, 1))) return  base.getRelative(1, a-1, 1).getLocation();
		}

		for (int a=3; a>=-5;a--){
			if(canStand(base.getRelative(1, a, -1))) return  base.getRelative(1, a-1, -1).getLocation();
		}


		return base.getLocation();

	}


	private boolean canStand(Block base){
		return base.getRelative(0, -1, 0).isEmpty() == false && base.getRelative(0, -1, 0).isLiquid() == false && (base.isEmpty()||(base.isLiquid()));		
	}

}
