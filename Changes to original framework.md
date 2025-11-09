- MarioForwardModel
	- getWorld method

- MarioLevel
	- getLevelTiles, getSpriteTemplates methods	
	- removed enemy respawning
	- removed lastSpawnTime, get/setLastSpawnTick
	- new setSpriteType method
	- clone - spriteTemplates is now a deep copy
		- because it now changes
	
- MarioWorld
	- spawning reworked, getSprites
	- new sprites are now added to the end of the sprite list, not the beginning (for speed and consistency)
	
- BulletBill
	- missing alive in clone

- Enemy
	- getPrivateEnemyCopyInfo, PrivateEnemyCopyInfo, alive in clone

- FireFlower
	- getLife, alive in clone

- Fireball
	- isOnGround, alive in clone

- FlowerEnemy
	- getyStart, getWaitTime, clone - alive, facing, x, y

- LifeMushroom
	- isOnGround, getLife, alive in clone

- Mario
	- getPrivateMarioCopyInfo, PrivateMarioCopyInfo, clone - alive, oldLarge, oldFire, x, y

- Mushroom
	- isOnGround, getLife, alive in clone

- Shell
	- isOnGround, alive in clone