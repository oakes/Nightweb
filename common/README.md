## I2P Update Instructions

1. Delete the existing I2P codebase
	- `rm -r common/java/apps`
	- `rm -r common/java/core`
	- `rm -r common/java/router`
2. Download the source tarball and extract it
3. Copy the required code into the `common` directory
	- `cp -r i2p-X.X.X/apps/i2psnark/java/src/ common/java/apps/`
	- `rm -r common/java/apps/org/klomp/snark/web/`
	- `cp -r i2p-X.X.X/apps/ministreaming/java/src/net/i2p/client/ common/java/apps/net/i2p/.`
	- `cp -r i2p-X.X.X/apps/streaming/java/src/net/i2p/client/ common/java/apps/net/i2p/.`
	- `cp -r i2p-X.X.X/core/java/src/ common/java/core/`
	- `cp -r i2p-X.X.X/router/java/src/ common/java/router/`
4. Commit the changes
5. Re-apply changes to I2PSnark
	- `git cherry-pick fe71fedd508030803f92a16e131821d6d238261f`
