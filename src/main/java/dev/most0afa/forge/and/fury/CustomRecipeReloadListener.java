package dev.most0afa.forge.and.fury;

import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class CustomRecipeReloadListener implements SimpleSynchronousResourceReloadListener {
    private static final Identifier ID = Identifier.of("forgeandfury", "custom_recipe_loader");

    @Override
    public Identifier getFabricId() {
        return ID;
    }

    @Override
    public void reload(ResourceManager resourceManager) {
        System.out.println("=== Custom Recipe Reload Listener triggered ===");

        Map<Identifier, net.minecraft.resource.Resource> recipeResources = resourceManager.findResources("recipes", path -> path.getPath().endsWith(".json"));

        System.out.println("Found recipe files:");
        for (Map.Entry<Identifier, net.minecraft.resource.Resource> entry : recipeResources.entrySet()) {
            Identifier recipeId = entry.getKey();
            if (!"forgeandfury".equals(recipeId.getNamespace())) {
                continue;
            }

            System.out.println(" - " + recipeId);
            try {
                net.minecraft.resource.Resource resource = entry.getValue();
                try (InputStream inputStream = resource.getInputStream()) {
                    byte[] data = inputStream.readAllBytes();
                    String json = new String(data, StandardCharsets.UTF_8);
                    System.out.println("Recipe JSON content: " + json);
                }
            } catch (Exception e) {
                System.out.println("Error reading recipe file " + recipeId + ": " + e.getMessage());
            }
        }
    }
}