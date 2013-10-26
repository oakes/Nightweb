## I2P Update Instructions

1. Delete the existing I2P codebase
	- `rm -r java/apps`
	- `rm -r java/core`
	- `rm -r java/router`
2. Download the source tarball and extract it
3. Copy the required code into the `common` directory
	- `cp -r i2p-X.X.X/apps/i2psnark/java/src/ java/apps/`
	- `rm -r java/apps/org/klomp/snark/web/`
	- `cp -r i2p-X.X.X/apps/ministreaming/java/src/net/i2p/client/ java/apps/net/i2p/.`
	- `cp -r i2p-X.X.X/apps/streaming/java/src/net/i2p/client/ java/apps/net/i2p/.`
	- `cp -r i2p-X.X.X/core/java/src/ java/core/`
	- `cp -r i2p-X.X.X/router/java/src/ java/router/`
4. Commit the changes
5. Re-apply changes to I2PSnark
	- `git cherry-pick 8da4bdb0ae54cdb28a6b60e8c5d78745a7b66785 5ec82d7ff44c4b68c47ef023711348d4542160a3`
