/* 
 * Copyright (c) CovertJaguar, 2014 http://railcraft.info
 * 
 * This code is the property of CovertJaguar
 * and may only be used with explicit written
 * permission unless otherwise specified on the
 * license page at http://railcraft.info/wiki/info:license.
 */
package mods.railcraft.client.render.carts;

import net.minecraft.client.renderer.Tessellator;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.item.ItemStack;
import net.minecraft.util.IIcon;
import org.lwjgl.opengl.GL11;
import mods.railcraft.client.render.FluidRenderer;
import mods.railcraft.client.render.RenderFakeBlock;
import mods.railcraft.client.render.RenderFakeBlock.RenderInfo;
import mods.railcraft.common.carts.EntityCartTank;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.init.Blocks;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fluids.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.IFluidTank;

/**
 *
 * @author CovertJaguar <http://www.railcraft.info>
 */
public class CartContentRendererTank extends CartContentRenderer {

    private final RenderInfo fillBlock = new RenderInfo(0.4f, 0.0f, 0.4f, 0.6f, 0.999f, 0.6f);
    private final RenderInfo bucketSign = new RenderInfo();
    private static final float FILTER_SCALE_X = 1.38f;
    private static final float FILTER_SCALE_Y = 0.5f;
    private static final float FILTER_SCALE_Z = 0.5f;

    public CartContentRendererTank() {
        bucketSign.template = Blocks.glass;
        bucketSign.texture = new IIcon[1];
        bucketSign.renderSide[0] = false;
        bucketSign.renderSide[1] = false;
        bucketSign.renderSide[2] = false;
        bucketSign.renderSide[3] = false;
        fillBlock.texture = new IIcon[6];
    }

    @Override
    public void render(RenderCart renderer, EntityMinecart cart, float light, float time) {
        super.render(renderer, cart, light, time);
        EntityCartTank cartTank = (EntityCartTank) cart;
        GL11.glPushMatrix();
        GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
        GL11.glTranslatef(0.0F, 0.3125F, 0.0F);
        GL11.glRotatef(90F, 0.0F, 1.0F, 0.0F);
        GL11.glDisable(2896 /*GL_LIGHTING*/);

        int x = (int) (Math.floor(cart.posX));
        int y = (int) (Math.floor(cart.posY));
        int z = (int) (Math.floor(cart.posZ));

        IFluidTank tank = cartTank.getTankManager().get(0);
        FluidStack fluidStack = tank.getFluid();
        if (fluidStack != null && fluidStack.amount > 0) {
            Fluid fluid = fluidStack.getFluid();
            int[] displayLists = FluidRenderer.getLiquidDisplayLists(fluidStack);
            if (fluid != null && displayLists != null) {
                GL11.glPushMatrix();

                GL11.glPushAttrib(GL11.GL_ENABLE_BIT);
                GL11.glEnable(GL11.GL_BLEND);

                GL11.glTranslatef(0, 0.0625f, 0);

                float cap = tank.getCapacity();
                float level = (float) Math.min(fluidStack.amount, cap) / cap;

                renderer.bindTex(FluidRenderer.getFluidSheet(fluidStack));
                FluidRenderer.setColorForFluidStack(fluidStack);
                GL11.glCallList(displayLists[(int) (level * (float) (FluidRenderer.DISPLAY_STAGES - 1))]);

                if (cartTank.isFilling()) {
                    ResourceLocation texSheet = FluidRenderer.setupFlowingLiquidTexture(fluidStack, fillBlock.texture);
                    if (texSheet != null) {
                        renderer.bindTex(texSheet);
                        RenderFakeBlock.renderBlockForEntity(fillBlock, cart.worldObj, x, y, z, false, true);
                    }
                }

                GL11.glPopAttrib();
                GL11.glPopMatrix();
            }
        }

        ItemStack bucket = cartTank.getFilterItem();

        if (bucket != null && bucket.getItem() != null) {

            GL11.glPushMatrix();
            GL11.glScalef(FILTER_SCALE_X, FILTER_SCALE_Y, FILTER_SCALE_Z);
            GL11.glTranslatef(0, -0.4f, 0);

            renderer.bindTex(TextureMap.locationItemsTexture);

            int meta = bucket.getItemDamage();
            for (int pass = 0; pass < bucket.getItem().getRenderPasses(meta); ++pass) {
                IIcon texture = bucket.getItem().getIconFromDamageForRenderPass(meta, pass);
                if(texture == null)
                    continue;
                
                int color = bucket.getItem().getColorFromItemStack(bucket, pass);

                float c1 = (float) (color >> 16 & 255) / 255.0F;
                float c2 = (float) (color >> 8 & 255) / 255.0F;
                float c3 = (float) (color & 255) / 255.0F;

                float dim = 0.7f;

                GL11.glColor4f(c1 * light * dim, c2 * light * dim, c3 * light * dim, 1.0F);

                Tessellator tess = Tessellator.instance;
                tess.setBrightness(bucketSign.template.getMixedBrightnessForBlock(cart.worldObj, x, y, z));

                bucketSign.texture[0] = texture;
                RenderFakeBlock.renderBlockForEntity(bucketSign, cart.worldObj, x, y, z, false, true);
            }

            GL11.glPopMatrix();
        }

        GL11.glPopAttrib();
        GL11.glPopMatrix();
    }
}
