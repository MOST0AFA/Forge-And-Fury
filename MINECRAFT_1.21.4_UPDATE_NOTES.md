# Minecraft 1.21.4 Update Notes

## Summary
This document describes the changes made to update the Forge-And-Fury mod from Minecraft 1.21 to Minecraft 1.21.4.

## Changes Made

### 1. build.gradle
- **Minecraft version**: Updated from `1.21` to `1.21.4`
- **Fabric Loom**: Changed to buildscript-based loading (version 1.6.12)
- **Fabric Loader**: Remains at `0.16.10` (compatible with both versions)
- **Fabric API**: Updated from `0.102.0+1.21` to `0.110.5+1.21.4`
- **Yarn mappings**: Updated from `1.21+build.2:v2` to `1.21.4+build.3:v2`

### 2. settings.gradle
- Added Fabric Snapshots repository for snapshot versions (if needed in future)
- Reordered repositories for better resolution

### 3. fabric.mod.json
- **fabricloader**: Updated minimum version from `>=0.14.21` to `>=0.16.10`
- **minecraft**: Updated version from `1.21` to `1.21.4`

### 4. pack.mcmeta
- **pack_format**: Updated from `34` to `48` (required for Minecraft 1.21.4)

## Known Issues

### Critical Blocker: maven.fabricmc.net Accessibility
**Status:** BLOCKED

The build process cannot complete because the domain `maven.fabricmc.net` is not accessible from the current environment. This domain is essential for:
- Fabric Loom (build tool)
- Fabric API (mod API)
- Fabric Loader (mod loader)
- Yarn Mappings (Minecraft deobfuscation mappings)

**Error Message:**
```
maven.fabricmc.net: No address associated with hostname
```

**Required Action:**
To proceed with building and testing the mod, access to `maven.fabricmc.net` must be granted in the environment configuration.

## Version Reference

### Minecraft 1.21.4 Fabric Versions
- **Minecraft**: 1.21.4
- **Fabric Loom**: 1.6.12+ (build tool)
- **Fabric Loader**: 0.16.10+ (mod loader)
- **Fabric API**: 0.110.5+1.21.4 (API)
- **Yarn Mappings**: 1.21.4+build.3 (mappings)
- **Pack Format**: 48 (resource pack format)
- **Java**: 21 (LTS)

## Next Steps (Once Domain Access is Granted)

1. **Build the Project**
   ```bash
   ./gradlew clean build
   ```

2. **Review Build Output**
   - Check for any deprecation warnings
   - Look for API changes that require code updates

3. **Test API Compatibility**
   - Review mixin compatibility with 1.21.4
   - Check for any renamed or removed methods in Minecraft/Fabric API
   - Test all custom items and their behaviors

4. **Code Changes (If Needed)**
   Based on the build output, the following areas may need attention:
   - Entity/living entity API changes
   - Particle effect API changes
   - Enchantment system changes
   - Recipe system changes

5. **Final Verification**
   - Run the mod in a test environment
   - Verify all custom items work correctly
   - Test multiplayer compatibility
   - Ensure resource packs load correctly

## Alternative Solutions

If `maven.fabricmc.net` cannot be made accessible:

1. **Manual Dependency Download**: Download required JARs manually and add them to a local Maven repository
2. **Mirror Repository**: Set up a mirror of the Fabric Maven repository in an accessible location
3. **Gradle Dependency Cache**: Pre-populate Gradle cache with required dependencies

## Files Modified
- `build.gradle` - Build configuration and dependencies
- `settings.gradle` - Plugin repository configuration
- `src/main/resources/fabric.mod.json` - Mod metadata
- `src/main/resources/pack.mcmeta` - Resource pack metadata

## Files Not Modified (But May Need Changes After Build)
- Java source files in `src/main/java/` - May need API compatibility updates
- Mixin files - May need updates if Minecraft internals changed
- Resource files - Should be compatible as-is
