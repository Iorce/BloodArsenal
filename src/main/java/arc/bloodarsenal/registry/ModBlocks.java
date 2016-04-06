package arc.bloodarsenal.registry;

import arc.bloodarsenal.BloodArsenal;
import arc.bloodarsenal.ConfigHandler;
import arc.bloodarsenal.block.*;
import arc.bloodarsenal.item.block.ItemBlockBloodInfusedWoodenSlab;
import net.minecraft.block.Block;
import net.minecraft.block.BlockSlab;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemSlab;
import net.minecraftforge.fml.common.registry.GameRegistry;

public class ModBlocks
{
    public static Block bloodInfusedWoodenLog;
    public static Block bloodInfusedWoodenPlanks;
    public static Block bloodInfusedWoodenStairs;
    public static Block bloodInfusedWoodenDoubleSlab;
    public static Block bloodInfusedWoodenSlab;
    public static Block bloodStainedGlass;

    public static void init()
    {
        bloodInfusedWoodenLog = registerBlock(new BlockBloodInfusedWoodenLog("bloodInfusedWoodenLog"));
        bloodInfusedWoodenPlanks = registerBlock(new BlockBloodInfusedWoodenPlanks("bloodInfusedWoodenPlanks"));
        bloodInfusedWoodenStairs = registerBlock(new BlockBloodInfusedWoodenStairs("bloodInfusedWoodenStairs", bloodInfusedWoodenPlanks));
        bloodInfusedWoodenDoubleSlab = registerBlock(new BlockBloodInfusedWoodenSlab.BlockBloodInfusedWoodenDoubleSlab().setUnlocalizedName(BloodArsenal.MOD_ID + ".bloodInfusedWoodenDoubleSlab"));
        bloodInfusedWoodenSlab = new BlockBloodInfusedWoodenSlab.BlockBloodInfusedWoodenHalfSlab().setUnlocalizedName(BloodArsenal.MOD_ID + ".bloodInfusedWoodenHalfSlab");
        bloodInfusedWoodenSlab = registerBlock(new ItemBlockBloodInfusedWoodenSlab("bloodInfusedWoodenSlab"));
        bloodStainedGlass = registerBlock(new BlockBloodStainedGlass("bloodStainedGlass"));

        initTiles();
    }

    public static void initTiles()
    {

    }

    public static void initSpecialRenders()
    {

    }

    public static Block registerBlock(ItemBlock itemBlock, String name)
    {
        Block block = itemBlock.block;

        if (!ConfigHandler.blockBlacklist.contains(name))
        {
            GameRegistry.register(block);
            GameRegistry.register(itemBlock.setRegistryName(block.getRegistryName()));
            BloodArsenal.PROXY.tryHandleBlockModel(block, name);
        }

        return block;
    }

    public static Block registerBlock(ItemBlock itemBlock)
    {
        Block block = itemBlock.block;
        block.setRegistryName(block.getClass().getSimpleName());
        if (block.getRegistryName() == null)
        {
            BloodArsenal.INSTANCE.getLogger().error("Attempted to register Block {} without setting a registry name. Block will not be registered. Please report this.", block.getClass().getCanonicalName());
            return block;
        }

        String blockName = block.getRegistryName().toString().split(":")[1];
        if (!ConfigHandler.blockBlacklist.contains(blockName))
        {
            GameRegistry.register(block);
            GameRegistry.register(itemBlock.setRegistryName(block.getRegistryName().getResourcePath()));
            BloodArsenal.PROXY.tryHandleBlockModel(block, blockName);
        }

        return block;
    }

    public static Block registerBlock(Block block)
    {
        block.setRegistryName(block.getClass().getSimpleName());
        if (block.getRegistryName() == null)
        {
            BloodArsenal.INSTANCE.getLogger().error("Attempted to register Block {} without setting a registry name. Block will not be registered. Please report this.", block.getClass().getCanonicalName());
            return null;
        }

        String blockName = block.getRegistryName().toString().split(":")[1];
        System.out.println("REGISTERY: " + blockName);
        if (!ConfigHandler.blockBlacklist.contains(blockName))
        {
            GameRegistry.register(block);
            GameRegistry.register(new ItemBlock(block).setRegistryName(block.getRegistryName().getResourcePath()));
            BloodArsenal.PROXY.tryHandleBlockModel(block, blockName);
        }

        return block;
    }
}
