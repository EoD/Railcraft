/* 
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 * 
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.common.util.inventory.wrappers;

import java.util.Iterator;
import net.minecraft.inventory.IInventory;
import net.minecraft.inventory.ISidedInventory;
import net.minecraft.item.ItemStack;

/**
 * @author CovertJaguar <http://www.railcraft.info>
 */
public class InventoryIterator implements Iterable<IInvSlot> {

    public static Iterable<IInvSlot> getIterable(IInventory inv) {
        if (inv instanceof ISidedInventory)
            return new SidedInventoryIterator((ISidedInventory) inv);
        return new InventoryIterator(inv);
    }

    private final IInventory inv;
    private final int invSize;

    private InventoryIterator(IInventory inv) {
        this.inv = inv;
        this.invSize = inv.getSizeInventory();
    }

    @Override
    public Iterator<IInvSlot> iterator() {
        return new Iterator<IInvSlot>() {
            int slot = 0;

            @Override
            public boolean hasNext() {
                return slot < invSize;
            }

            @Override
            public IInvSlot next() {
                return new InvSlot(slot++);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException("Remove not supported.");
            }

        };
    }

    private class InvSlot implements IInvSlot {

        private int slot;

        public InvSlot(int slot) {
            this.slot = slot;
        }

        @Override
        public ItemStack getStackInSlot() {
            return inv.getStackInSlot(slot);
        }

        @Override
        public void setStackInSlot(ItemStack stack) {
            inv.setInventorySlotContents(slot, stack);
            inv.markDirty();
        }

        @Override
        public boolean canPutStackInSlot(ItemStack stack) {
            return inv.isItemValidForSlot(slot, stack);
        }

        @Override
        public boolean canTakeStackFromSlot(ItemStack stack) {
            return true;
        }

        @Override
        public ItemStack decreaseStackInSlot() {
            ItemStack stack = inv.decrStackSize(slot, 1);
            inv.markDirty();
            return stack;
        }

        @Override
        public int getIndex() {
            return slot;
        }

    }
}
