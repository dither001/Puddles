package puddles;

import java.util.Iterator;
import java.util.Random;

import org.apache.logging.log4j.Logger;

import net.minecraft.block.Block;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.init.PotionTypes;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemHoe;
import net.minecraft.item.ItemSpade;
import net.minecraft.item.ItemStack;
import net.minecraft.potion.PotionUtils;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraft.world.biome.Biome;
import net.minecraft.world.chunk.Chunk;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.common.DimensionManager;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;

@Mod(modid = Puddles.MODID, name = Puddles.NAME, version = Puddles.VERSION)
public class Puddles
{
    public static final String MODID = "puddles";
    public static final String NAME = "Puddles";
    public static final String VERSION = "1.0";

    public static Logger logger;
    
    public static Block puddle;

    @EventHandler
    public void preInit(FMLPreInitializationEvent event)
    {
        logger = event.getModLog();
        logger.info("splish spash you have puddles installed");
        puddle = new BlockPuddle().setUnlocalizedName("puddle").setRegistryName(new ResourceLocation(MODID, "puddle"));
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new PuddlesConfig.ConfigEventHandler());
    }
    
	@SubscribeEvent
	public void registerBlocks(RegistryEvent.Register<Block> event)
	{
		event.getRegistry().register(puddle);
	}
	
	@SubscribeEvent
	public void registerItems(RegistryEvent.Register<Item> event)
	{
		ItemBlock item = new ItemBlock(puddle);
		item.setRegistryName(new ResourceLocation(MODID, "puddle"));
		event.getRegistry().register(item);
	}
	
	@SubscribeEvent
	public void registerItemModels(ModelRegistryEvent event)
	{
		Item item = Item.getItemFromBlock(puddle);
		ModelLoader.setCustomModelResourceLocation(item, 0, new ModelResourceLocation(item.getRegistryName(), "inventory"));
	}
	
	@SubscribeEvent
	public void placePuddles(TickEvent.ServerTickEvent event)
	{
		if(event.phase == TickEvent.Phase.END)
		{
			WorldServer world = DimensionManager.getWorld(0);
			try
			{
				if(world.getTotalWorldTime() % 10 == 0)
				{
					Iterator<Chunk> iterator = world.getPlayerChunkMap().getChunkIterator();
					
					while(iterator.hasNext())
					{
						Random random = world.rand;
						ChunkPos chunkPos = iterator.next().getPos();
						
						int x = random.nextInt(8) - random.nextInt(8);
						int z = random.nextInt(8) - random.nextInt(8);
						BlockPos pos = chunkPos.getBlock(8 + x, 0, 8 + z);
						
						int y = world.getHeight(pos).getY() + random.nextInt(4) - random.nextInt(4);
						BlockPos puddlePos = pos.add(0, y, 0);
						
						if(this.canSpawnPuddle(world, puddlePos))
						{
							if(random.nextInt(100) < PuddlesConfig.puddleRate)
							{
								world.setBlockState(puddlePos.up(), puddle.getDefaultState(), 2);
							}
						}
					}
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public boolean canSpawnPuddle(World world, BlockPos pos)
	{
		if(!world.isSideSolid(pos, EnumFacing.UP))
			return false;
		if(!world.isAirBlock(pos.up()))
			return false;
		if(!world.isRaining())
			return false;
		
		Biome biome = world.getBiomeForCoordsBody(pos);
		if(biome.canRain() && !biome.getEnableSnow())
		{
			for(int y = pos.getY() + 1; y < world.getHeight(); y++)
			{
				BlockPos up = new BlockPos(pos.getX(), y, pos.getZ());
				if(!world.isAirBlock(up))
					return false;
			}
			return true;
		}
		
		return false;
	}
	
	@SubscribeEvent
	public void puddleInteract(PlayerInteractEvent.RightClickBlock event)
	{
		ItemStack stack = event.getItemStack();
		World world = event.getWorld();
		BlockPos pos = event.getPos().up();
		EntityPlayer player = event.getEntityPlayer();
		if(world.getBlockState(pos).getBlock() == Puddles.puddle)
		{
			if(stack.getItem() == Items.GLASS_BOTTLE)
			{
				if(event.getFace() == EnumFacing.UP)
				{
					if(!world.isRemote)
					{
						stack.shrink(1);
		                if (!player.inventory.addItemStackToInventory(PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.WATER)))
		                {
		                    player.dropItem(PotionUtils.addPotionToItemStack(new ItemStack(Items.POTIONITEM), PotionTypes.WATER), false);
		                }
						world.setBlockToAir(pos);
					}
					else
					{
						world.playSound(player, player.posX, player.posY, player.posZ, SoundEvents.ITEM_BOTTLE_FILL, SoundCategory.NEUTRAL, 1.0F, 1.0F);
					}
				}
			}
			if(stack.getItem() instanceof ItemHoe)
			{
				world.setBlockToAir(pos);
				ItemHoe hoe = (ItemHoe)stack.getItem();
				hoe.onItemUse(player, world, pos.down(), event.getHand(), event.getFace(), 0, 0, 0);
			}
			if(stack.getItem() instanceof ItemSpade)
			{
				world.setBlockToAir(pos);
				ItemSpade shovel = (ItemSpade)stack.getItem();
				shovel.onItemUse(player, world, pos.down(), event.getHand(), event.getFace(), 0, 0, 0);
			}
		}
	}
}
