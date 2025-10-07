# Comment for Pull Request #2

## Important: This is a Fabric Mod, Not a Forge Mod

This mod uses the **Fabric modding framework**, not Forge. The update approach for Minecraft 1.21.4 needs to be adjusted accordingly.

### Current Framework (Fabric):
The codebase clearly shows this is a Fabric mod:
- Uses `fabric-loom` build plugin
- Implements `ModInitializer` and `ClientModInitializer` interfaces
- Has `fabric.mod.json` configuration file
- Depends on `fabric-loader` and `fabric-api`
- Uses Yarn mappings (not MCP mappings)
- Uses Fabric-specific APIs like `SimpleSynchronousResourceReloadListener`

### What needs updating for Minecraft 1.21.4:

1. **Minecraft version** in `build.gradle`: Update from `1.21` to `1.21.4`
2. **Fabric Loader**: Update to a version compatible with 1.21.4 (check [Fabric website](https://fabricmc.net/versions.html))
3. **Fabric API**: Update to the 1.21.4 compatible version
4. **Yarn mappings**: Update to 1.21.4 mappings (format: `1.21.4+build.X:v2`)
5. **Fabric Loom**: Update to a stable version (1.9+ as mentioned in the plan)
6. **fabric.mod.json**: Update the Minecraft dependency from `"1.21"` to `"1.21.4"`

### What should NOT be updated:

- ❌ **Forge version** - Not applicable, this is a Fabric mod
- ❌ **MCP mappings** - Fabric uses Yarn mappings instead
- ❌ **mods.toml** or **mcmod.info** - Fabric uses `fabric.mod.json`

### Recommendation:

Please update the PR description and checklist to reflect that this is a Fabric mod update, not a Forge mod update. The current description mentions several Forge-specific concepts that don't apply to this project.

Focus the update on:
- Fabric Loader version
- Fabric API version
- Yarn mappings for 1.21.4
- Fabric Loom version
- Testing with Fabric framework

---

**Note**: This comment was prepared to clarify the modding framework being used. The repository name "Forge-And-Fury" might be causing confusion, but the actual implementation is entirely Fabric-based.
