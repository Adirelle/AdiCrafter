@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.utils.extensions

import dev.adirelle.adicrafter.crafter.impl.Grid
import dev.adirelle.adicrafter.utils.memoize
import dev.adirelle.adicrafter.utils.requireExactSize
import net.fabricmc.fabric.api.transfer.v1.item.InventoryStorage
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant
import net.fabricmc.fabric.api.transfer.v1.storage.base.SingleSlotStorage
import net.minecraft.entity.player.PlayerEntity
import net.minecraft.inventory.Inventory
import net.minecraft.item.ItemStack
import net.minecraft.recipe.CraftingRecipe
import net.minecraft.recipe.ShapedRecipe
import net.minecraft.recipe.ShapelessRecipe
import java.util.*
import kotlin.reflect.KProperty

fun Inventory.copyFrom(list: List<ItemStack>) {
    requireExactSize(list, size()).forEachIndexed { index, stack ->
        this[index] = stack
    }
}

fun Inventory.iterator() =
    InventoryIterator(this)

operator fun Inventory.get(index: Int): ItemStack =
    getStack(index)

operator fun Inventory.set(index: Int, stack: ItemStack) =
    setStack(index, stack)

private val storageMemoizer = memoize<Inventory, InventoryStorage> { inventory ->
    InventoryStorage.of(inventory, null)
}

fun Inventory.asStorage(): InventoryStorage =
    storageMemoizer(this)

class InventoryIterator(private val inventory: Inventory) : Iterator<ItemStack> {

    private var idx = 0
    override fun hasNext() = idx < inventory.size()
    override fun next(): ItemStack = inventory.getStack(idx++)
}

operator fun InventoryStorage.get(slot: Int): SingleSlotStorage<ItemVariant> =
    slots[slot]

private val propertyInventoryInstances =
    Collections.synchronizedMap(WeakHashMap<KProperty<ItemStack>, Inventory>())

fun KProperty<ItemStack>.toInventory(): Inventory =
    propertyInventoryInstances.computeIfAbsent(this) { property ->
        object : Inventory {
            fun get() = property.getter.call()

            override fun clear() {}
            override fun isEmpty() = get().isEmpty
            override fun getStack(slot: Int) = get().copy()
            override fun removeStack(slot: Int, amount: Int) = ItemStack.EMPTY
            override fun removeStack(slot: Int) = ItemStack.EMPTY
            override fun setStack(slot: Int, stack: ItemStack) {}
            override fun size() = 1
            override fun markDirty() {}
            override fun canPlayerUse(player: PlayerEntity) = true
        }
    }

fun Inventory.loadFrom(recipe: ShapedRecipe) {
    for (x in 0 until Grid.WIDTH) {
        for (y in 0 until Grid.HEIGHT) {
            setStack(y * Grid.WIDTH + x, recipe.getIngredient(x, y))
        }
    }
}

fun ShapedRecipe.getIngredient(x: Int, y: Int): ItemStack =
    if (x < width && y < height)
        getIngredient(y * width + x)
    else
        ItemStack.EMPTY

fun Inventory.loadFrom(recipe: ShapelessRecipe) {
    for (i in 0 until size()) {
        setStack(i, recipe.getIngredient(i))
    }
}

fun CraftingRecipe.getIngredient(i: Int): ItemStack =
    if (i < ingredients.size && !ingredients[i].isEmpty)
        ingredients[i].matchingStacks[0]
    else
        ItemStack.EMPTY
