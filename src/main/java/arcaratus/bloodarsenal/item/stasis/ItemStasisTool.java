package arcaratus.bloodarsenal.item.stasis;

import WayofTime.bloodmagic.client.IMeshProvider;
import WayofTime.bloodmagic.event.BoundToolEvent;
import WayofTime.bloodmagic.iface.IActivatable;
import WayofTime.bloodmagic.iface.IBindable;
import WayofTime.bloodmagic.util.Utils;
import WayofTime.bloodmagic.util.helper.NetworkHelper;
import WayofTime.bloodmagic.util.helper.TextHelper;
import arcaratus.bloodarsenal.BloodArsenal;
import arcaratus.bloodarsenal.core.RegistrarBloodArsenalItems;
import arcaratus.bloodarsenal.modifier.*;
import arcaratus.bloodarsenal.util.BloodArsenalUtils;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.entity.*;
import net.minecraft.entity.ai.attributes.AttributeModifier;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.*;
import net.minecraft.util.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.lwjgl.input.Keyboard;

import javax.annotation.Nullable;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Consumer;

public abstract class ItemStasisTool extends ItemTool implements IBindable, IActivatable, IModifiableItem, IMeshProvider//, IProfilable
{
    protected final String tooltipBase;

    public Map<ItemStack, Boolean> heldDownMap = new HashMap<>();
    public Map<ItemStack, Integer> heldDownCountMap = new HashMap<>();

    public final int CHARGE_TIME = 30;
    public int cost = 5;

    private String name;

    public ItemStasisTool(String name, float damage)
    {
        super(damage, -3.2F, RegistrarBloodArsenalItems.STASIS, BloodArsenalUtils.getEffectiveBlocksForTool(name));

        setUnlocalizedName(BloodArsenal.MOD_ID + ".stasis." + name);
        setRegistryName("stasis_" + name);
        setCreativeTab(BloodArsenal.TAB_BLOOD_ARSENAL);
        setHarvestLevel(name, 4);

        this.name = name;
        tooltipBase = "tooltip.bloodarsenal.stasis." + name + ".";
    }

    @Override
    public void onUpdate(ItemStack itemStack, World world, Entity entity, int itemSlot, boolean isSelected)
    {
        if (getBinding(itemStack) == null)
        {
            setActivatedState(itemStack, false);
            return;
        }

        if (entity instanceof EntityPlayer && getActivated(itemStack) && isSelected && getBeingHeldDown(itemStack) && itemStack == ((EntityPlayer) entity).getActiveItemStack())
        {
            EntityPlayer player = (EntityPlayer) entity;
            setHeldDownCount(itemStack, Math.min(player.getItemInUseCount(), CHARGE_TIME));
        }
        else if (!isSelected)
        {
            setBeingHeldDown(itemStack, false);
        }

        if (entity instanceof EntityPlayer)
        {
            NewModifiable modifiable = NewModifiable.getModifiableFromStack(itemStack);
            if (getActivated(itemStack))
            {
                modifiable.onUpdate(itemStack, world, entity, itemSlot);

                if (world.getTotalWorldTime() % 80 == 0)
                    NetworkHelper.getSoulNetwork(getBinding(itemStack).getOwnerId()).syphonAndDamage((EntityPlayer) entity, cost);
            }
            else
            {
//                StasisModifiable modifiable = StasisModifiable.getModFromNBT(itemStack);
//                if (modifiable != null && modifiable.hasModifier(ModifierShadowTool.class))
//                    modifiable.onSpecialUpdate(itemStack, world, entity);
            }
        }
    }

    @Override
    public boolean hitEntity(ItemStack itemStack, EntityLivingBase target, EntityLivingBase attacker)
    {
        NewModifiable modifiable = NewModifiable.getModifiableFromStack(itemStack);
        if (getActivated(itemStack))
        {
            modifiable.hitEntity(itemStack, target, attacker);
        }
        else
        {
//            StasisModifiable modifiable = StasisModifiable.getModFromNBT(itemStack);
//            if (modifiable != null && modifiable.hasModifier(ModifierShadowTool.class))
//                ModifierTracker.getTracker(ModifierShadowTool.class).incrementCounter(modifiable, 1);
        }

        return true;
    }

