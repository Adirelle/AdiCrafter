@file:Suppress("UnstableApiUsage")

package dev.adirelle.adicrafter.crafter.power

import dev.adirelle.adicrafter.crafter.CrafterConfig
import net.fabricmc.fabric.api.transfer.v1.storage.Storage
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView
import net.minecraft.nbt.NbtCompound

interface PowerGenerator : Storage<PowerVariant>, StorageView<PowerVariant> {

    var onUpdate: () -> Unit

    fun readFromNbt(nbt: NbtCompound)
    fun writeToNbt(nbt: NbtCompound)

    companion object {

        fun fromConfig(config: CrafterConfig.PowerConfig): PowerGenerator =
            if (config.enabled)
                SteadyPowerGenerator(config.capacity, config.reloadRate)
            else
                IllimitedPowerGenerator()

    }

}
