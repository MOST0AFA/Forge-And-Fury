package dev.most0afa.forge.and.fury;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.recipe.RecipeManager;
import net.minecraft.recipe.ShapelessRecipe;
import net.minecraft.recipe.Ingredient;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.registry.Registries;
import net.minecraft.resource.ResourceManager;
import net.minecraft.resource.ResourceType;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import dev.most0afa.forge.and.fury.Items.ModItems;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

public class ForgeAndFuryMain implements ModInitializer {
    public static final String MOD_ID = "forgeandfury";
    private static final Gson GSON = new Gson();
    private static RecipeManager serverRecipeManager;
    @Override
    public void onInitialize() {
        System.out.println("=== FORGE & FURY MOD INITIALIZING ===");
        System.out.println("Mod ID: " + MOD_ID);
        ModItems.registerItems();
        ModCreativeTabs.registerModCreativeTabs();
        System.out.println("=== ITEM REGISTRATION CHECK (onInitialize) ===");
        Identifier fireAxeId = Identifier.of(MOD_ID, "fire_axe");
        if (Registries.ITEM.containsId(fireAxeId)) {
            System.out.println("✓ fire_axe item found in registry!");
        } else {
            System.out.println("✗ fire_axe item NOT found in registry!");
        }

        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new CustomRecipeReloadListener());

        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            serverRecipeManager = server.getRecipeManager();
            System.out.println("=== FINAL RECIPE DEBUG INFO (SERVER STARTED) ===");
            System.out.println("Total recipes loaded by RecipeManager: " + serverRecipeManager.values().size());

            Identifier targetRecipeId = Identifier.of(MOD_ID, "fire_axe");
            Optional<?> recipeOptional = serverRecipeManager.get(targetRecipeId);
            boolean recipeExists = recipeOptional.isPresent();
            System.out.println("Is 'fire_axe' recipe present in manager? " + recipeExists);

            if (recipeExists) {
                System.out.println("Successfully found 'fire_axe' recipe in manager!");
            } else {
                System.out.println("ATTENTION: 'fire_axe' recipe NOT FOUND in manager after server started!");
            }

            long modRecipeCount = serverRecipeManager.values().stream()
                    .filter(recipeEntry -> recipeEntry.id().getNamespace().equals(MOD_ID))
                    .count();
            System.out.println("Total recipes from " + MOD_ID + " namespace: " + modRecipeCount);

            System.out.println("Listing all recipes from " + MOD_ID + " namespace:");
            serverRecipeManager.values().stream()
                    .filter(recipeEntry -> recipeEntry.id().getNamespace().equals(MOD_ID))
                    .forEach(recipeEntry -> System.out.println("  - Found recipe ID: " + recipeEntry.id() + " (Type: " + recipeEntry.value().getType().toString() + ")"));
        });

        System.out.println("=== INITIALIZATION COMPLETE ===");
    }

    private static class CustomRecipeReloadListener implements SimpleSynchronousResourceReloadListener {
        public Identifier getId() {
            return Identifier.of(MOD_ID, "recipes_manual_loader");
        }

        public Identifier getFabricId() {
            return getId();
        }

        @Override
        public void reload(ResourceManager manager) {
            System.out.println("--- CustomRecipeReloadListener: Attempting manual recipe loading ---");
            System.out.println("Looking for data/" + MOD_ID + "/recipe/fire_axe.json");

            Identifier recipeFileId = Identifier.of(MOD_ID, "recipe/fire_axe.json");

            manager.getResource(recipeFileId).ifPresentOrElse(resource -> {
                try (InputStream stream = resource.getInputStream();
                     InputStreamReader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {

                    JsonElement jsonElement = JsonParser.parseReader(reader);
                    JsonObject jsonObject = jsonElement.getAsJsonObject();

                    System.out.println("Successfully read and parsed JSON for " + recipeFileId.toString());
                    System.out.println("JSON Content Preview: " + GSON.toJson(jsonObject));

                    Identifier recipeId = Identifier.of(recipeFileId.getNamespace(), recipeFileId.getPath().replace("recipe/", "").replace(".json", ""));
                    String type = jsonObject.get("type").getAsString();
                    if (!type.equals("minecraft:crafting_shapeless")) {
                        System.err.println("ERROR: Recipe type for " + recipeFileId.toString() + " is not 'minecraft:crafting_shapeless'. Skipping manual add.");
                        return;
                    }

                    DefaultedList<Ingredient> ingredients = DefaultedList.of();
                    jsonObject.getAsJsonArray("ingredients").forEach(ingJson -> {
                        JsonObject obj = ingJson.getAsJsonObject();
                        if (obj.has("item")) {
                            Identifier itemId = Identifier.of(obj.get("item").getAsString());
                            ingredients.add(Ingredient.ofItems(Registries.ITEM.get(itemId)));
                        }
                    });

                    JsonObject resultJson = jsonObject.getAsJsonObject("result");
                    Identifier resultItemId = Identifier.of(resultJson.get("item").getAsString());
                    int count = resultJson.has("count") ? resultJson.get("count").getAsInt() : 1;
                    ItemStack resultStack = new ItemStack(Registries.ITEM.get(resultItemId), count);

                    ShapelessRecipe manualRecipe = new ShapelessRecipe(
                            recipeId.getPath(),
                            CraftingRecipeCategory.MISC,
                            resultStack,
                            ingredients
                    );

                    if (serverRecipeManager != null) {
                        System.out.println("Successfully created ShapelessRecipe object for " + recipeId.toString());
                    } else {
                        System.err.println("ERROR: serverRecipeManager is null during CustomRecipeReloadListener.");
                    }

                } catch (JsonSyntaxException e) {
                    System.err.println("ERROR: JSON syntax error in " + recipeFileId.toString() + ": " + e.getMessage());
                    e.printStackTrace();
                } catch (Exception e) {
                    System.err.println("ERROR: Failed to process recipe file " + recipeFileId.toString() + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }, () -> {
                System.err.println("ERROR: Could not find resource file: " + recipeFileId.toString());
            });

            System.out.println("--- CustomRecipeReloadListener finished manual loading attempt ---");
        }
    }
}