    @Override
    public boolean onBlockDestroyed(ItemStack itemStack, World world, IBlockState state, BlockPos pos, EntityLivingBase entityLivingBase)
    {
        if (!world.isRemote)
        {
            NewModifiable modifiable = NewModifiable.getModifiableFromStack(itemStack);
            if (getActivated(itemStack))
            {
                if (entityLivingBase instanceof EntityPlayer)
                {
                    EntityPlayer player = (EntityPlayer) entityLivingBase;
                    modifiable.onBlockDestroyed(itemStack, world, state, pos, player);
                    NewModifiable.setModifiable(itemStack, modifiable, false);
                }
            }
            else
            {
//                StasisModifiable modifiable = StasisModifiable.getModFromNBT(itemStack);
//                if (modifiable != null && modifiable.hasModifier(ModifierShadowTool.class))
//                    ModifierTracker.getTracker(ModifierShadowTool.class).incrementCounter(modifiable, 1);
            }
        }

        return true;
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(World world, EntityPlayer player, EnumHand hand)
    {
        ItemStack itemStack = player.getHeldItem(hand);
        if (player.isSneaking())
            setActivatedState(itemStack, !getActivated(itemStack));

        if (!player.isSneaking() && getActivated(itemStack))
        {
            NewModifiable modifiable = NewModifiable.getModifiableFromStack(itemStack);
            Modifier modifier = Modifier.EMPTY_MODIFIER;
            for (Entry<String, Pair<Modifier, ModifierTracker>> entry : modifiable.getModifierMap().entrySet())
            {
                if (entry.getValue().getLeft().getType() == EnumModifierType.ABILITY)
                {
                    modifier = entry.getValue().getLeft();
                    break;
                }
            }

            if (modifier != Modifier.EMPTY_MODIFIER)
            {
                if (modifier.getAction() == EnumAction.BOW)
                {
                    BoundToolEvent.Charge event = new BoundToolEvent.Charge(player, itemStack);
                    if (MinecraftForge.EVENT_BUS.post(event))
                        return ActionResult.newResult(EnumActionResult.FAIL, event.result);

                    player.setActiveHand(hand);
                    return ActionResult.newResult(EnumActionResult.SUCCESS, itemStack);
                }
                else if (!world.isRemote)
                {
                    modifiable.onRightClick(itemStack, world, player);
                }
            }
        }

        return super.onItemRightClick(world, player, hand);
    }

    @Override
    public void onPlayerStoppedUsing(ItemStack itemStack, World world, EntityLivingBase entityLiving, int timeLeft)
    {
        if (entityLiving instanceof EntityPlayer)
        {
            EntityPlayer player = (EntityPlayer) entityLiving;
            if (!player.isSneaking() && getActivated(itemStack))
            {
                int i = getMaxItemUseDuration(itemStack) - timeLeft;
                BoundToolEvent.Release event = new BoundToolEvent.Release(player, itemStack, i);
                if (MinecraftForge.EVENT_BUS.post(event))
                    return;

                i = event.charge;

                NewModifiable modifiable = NewModifiable.getModifiableFromStack(itemStack);
                modifiable.onRelease(itemStack, world, player, i);

                setBeingHeldDown(itemStack, false);
            }
        }
    }

    @Override
    public float getDestroySpeed(ItemStack itemStack, IBlockState state)
    {
        NewModifiable modifiable = NewModifiable.getModifiableFromStack(itemStack);
        return 1;//getActivated(itemStack) ? efficiency : (modifiable.hasModifier(ModifierShadowTool.class) ? efficiency * (((float) modifiable.getModifierAndTracker(ModifierShadowTool.class).getLevel() + 2) / 6) : 1);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public boolean hasEffect(ItemStack itemStack)
    {
        return false;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged)
    {
        return !ItemStack.areItemsEqual(oldStack, newStack) || slotChanged;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void getSubItems(CreativeTabs creativeTab, NonNullList<ItemStack> list)
    {
        list.add(Utils.setUnbreakable(new ItemStack(this)));
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(ItemStack itemStack, World world, List<String> tooltip, ITooltipFlag flag)
    {
        if (!itemStack.hasTagCompound())
            return;

        if (Keyboard.isKeyDown(Keyboard.KEY_LSHIFT))
        {
            NewModifiable modifiable = NewModifiable.getModifiableFromStack(itemStack);
            for (EnumModifierType type : EnumModifierType.values())
            {
                tooltip.add(TextHelper.localize("tooltip.bloodarsenal.modifierType." + WordUtils.swapCase(type.toString())));
                for (Pair<Modifier, ModifierTracker> entry : modifiable.getModifierMap().values())
                {
                    Modifier modifier = entry.getLeft();
                    ModifierTracker tracker = entry.getRight();
                    if (modifier != Modifier.EMPTY_MODIFIER && modifier.getType() == type)
                    {
                        String name = modifier.hasAltName() ? TextHelper.localize(modifier.getAlternateName(itemStack)) : TextHelper.localize(modifier.getUnlocalizedName());
                        tooltip.add(" -" + TextHelper.localize("tooltip.bloodarsenal.modifier.level", name, tracker.getLevel() + 1, (tracker.isReadyToUpgrade() ? "+" : "")));
                    }
                }
            }
        }
        else
        {
            tooltip.add(TextHelper.localizeEffect("tooltip.bloodarsenal.holdShift"));
        }

        super.addInformation(itemStack, world, tooltip, flag);
    }

    @Override
    public ItemStack onItemUseFinish(ItemStack itemStack, World world, EntityLivingBase entityLiving)
    {
        return itemStack;
    }

    @Override
    public int getMaxItemUseDuration(ItemStack itemStack)
    {
        return 72000;
    }

    @Override
    public EnumAction getItemUseAction(ItemStack itemStack)
    {
        NewModifiable modifiable = NewModifiable.getModifiableFromStack(itemStack);
        Modifier modifier = Modifier.EMPTY_MODIFIER;
        for (Pair<Modifier, ModifierTracker> entry : modifiable.getModifierMap().values())
        {
            if (entry.getLeft().getType() == EnumModifierType.ABILITY)
            {
                modifier = entry.getLeft();
                break;
            }
        }

        return modifier != Modifier.EMPTY_MODIFIER ? modifier.getAction() : EnumAction.NONE;
    }

    @Override
    public Set<String> getToolClasses(ItemStack itemStack)
    {
        return ImmutableSet.of(name);
    }

    @Override
    public boolean showDurabilityBar(ItemStack itemStack)
    {
        return getActivated(itemStack) && getBeingHeldDown(itemStack);
    }

    @Override
    public double getDurabilityForDisplay(ItemStack itemStack)
    {
        return ((double) -Math.min(getHeldDownCount(itemStack), CHARGE_TIME) / CHARGE_TIME) + 1;
    }

    @Override
    public EnumRarity getRarity(ItemStack itemStack)
    {
        return EnumRarity.RARE;
    }

    @Override
    public Multimap<String, AttributeModifier> getAttributeModifiers(EntityEquipmentSlot equipmentSlot, ItemStack itemStack)
    {
        if (equipmentSlot == EntityEquipmentSlot.MAINHAND)
        {
            if (getActivated(itemStack))
            {
                return NewModifiable.getModifiableFromStack(itemStack).getAttributeModifiers();
            }
            else
            {
                NewModifiable modifiable = NewModifiable.getModifiableFromStack(itemStack);
                Multimap<String, AttributeModifier> map = modifiable.getAttributeModifiers();
//                boolean hasShadow = modifiable.hasModifier(ModifierShadowTool.class);
//
//                if (hasShadow)
//                {
//                    Modifier modifier = modifiable.getModifierAndTracker(ModifierShadowTool.class);
//                    map.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", 2.7 * (modifier.getLevel() + 1) / 5, 0));
//                    map.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, "Tool modifier", -2.5 * (modifier.getLevel() + 1) / 5, 0));
//                }
//                else
                {
                    map.put(SharedMonsterAttributes.ATTACK_DAMAGE.getName(), new AttributeModifier(ATTACK_DAMAGE_MODIFIER, "Weapon modifier", 0, 0));
                    map.put(SharedMonsterAttributes.ATTACK_SPEED.getName(), new AttributeModifier(ATTACK_SPEED_MODIFIER, "Tool modifier", -2.8, 0));
                }

                return map;
            }
        }

        return super.getAttributeModifiers(equipmentSlot, itemStack);
    }

    @Override
    public IModifiable getModifiable(ItemStack itemStack)
    {
        return NewModifiable.getModifiableFromStack(itemStack);
    }

    @Nullable
    @Override
    public ResourceLocation getCustomLocation()
    {
        return null;
    }

    @Override
    public void gatherVariants(Consumer<String> variants)
    {
        variants.accept("active=true");
        variants.accept("active=false");
    }

    protected int getHeldDownCount(ItemStack itemStack)
    {
        if (!heldDownCountMap.containsKey(itemStack))
            return 0;

        return heldDownCountMap.get(itemStack);
    }

    protected void setHeldDownCount(ItemStack itemStack, int count)
    {
        heldDownCountMap.put(itemStack, count);
    }

    protected boolean getBeingHeldDown(ItemStack itemStack)
    {
        if (!heldDownMap.containsKey(itemStack))
            return false;

        return heldDownMap.get(itemStack);
    }

    protected void setBeingHeldDown(ItemStack itemStack, boolean heldDown)
    {
        heldDownMap.put(itemStack, heldDown);
    }
}
