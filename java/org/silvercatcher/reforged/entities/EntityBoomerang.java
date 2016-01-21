package org.silvercatcher.reforged.entities;

import java.util.List;

import org.omg.IOP.TaggedComponent;
import org.silvercatcher.reforged.items.weapons.ItemBoomerang;

import net.minecraft.entity.DataWatcher.WatchableObject;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.projectile.EntityThrowable;
import net.minecraft.init.Blocks;
import net.minecraft.item.Item.ToolMaterial;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.MathHelper;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

public class EntityBoomerang extends EntityThrowable {
	
	public EntityBoomerang(World worldIn) {
		
		super(worldIn);
	}
	
	
	public EntityBoomerang(World worldIn, EntityLivingBase getThrowerIn, ItemStack stack) {
		
		super(worldIn, getThrowerIn);
		setItemStack(stack);
		setThrowerName(getThrowerIn.getName());
		setCoords(getThrowerIn.posX, getThrowerIn.posY, getThrowerIn.posZ);
	}
	
	@Override
	protected void entityInit() {
		super.entityInit();
		
		List allwatched = dataWatcher.getAllWatched();
		
		if(!allwatched.contains(5)) {
		// id 5 = ItemStack of Boomerang, type 5 = ItemStack
		dataWatcher.addObjectByDataType(5, 5);
		}
		
		if(!allwatched.contains(6)) {
		// id 6 = Name of Thrower, type 4 = String
		dataWatcher.addObjectByDataType(6, 4);
		}
		
		if(!allwatched.contains(7)) {
		// id 7 = posX, type 3 = float
		dataWatcher.addObjectByDataType(7, 3);
		}
		
		if(!allwatched.contains(8)) {
		// id 8 = posY, type 3 = float
		dataWatcher.addObjectByDataType(8, 3);
		}
		
		if(!allwatched.contains(9)) {
		// id 9 = posZ, type 3 = float
		dataWatcher.addObjectByDataType(9, 3);
		}
	}

	public ItemStack getItemStack() {
		
		return dataWatcher.getWatchableObjectItemStack(5);
	}
	
	public void setItemStack(ItemStack stack) {
		
		if(stack == null || !(stack.getItem() instanceof ItemBoomerang)) {
			throw new IllegalArgumentException("Invalid Itemstack!");
		}
		dataWatcher.updateObject(5, stack);
	}
	
	public void setCoords(double playerX, double playerY, double playerZ) {
		dataWatcher.updateObject(7, (float) playerX);
		dataWatcher.updateObject(8, (float) playerY);
		dataWatcher.updateObject(9, (float) playerZ);
	}
	
	public double getCoord(int coordId) {
		switch(coordId) {
		//1 returns X, 2 returns Y, 3 returns Z
		case 1: return (double) dataWatcher.getWatchableObjectFloat(7);
		case 2: return (double) dataWatcher.getWatchableObjectFloat(8);
		case 3: return (double) dataWatcher.getWatchableObjectFloat(9);
		default: throw new IllegalArgumentException("Invalid coordId!");
		}
	}
	
	public EntityLivingBase getThrowerASave() {
		return getEntityWorld().getPlayerEntityByName(getThrowerName());
	}
	
	public String getThrowerName() {
		return dataWatcher.getWatchableObjectString(6);
	}
	
	public void setThrowerName(String name) {
		
		dataWatcher.updateObject(6, name);
	}
	
	private void printDatawatcher() {
		
		System.out.println("##########");
		for(Object o : dataWatcher.getAllWatched()) {
			WatchableObject wo = (WatchableObject) o;
			System.out.println(wo.getDataValueId() + ": (" + wo.getObjectType() + ") " + wo.getObject());
		}
		
		System.out.println("++++++++++++++++");
	}
	
	public ToolMaterial getMaterial() {

		return ((ItemBoomerang) getItemStack().getItem()).getMaterial();
	}

	private float getImpactDamage() {
		
		return getMaterial().getDamageVsEntity()  + 3;
	}
	
	private static final double returnStrength = 0.05D;
	private static final double returnStrengthY = 0.01D;
	
	@Override
	public void onUpdate() {
		if(!getEntityWorld().isRemote) {
			super.onUpdate();
			double dx = this.posX - getCoord(1);
			double dy = this.posY - getCoord(2);
			double dz = this.posZ - getCoord(3);
			
			double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
			double ds = Math.sqrt(dx * dx + dy + dz * dz);
			dx /= d;
			dy /= ds;
			dz /= d;
			
			motionX -= returnStrength * dx;
			motionY -= returnStrengthY * dy;
			motionZ -= returnStrength * dz;
		}
	}
	
	@Override
	protected float getGravityVelocity() {
		return 0.0F;
	}

	@Override
	protected void onImpact(MovingObjectPosition target) {
			
		//Target is entity or block?
		if(target.entityHit == null) {
			//It's a block
			if(!worldObj.isRemote) {
				entityDropItem(getItemStack(), 0.5f);
			}
			setDead();
		} else {
			//It's an entity
			if(target.entityHit != getThrowerASave()) {
				//It's an hit entity
				target.entityHit.attackEntityFrom(DamageSource.causeThrownDamage(
						target.entityHit, getThrowerASave()), getImpactDamage());
				ItemStack stack = getItemStack();
				if(stack.attemptDamageItem(1, rand)) {
					this.setDead();
				} else {
					setItemStack(stack);
				}
			} else {
				//It's the thrower himself
				this.setDead();
				ItemStack stack = getItemStack();
				entityDropItem(stack, 0.0F);
			}
		}
	}

	@Override
	public void writeEntityToNBT(NBTTagCompound tagCompound) {
		
		super.writeEntityToNBT(tagCompound);
		
		tagCompound.setString("thrower", getThrower().getName());
		tagCompound.setDouble("throwerX", getCoord(1));
		tagCompound.setDouble("throwerY", getCoord(2));
		tagCompound.setDouble("throwerZ", getCoord(3));
		
		if(getItemStack() != null) {
			tagCompound.setTag("item", getItemStack().writeToNBT(new NBTTagCompound()));
		}
	}
	
	@Override
	public void readEntityFromNBT(NBTTagCompound tagCompund) {
		
		super.readEntityFromNBT(tagCompund);
		setItemStack(ItemStack.loadItemStackFromNBT(tagCompund.getCompoundTag("item")));
		setCoords(tagCompund.getDouble("throwerX"), tagCompund.getDouble("throwerY"), tagCompund.getDouble("throwerZ"));
		setThrowerName(tagCompund.getString("thrower"));
	}
}
