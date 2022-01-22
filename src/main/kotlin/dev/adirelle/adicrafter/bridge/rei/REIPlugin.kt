package dev.adirelle.adicrafter.bridge.rei

import dev.adirelle.adicrafter.crafter.CrafterFeature
import dev.adirelle.adicrafter.crafter.CrafterScreenHandler
import me.shedaniel.rei.api.client.plugins.REIClientPlugin
import me.shedaniel.rei.api.client.registry.category.CategoryRegistry
import me.shedaniel.rei.api.client.registry.transfer.TransferHandler.Result
import me.shedaniel.rei.api.client.registry.transfer.TransferHandlerRegistry
import me.shedaniel.rei.api.common.entry.EntryIngredient
import me.shedaniel.rei.api.common.util.EntryStacks
import me.shedaniel.rei.plugin.common.BuiltinPlugin
import me.shedaniel.rei.plugin.common.displays.crafting.DefaultCraftingDisplay
import net.minecraft.recipe.CraftingRecipe

@Suppress("unused")
class REIPlugin : REIClientPlugin {

    override fun registerCategories(registry: CategoryRegistry) {
        registry.addWorkstations(
            BuiltinPlugin.CRAFTING,
            EntryIngredient.of(
                EntryStacks.of(CrafterFeature.BASIC_CRAFTER),
                EntryStacks.of(CrafterFeature.FUELED_CRAFTER)
            )
        )
    }

    override fun registerTransferHandlers(registry: TransferHandlerRegistry) {
        registry.register r@{ ctx ->
            ((ctx.display as? DefaultCraftingDisplay<*>)
                ?.optionalRecipe?.orElse(null) as? CraftingRecipe)
                ?.let { recipe ->
                    (ctx.menu as? CrafterScreenHandler)?.let { handler ->
                        if (ctx.isActuallyCrafting) {
                            ctx.minecraft.setScreen(ctx.containerScreen)
                            handler.applyRecipe(recipe)
                        }
                        Result.createSuccessful()
                    }
                }
                ?: Result.createNotApplicable()
        }
    }
}
