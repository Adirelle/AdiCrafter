package dev.adirelle.adicrafter.screen

import io.github.cottonmc.cotton.gui.client.CottonInventoryScreen
import net.fabricmc.api.EnvType.CLIENT
import net.fabricmc.api.Environment
import net.minecraft.entity.player.PlayerInventory
import net.minecraft.text.Text

@Environment(CLIENT)
class CrafterScreen(gui: CrafterScreenHandler, playerInventory: PlayerInventory, title: Text) :
    CottonInventoryScreen<CrafterScreenHandler>(gui, playerInventory, title)
