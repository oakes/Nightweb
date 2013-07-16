## I2P Update Instructions

1. Delete existing I2P codebase
	- `rm -r common/apps`
	- `rm -r common/core`
	- `rm -r common/router`
2. Download the tarball and extract it
3. Copy the required code into the `common` directory
	- `cp -r i2p-X.X.X/apps/i2psnark/java/src/ common/java/apps/`
	- `rm -r common/java/apps/org/klomp/snark/web/`
	- `cp -r i2p-X.X.X/apps/ministreaming/java/src/net/i2p/client/ common/java/apps/net/i2p/.`
	- `cp -r i2p-X.X.X/apps/streaming/java/src/net/i2p/client/ common/java/apps/net/i2p/.`
	- `cp -r i2p-X.X.X/core/java/src/ common/java/core/`
	- `cp -r i2p-X.X.X/router/java/src/ common/java/router/`
4. Re-apply changes to I2PSnark
	- `git cherry-pick 8da4bdb0ae54cdb28a6b60e8c5d78745a7b66785 5ec82d7ff44c4b68c47ef023711348d4542160a3`
