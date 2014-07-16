package enviromine.core;

import java.util.logging.Level;
import java.util.logging.Logger;
import net.minecraft.client.settings.KeyBinding;
import net.minecraft.command.ICommandManager;
import net.minecraft.command.ServerCommandManager;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraft.world.gen.structure.MapGenStructureIO;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;
import net.minecraftforge.common.DimensionManager;
import org.lwjgl.input.Keyboard;
import cpw.mods.fml.client.registry.KeyBindingRegistry;
import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.Mod.Instance;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPostInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.network.NetworkMod;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.common.registry.LanguageRegistry;
import cpw.mods.fml.common.registry.VillagerRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import enviromine.EM_VillageMineshaft;
import enviromine.EnviroPotion;
import enviromine.EnviroUtils;
import enviromine.core.commands.CommandPhysics;
import enviromine.core.commands.EnviroCommand;
import enviromine.core.proxies.EM_CommonProxy;
import enviromine.gui.UpdateNotification;
import enviromine.handlers.EnviroPacketHandler;
import enviromine.handlers.EnviroShaftCreationHandler;
import enviromine.handlers.ObjectHandler;
import enviromine.world.WorldProviderCaves;
import enviromine.world.biomes.BiomeGenCaves;

@Mod(modid = EM_Settings.ID, name = EM_Settings.Name, version = EM_Settings.Version)
@NetworkMod(clientSideRequired = true, serverSideRequired = true, channels = {EM_Settings.Channel}, packetHandler = EnviroPacketHandler.class)
public class EnviroMine
{
	public static Logger logger;
	public static BiomeGenBase caves;
	public static CreativeTabs enviroTab = new CreativeTabs("EnviroMine")
	{
		public ItemStack getIconItemStack()
		{
			return new ItemStack(ObjectHandler.camelPack, 1, 0);
		}
	};
	
	@Instance("EM_Instance")
	public static EnviroMine instance;
	
	@SidedProxy(clientSide = EM_Settings.Proxy + ".EM_ClientProxy", serverSide = EM_Settings.Proxy + ".EM_CommonProxy")
	public static EM_CommonProxy proxy;
	
	@EventHandler
	public static void preInit(FMLPreInitializationEvent event)
	{
		logger = event.getModLog();
		logger.setParent(FMLLog.getLogger());
		
		proxy.preInit(event);
		
		// Load Configuration files And Custom files
		EM_ConfigHandler.initConfig();
		
		LanguageRegistry.instance().addStringLocalization("itemGroup.EnviroMine", "en_US", "EnviroMine");
		
		ObjectHandler.RegisterItems();
		ObjectHandler.RegisterBlocks();
		ObjectHandler.RegisterGases();
		ObjectHandler.RegisterEntities();
		
		if(EM_Settings.shaftGen == true)
		{
			VillagerRegistry.instance().registerVillageCreationHandler(new EnviroShaftCreationHandler());
			MapGenStructureIO.func_143031_a(EM_VillageMineshaft.class, "ViMS");
		}
	}
	
	@EventHandler
	public static void init(FMLInitializationEvent event)
	{
		proxy.init(event);
		
		ObjectHandler.RegisterRecipes();
		ObjectHandler.RegisterNames();
		
		EnviroUtils.extendPotionList();
		
		EnviroPotion.RegisterPotions();
		
		GameRegistry.registerPlayerTracker(new UpdateNotification());
		
		caves = (new BiomeGenCaves(23)).setColor(16711680).setBiomeName("Caves").setDisableRain().setTemperatureRainfall(1.0F, 0.0F);
		GameRegistry.addBiome(caves);
		BiomeDictionary.registerBiomeType(caves, Type.WASTELAND);
		
		
		DimensionManager.registerProviderType(EM_Settings.caveDimID, WorldProviderCaves.class, false);
		DimensionManager.registerDimension(EM_Settings.caveDimID, EM_Settings.caveDimID);
		


		proxy.registerTickHandlers();
		proxy.registerEventHandlers();
	}
	
	@EventHandler
	public static void postInit(FMLPostInitializationEvent event)
	{
		proxy.postInit(event);
		
		if(EM_Settings.genArmorConfigs)
		{
			EM_ConfigHandler.SearchForModdedArmors();
		}
		
		EM_ConfigHandler.SearchForDimensions();
		EM_ConfigHandler.SearchForBiomes();
		
		EnviroMine.logger.log(Level.INFO, "Loaded " + EM_Settings.armorProperties.size() + " armor properties");
		EnviroMine.logger.log(Level.INFO, "Loaded " + EM_Settings.blockProperties.size() + " block properties");
		EnviroMine.logger.log(Level.INFO, "Loaded " + EM_Settings.livingProperties.size() + " entity properties");
		EnviroMine.logger.log(Level.INFO, "Loaded " + EM_Settings.itemProperties.size() + " item properties");
		EnviroMine.logger.log(Level.INFO, "Loaded " + EM_Settings.biomeProperties.size() + " biome properties");
		EnviroMine.logger.log(Level.INFO, "Loaded " + EM_Settings.dimensionProperties.size() + " dimension properties");
	}
	
	@SideOnly(Side.CLIENT)
	public static void registerKeyBindings(FMLInitializationEvent event)
	{
		// Add remove Keybind
		KeyBinding[] key = {new KeyBinding("EnviroMine Add/Remove Custom Object", Keyboard.KEY_J)};
		boolean[] repeat = {false};
		KeyBindingRegistry.registerKeyBinding(new enviromine.handlers.keybinds.AddRemoveCustom(key, repeat));
		
		// Reload Custom Objects Files
		KeyBinding[] key1 = {new KeyBinding("EnviroMine Reload All Custom Object", Keyboard.KEY_K)};
		boolean[] repeat1 = {false};
		KeyBindingRegistry.registerKeyBinding(new enviromine.handlers.keybinds.ReloadCustomObjects(key1, repeat1));
	
	}
	
    @EventHandler
    public void serverStart(FMLServerStartingEvent event)
    {
    	MinecraftServer server = MinecraftServer.getServer();
    	ICommandManager command = server.getCommandManager();
    	ServerCommandManager manager = (ServerCommandManager) command;

    	manager.registerCommand(new CommandPhysics());
    	manager.registerCommand(new EnviroCommand());
    }
}
