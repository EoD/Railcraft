/* 
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 * 
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.fluids;

import com.google.common.collect.ForwardingList;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.ICrafting;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.nbt.NBTTagList;
import net.minecraftforge.common.util.ForgeDirection;
import mods.railcraft.common.fluids.tanks.StandardTank;
import mods.railcraft.common.plugins.forge.NBTPlugin;
import mods.railcraft.common.plugins.forge.NBTPlugin.NBTList;
import mods.railcraft.common.util.misc.AdjacentTileCache;
import mods.railcraft.common.util.misc.ITileFilter;
import mods.railcraft.common.util.network.PacketBuilder;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.tileentity.TileEntity;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidTankInfo;
import net.minecraftforge.fluids.IFluidHandler;
import net.minecraftforge.fluids.IFluidTank;

/**
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public class TankManager extends ForwardingList<StandardTank> implements IFluidHandler, List<StandardTank> {

    public static final ITileFilter TANK_FILTER = new ITileFilter() {
        @Override
        public boolean matches(TileEntity tile) {
            return tile instanceof IFluidHandler;
        }

    };
    private static final byte NETWORK_DATA = 3;
    private final List<StandardTank> tanks = new ArrayList<StandardTank>();
    private final List<FluidStack> prevFluidStacks = new ArrayList<FluidStack>();
    private final List<Integer> prevColor = new ArrayList<Integer>();

    public TankManager() {
    }

    public TankManager(StandardTank... tanks) {
        addAll(Arrays.asList(tanks));
    }

    @Override
    protected List<StandardTank> delegate() {
        return tanks;
    }

    @Override
    public boolean add(StandardTank tank) {
        boolean added = tanks.add(tank);
        int index = tanks.indexOf(tank);
        tank.setTankIndex(index);
        prevFluidStacks.add(tank.getFluid() == null ? null : tank.getFluid().copy());
        prevColor.add(tank.getColor());
        return added;
    }

    public void writeTanksToNBT(NBTTagCompound data) {
        NBTTagList tagList = new NBTTagList();
        for (byte slot = 0; slot < tanks.size(); slot++) {
            StandardTank tank = tanks.get(slot);
            if (tank.getFluid() != null) {
                NBTTagCompound tag = new NBTTagCompound();
                tag.setByte("tank", slot);
                tank.writeToNBT(tag);
                tagList.appendTag(tag);
            }
        }
        data.setTag("tanks", tagList);
    }

    public void readTanksFromNBT(NBTTagCompound data) {
        NBTList<NBTTagCompound> tagList = NBTPlugin.getNBTList(data, "tanks", NBTPlugin.EnumNBTType.COMPOUND);
        for (NBTTagCompound tag : tagList) {
            int slot = tag.getByte("tank");
            if (slot >= 0 && slot < tanks.size())
                tanks.get(slot).readFromNBT(tag);
        }
    }

    public void writePacketData(DataOutputStream data) throws IOException {
        for (int i = 0; i < tanks.size(); i++) {
            writePacketData(data, i);
        }
    }

    public void writePacketData(DataOutputStream data, int tankIndex) throws IOException {
        if (tankIndex >= tanks.size())
            return;
        StandardTank tank = tanks.get(tankIndex);
        FluidStack fluidStack = tank.getFluid();
        if (fluidStack != null) {
            data.writeShort(fluidStack.fluidID);
            data.writeInt(fluidStack.amount);
            data.writeInt(fluidStack.getFluid().getColor(fluidStack));
        } else
            data.writeShort(-1);
    }

    public void readPacketData(DataInputStream data) throws IOException {
        for (int i = 0; i < tanks.size(); i++) {
            readPacketData(data, i);
        }
    }

    public void readPacketData(DataInputStream data, int tankIndex) throws IOException {
        if (tankIndex >= tanks.size())
            return;
        StandardTank tank = tanks.get(tankIndex);
        int fluidId = data.readShort();
        if (fluidId != -1) {
            tank.setFluid(new FluidStack(fluidId, data.readInt()));
            tank.colorCache = data.readInt();
        } else
            tank.setFluid(null);
    }

    public void initGuiData(Container container, ICrafting player, int tankIndex) {
        if (tankIndex >= tanks.size())
            return;
        FluidStack fluidStack = tanks.get(tankIndex).getFluid();
        int fluidId = -1;
        int fluidAmount = 0;
        if (fluidStack != null && fluidStack.amount > 0) {
            fluidId = fluidStack.getFluid().getID();
            fluidAmount = fluidStack.amount;
        }

        player.sendProgressBarUpdate(container, tankIndex * NETWORK_DATA + 0, fluidId);
        PacketBuilder.instance().sendGuiIntegerPacket((EntityPlayerMP) player, container.windowId, tankIndex * NETWORK_DATA + 1, fluidAmount);
    }

    public void updateGuiData(Container container, List crafters, int tankIndex) {
        StandardTank tank = tanks.get(tankIndex);
        FluidStack fluidStack = tank.getFluid();
        FluidStack prev = prevFluidStacks.get(tankIndex);
        int color = tank.getColor();
        int pColor = prevColor.get(tankIndex);

        for (Object crafter1 : crafters) {
            ICrafting crafter = (ICrafting) crafter1;
            EntityPlayerMP player = (EntityPlayerMP) crafter1;
            if (fluidStack == null ^ prev == null) {
                int fluidId = -1;
                int fluidAmount = 0;
                if (fluidStack != null) {
                    fluidId = fluidStack.fluidID;
                    fluidAmount = fluidStack.amount;
                }
                crafter.sendProgressBarUpdate(container, tankIndex * NETWORK_DATA + 0, fluidId);
                PacketBuilder.instance().sendGuiIntegerPacket(player, container.windowId, tankIndex * NETWORK_DATA + 1, fluidAmount);
            } else if (fluidStack != null && prev != null) {
                if (fluidStack.getFluid() != prev.getFluid())
                    crafter.sendProgressBarUpdate(container, tankIndex * NETWORK_DATA + 0, fluidStack.fluidID);
                if (fluidStack.amount != prev.amount)
                    PacketBuilder.instance().sendGuiIntegerPacket(player, container.windowId, tankIndex * NETWORK_DATA + 1, fluidStack.amount);
                if (color != pColor)
                    PacketBuilder.instance().sendGuiIntegerPacket(player, container.windowId, tankIndex * NETWORK_DATA + 2, color);
            }
        }

        prevFluidStacks.set(tankIndex, tank.getFluid() == null ? null : tank.getFluid().copy());
        prevColor.set(tankIndex, color);
    }

    public void processGuiUpdate(int messageId, int data) {
        int tankIndex = messageId / NETWORK_DATA;

        if (tankIndex >= tanks.size())
            return;
        StandardTank tank = tanks.get(tankIndex);
        FluidStack fluidStack = tank.getFluid();
        if (fluidStack == null) {
            fluidStack = new FluidStack(-1, 0);
            tank.setFluid(fluidStack);
        }
        int fluidId = fluidStack.fluidID;
        int amount = fluidStack.amount;
        int color = tank.colorCache;
        boolean newLiquid = false;
        switch (messageId % NETWORK_DATA) {
            case 0:
                fluidId = data;
                newLiquid = true;
                break;
            case 1:
                amount = data;
                break;
            case 2:
                color = data;
                break;
        }
        if (newLiquid) {
            fluidStack = new FluidStack(fluidId, 0);
            tank.setFluid(fluidStack);
        }
        fluidStack.amount = amount;
        tank.colorCache = color;
    }

    @Override
    public int fill(ForgeDirection from, FluidStack resource, boolean doFill) {
        return fill(0, resource, doFill);
    }

    public int fill(int tankIndex, FluidStack resource, boolean doFill) {
        if (tankIndex < 0 || tankIndex >= tanks.size() || resource == null)
            return 0;

        return tanks.get(tankIndex).fill(resource, doFill);
    }

    @Override
    public FluidStack drain(ForgeDirection from, int maxDrain, boolean doDrain) {
        return drain(0, maxDrain, doDrain);
    }

    public FluidStack drain(int tankIndex, int maxDrain, boolean doDrain) {
        if (tankIndex < 0 || tankIndex >= tanks.size())
            return null;

        return tanks.get(tankIndex).drain(maxDrain, doDrain);
    }

    @Override
    public FluidStack drain(ForgeDirection from, FluidStack resource, boolean doDrain) {
        for (StandardTank tank : tanks) {
            if (tankCanDrainFluid(tank, resource))
                return tank.drain(resource.amount, doDrain);
        }
        return null;
    }

    @Override
    public boolean canFill(ForgeDirection from, Fluid fluid) {
        return true;
    }

    @Override
    public boolean canDrain(ForgeDirection from, Fluid fluid) {
        return true;
    }

    @Override
    public FluidTankInfo[] getTankInfo(ForgeDirection direction) {
        FluidTankInfo[] info = new FluidTankInfo[size()];
        for (int i = 0; i < size(); i++) {
            info[i] = get(i).getInfo();
        }
        return info;
    }

    public FluidTankInfo[] getTankInfo() {
        return getTankInfo(ForgeDirection.UNKNOWN);
    }

    @Override
    public StandardTank get(int tankIndex) {
        if (tankIndex < 0 || tankIndex >= tanks.size())
            return null;
        return tanks.get(tankIndex);
    }

    public void setCapacity(int tankIndex, int capacity) {
        StandardTank tank = get(tankIndex);
        tank.setCapacity(capacity);
        FluidStack fluidStack = tank.getFluid();
        if (fluidStack != null && fluidStack.amount > capacity)
            fluidStack.amount = capacity;
    }

    public void outputLiquid(AdjacentTileCache cache, ITileFilter filter, ForgeDirection[] sides, int tankIndex, int amount) {
        for (ForgeDirection side : sides) {
            TileEntity tile = cache.getTileOnSide(side);
            if (!filter.matches(tile)) continue;
            IFluidHandler tank = getTankFromTile(tile);
            if (tank == null) continue;
            outputLiquid((IFluidHandler) tile, side, tankIndex, amount);
        }
    }

    public void outputLiquid(IFluidHandler[] outputs, int tankIndex, int amount) {
        for (int side = 0; side < 6; side++) {
            IFluidHandler nearbyTank = outputs[side];
            if (nearbyTank != null)
                outputLiquid(nearbyTank, ForgeDirection.getOrientation(side), tankIndex, amount);
        }
    }

    public static IFluidHandler getTankFromTile(TileEntity tile) {
        IFluidHandler tank = null;
        if (tile instanceof IFluidHandler)
            tank = (IFluidHandler) tile;
        return tank;
    }

    private void outputLiquid(IFluidHandler tank, ForgeDirection side, int tankIndex, int amount) {
        FluidStack available = drain(tankIndex, amount, false);
        if (available != null) {
            int used = tank.fill(side.getOpposite(), available, true);
            if (used > 0)
                drain(tankIndex, used, true);
        }
    }

    private boolean tankAcceptsFluid(IFluidTank tank, FluidStack fluidStack) {
        if (fluidStack == null)
            return false;
        if (tank.fill(fluidStack, false) > 0)
            return true;
        return false;
    }

    private boolean tankCanDrainFluid(IFluidTank tank, FluidStack fluidStack) {
        if (fluidStack == null)
            return false;
        FluidStack drained = tank.drain(1, false);
        if (drained != null && drained.amount > 0)
            return true;
        return false;
    }

}
