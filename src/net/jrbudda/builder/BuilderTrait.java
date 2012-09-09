package net.jrbudda.builder;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.entity.CraftEntity;
import org.bukkit.util.Vector;

import net.citizensnpcs.api.exception.NPCLoadException;

import net.citizensnpcs.api.trait.Trait;
import net.citizensnpcs.api.util.DataKey;
import net.citizensnpcs.trait.Toggleable;
import net.jrbudda.builder.BuilderSchematic.BuildBlock;
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
		if (!this.getNPC().hasTrait(BuilderTrait.class)) return;

		plugin = (Builder) Bukkit.getServer().getPluginManager().getPlugin("Builder");

		//		if (thisInstance !=null ){
		//			thisInstance.cancelRunnable();
		//			if (thisInstance.myNPC != null) thisInstance.myNPC.despawn();
		//			thisInstance = null;
		//		}
		//
		//
		//		thisInstance = new SentryInstance(plugin);
		//		thisInstance.myTrait = this;
		//
		//		if(key.keyExists("traits")) key = key.getRelative("traits");
		//
		//		isToggled=	key.getBoolean("toggled", true);
		//		thisInstance.Retaliate=	key.getBoolean("Retaliate", true);
		//		thisInstance.Invincible=	key.getBoolean("Invincinble", false);
		//		thisInstance.DropInventory=	key.getBoolean("DropInventory", false);
		//		thisInstance.FriendlyFire = key.getBoolean("FriendlyFire",false);
		//		thisInstance.LuckyHits=	key.getBoolean("CriticalHits", true);
		//		thisInstance.sentryHealth=	key.getInt("Health", 20);
		//		thisInstance.sentryRange=	key.getInt("Range", 10);
		//		thisInstance.RespawnDelaySeconds=	key.getInt("RespawnDelay", 10);
		//		thisInstance.sentrySpeed=	(float) (key.getDouble("Speed",1.0));
		//		thisInstance.sentryWeight=	 (key.getDouble("Weight",1.0));
		//		thisInstance.Armor=		key.getInt("Armor", 0);
		//		thisInstance.Strength=		key.getInt("Strength", 1);
		//		thisInstance.guardTarget = (key.getString("GuardTarget", null));
		//		thisInstance.GreetingMessage = (key.getString("Greeting", "'§b<NPC> says Welcome, <PLAYER>'"));
		//		thisInstance.WarningMessage = (key.getString("Warning", "'§c<NPC> says Halt! Come no closer!'"));
		//		thisInstance.WarningRange = key.getInt("WarningRange", 0);
		//		thisInstance.AttackRateSeconds =  (key.getDouble("AttackRate",2.0));
		//		thisInstance.HealRate =  (key.getDouble("HealRate",0.0));
		//		thisInstance.NightVision = key.getInt("NightVision",16);
	}


	@Override
	public void onSpawn() {

		plugin = (Builder) Bukkit.getPluginManager().getPlugin("Builder");

		anim = new Packet18ArmAnimation( ((CraftEntity)npc.getBukkitEntity()).getHandle(),1);

	}

	@Override
	public void onRemove() {

		this.State = BuilderState.idle;

	}

	@Override
	public void save(DataKey key) {
		key.setBoolean("toggled", true);
		//		key.setInt("Strength", thisInstance.Strength);
		//		key.setInt("WarningRange", thisInstance.WarningRange);
		//		key.setDouble("AttackRate", thisInstance.AttackRateSeconds);
		//		key.setBoolean("FriendlyFire", thisInstance.FriendlyFire);
		//		key.setInt("NightVision", thisInstance.NightVision);
		//		if (thisInstance.guardTarget !=null)	key.setString("GuardTarget", thisInstance.guardTarget);
		//		key.setString("Warning",thisInstance.WarningMessage);
		//		key.setString("Greeting",thisInstance.GreetingMessage);
	}

	@Override
	public boolean toggle() {
		isToggled = !isToggled;
		return isToggled;
	}

	public boolean isToggled() {
		return isToggled;
	}

	public enum BuilderState {idle, building};

	public BuilderState State = BuilderState.idle;
	public BuilderSchematic Schematic = null;
	Packet anim = null;

	public boolean StartBuild(){
		if(!npc.isSpawned()) return false;
		if (Schematic == null) return false;
		if (this.State != BuilderState.idle) return false;

		Schematic.Reset(npc);

		this.State = BuilderState.building;

		SetupNextBlock();

		return true;
	}

	private BuildBlock next = null;

	public void SetupNextBlock(){
		if(Schematic ==null) {
			CancelBuild();
			return;
		}
		next = Schematic.getNext();
		if (next == null) CancelBuild();	

		canceltaskid=	plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				if(npc.getNavigator().isNavigating()){
					npc.getBukkitEntity().teleport(npc.getNavigator().getTargetAsLocation().add(0, 1, 0));
					npc.getNavigator().cancelNavigation();
				}
			}
		}, 41);

		plugin.getServer().getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
			public void run() {
				npc.getNavigator().setTarget(findaspot(next));
				npc.getNavigator().getLocalParameters().stationaryTicks(20);
				npc.getNavigator().getLocalParameters().stuckAction(BuilderTeleportStuckAction.INSTANCE);
			}
		}, 1);

	}


	public void CancelBuild(){
		this.State =BuilderState.idle;
		if(canceltaskid > 0) plugin.getServer().getScheduler().cancelTask((int) canceltaskid);
		if (npc.isSpawned() && npc.getNavigator().isNavigating()){
			npc.getNavigator().cancelNavigation();
		}
	}

	private long canceltaskid;

	public void PlaceNextBlock(){

		//change block
		org.bukkit.block.Block block = this.npc.getBukkitEntity().getWorld().getBlockAt(next.loc);
		block.setTypeIdAndData(next.mat.getItemTypeId(), next.mat.getData(), false);

		//arm swing
		net.citizensnpcs.util.Util.sendPacketNearby(npc.getBukkitEntity().getLocation(),anim , 64);

		//setup next
		SetupNextBlock();

	}


	//Given a BuildBlock to place, find a good place to stand to plave it.
	private Location findaspot(BuildBlock block){

		return block.loc.add(0,0,1);

		//		Location loco =  npc.getBukkitEntity().getLocation();
		//		Vector norman = loco.subtract(block.getLocation()).toVector();
		//		norman = normalizeVector(norman);
		//		norman.multiply(1);
		//
		//		Location loc =block.getLocation().add(norman);
		//
		//		return loc;

	}

	public static Vector normalizeVector(Vector victor){

		double	mag = Math.sqrt(Math.pow(victor.getX(), 2) + Math.pow(victor.getY(), 2)  + Math.pow(victor.getZ(), 2)) ;
		if (mag !=0) return victor.multiply(1/mag);

		return victor.multiply(0);

	}

}
