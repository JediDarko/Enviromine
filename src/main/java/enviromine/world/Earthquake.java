package enviromine.world;

import java.util.ArrayList;
import enviromine.core.EM_Settings;
import enviromine.handlers.EM_PhysManager;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;

public class Earthquake
{
	public static ArrayList<Earthquake> pendingQuakes = new ArrayList<Earthquake>();
	public static int tickCount = 0;
	
	World world;
	int posX;
	int posZ;
	
	int length;
	int width;
	
	int passY = 1;
	
	ArrayList<int[]> ravineMask = new ArrayList<int[]>(); // 2D array containing x,z coordinates of blocks within the ravine
	
	public Earthquake(World world, int i, int k, int l, int w)
	{
		this.world = world;
		this.posX = i;
		this.posZ = k;
		this.length = l;
		this.width = w;
		
		this.markRavine();
		pendingQuakes.add(this);
	}
	
	public void markRavine()
	{
		float angle = world.rand.nextFloat() * 4 - 2;
		
		for(int i = -length / 2; i < length / 2; i++)
		{
			int fx = MathHelper.floor_float(Math.abs(angle) > 1F ? i * (angle > 0 ? angle - 1F : angle + 1F) : i);
			int fz = MathHelper.floor_float(Math.abs(angle) > 1F ? i : i * angle);
			int widthFactor = MathHelper.ceiling_double_int(Math.cos(i / (length / 3D)) * width);
			
			if(Math.abs(angle) <= 1F)
			{
				for(int z = fz - widthFactor / 2; z < fz + widthFactor / 2; z++)
				{
					this.ravineMask.add(new int[]{fx + posX, 1, z + posZ});
				}
			} else
			{
				for(int x = fx - widthFactor / 2; x < fx + widthFactor / 2; x++)
				{
					this.ravineMask.add(new int[]{x + posX, 1, fz + posZ});
				}
			}
		}
	}
	
	public boolean removeBlock()
	{
		while(passY < world.getActualHeight())
		{
			for(int i = 0; i < ravineMask.size(); i++)
			{
				int[] pos = this.ravineMask.get(i);
				
				int x = pos[0];
				int y = pos[1];
				int z = pos[2];
				
				if(y > passY)
				{
					continue;
				}
				
				for(int yy = y; yy >= 1; yy--)
				{
					if((world.getBlockMaterial(x, yy, z) == Material.lava && yy >= 8) || world.getBlockMaterial(x, yy, z) == Material.water || world.getBlockMaterial(x, yy, z) == Material.rock || world.getBlockMaterial(x, yy, z) == Material.clay || world.getBlockMaterial(x, yy, z) == Material.sand || world.getBlockMaterial(x, yy, z) == Material.ground || world.getBlockMaterial(x, yy, z) == Material.grass || (yy < 8 && world.getBlockMaterial(x, yy, z) == Material.air))
					{
						world.setBlock(x, y, z, Block.lavaMoving.blockID);
						
						if(EM_Settings.enablePhysics)
						{
							EM_PhysManager.schedulePhysUpdate(world, x, y, z, false, "Quake");
						}
						return true;
					} else
					{
						world.setBlockToAir(x, y, z);
						
						if(yy < 8)
						{
							world.setBlock(x, yy, z, Block.lavaMoving.blockID);
							//System.out.println("Placed lava at (" + x + "," + yy + "," + z + ")");
							
							if(EM_Settings.enablePhysics)
							{
								EM_PhysManager.schedulePhysUpdate(world, x, yy, z, false, "Quake");
							}
							
							ravineMask.set(i, new int[]{x, y + 1, z});
							if(yy == y)
							{
								return true;
							}
						} else
						{
							world.setBlockToAir(x, yy, z);
							//System.out.println("Placed air at (" + x + "," + yy + "," + z + ")");
							
							if(EM_Settings.enablePhysics)
							{
								EM_PhysManager.schedulePhysUpdate(world, x, yy, z, false, "Quake");
							}
							
							ravineMask.set(i, new int[]{x, y + 1, z});
							if(yy == y)
							{
								return true;
							}
						}
					}
				}
				
				if(world.getTopSolidOrLiquidBlock(x, z) < 16)
				{
					ravineMask.remove(i);
				} else
				{
					ravineMask.set(i, new int[]{x, y + 1, z});
				}
			}
			
			passY += 1;
		}
		
		return false;
	}
	
	public void removeAll()
	{
		for(int y = 1; y < world.getActualHeight(); y++)
		{
			for(int i = 0; i < this.ravineMask.size(); i++)
			{
				int[] pos = this.ravineMask.get(i);
				
				int x = pos[0];
				int z = pos[2];
				
				if((world.getBlockMaterial(x, y, z) == Material.lava && y >= 8) || world.getBlockMaterial(x, y, z) == Material.water || world.getBlockMaterial(x, y, z) == Material.rock || world.getBlockMaterial(x, y, z) == Material.clay || world.getBlockMaterial(x, y, z) == Material.sand || world.getBlockMaterial(x, y, z) == Material.ground || world.getBlockMaterial(x, y, z) == Material.grass || (y < 8 && world.getBlockMaterial(x, y, z) == Material.air))
				{
					if(y < 8)
					{
						world.setBlock(x, y, z, Block.lavaMoving.blockID);
						
						if(EM_Settings.enablePhysics)
						{
							EM_PhysManager.schedulePhysUpdate(world, x, y, z, false, "Quake");
						}
					} else
					{
						world.setBlockToAir(x, y, z);
						
						if(EM_Settings.enablePhysics)
						{
							EM_PhysManager.schedulePhysUpdate(world, x, y, z, false, "Quake");
						}
					}
				}
			}
		}
		
		this.ravineMask.clear();
	}
	
	public static void updateEarthquakes()
	{
		if(tickCount >= 5)
		{
			tickCount = 0;
		} else
		{
			tickCount++;
			return;
		}
		
		for(int i = pendingQuakes.size() - 1; i >= 0; i--)
		{
			Earthquake quake = pendingQuakes.get(i);
			
			if(quake.world.isRemote)
			{
				pendingQuakes.remove(i);
				continue;
			}
			
			//quake.removeAll();
			if(!quake.removeBlock() || quake.ravineMask.size() <= 0)
			{
				pendingQuakes.remove(i);
			}
		}
	}
}