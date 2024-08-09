/*
 * This file is part of HuskHomesGUI, licensed under the Apache License 2.0.
 *
 *  Copyright (c) William278 <will27528@gmail.com>
 *  Copyright (c) contributors
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package net.william278.huskhomes.gui.menu;

import de.themoep.inventorygui.InventoryGui;
import de.themoep.inventorygui.StaticGuiElement;
import net.wesjd.anvilgui.AnvilGUI;
import net.william278.huskhomes.gui.HuskHomesGui;
import net.william278.huskhomes.position.Home;
import net.william278.huskhomes.position.SavedPosition;
import net.william278.huskhomes.position.Warp;
import net.william278.huskhomes.user.OnlineUser;
import net.william278.huskhomes.util.ValidationException;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static net.william278.huskhomes.gui.config.Locales.textWrap;

/**
 * A menu for editing a saved position
 */
public class EditMenu<T extends SavedPosition> extends Menu {

    private final T position;
    private final Type type;
    private final ListMenu<T> parentMenu;
    private final int pageNumber;

    @NotNull
    private static String[] getEditMenuLayout() {
        return new String[]{"       ub", "  ic  r  ", "         ",};
    }

    private EditMenu(@NotNull HuskHomesGui plugin, @NotNull T position, @NotNull ListMenu<T> parentMenu, int pageNumber) {
        super(plugin, plugin.getLocales().getLocale(position instanceof Home ? "home_editor_title" : "warp_editor_title", position.getName()), getEditMenuLayout());
        this.type = position instanceof Home ? Type.HOME : Type.WARP;
        this.position = position;
        this.parentMenu = parentMenu;
        this.pageNumber = pageNumber;
    }

    public static EditMenu<Home> home(@NotNull HuskHomesGui plugin, @NotNull Home home, @NotNull ListMenu<Home> parentMenu, int pageNumber) {
        return new EditMenu<>(plugin, home, parentMenu, pageNumber);
    }

    public static EditMenu<Warp> warp(@NotNull HuskHomesGui plugin, @NotNull Warp warp, @NotNull ListMenu<Warp> parentMenu, int pageNumber) {
        return new EditMenu<>(plugin, warp, parentMenu, pageNumber);
    }

    @Override
    protected Consumer<InventoryGui> buildMenu() {
        return (menu) -> {
            final ItemStack positionIcon = new ItemStack(getPositionMaterial(position).orElse(plugin.getSettings().getDefaultIcon()));
            menu.setCloseAction(i -> false);

            // Filler background icons
            menu.addElement(new StaticGuiElement('a', new ItemStack(switch (type) {
                case HOME, PUBLIC_HOME -> plugin.getSettings().getHomeEditorFillerIcon();
                case WARP -> plugin.getSettings().getWarpEditorFillerIcon();
            }), " "));

            // Return to the parent list menu
            menu.addElement(new StaticGuiElement('b', new ItemStack(plugin.getSettings().getEditorBackButtonIcon()), (click) -> {
                if (click.getWhoClicked() instanceof Player player) {
                    final OnlineUser user = api.adaptUser(player);
                    this.close(user);
                    parentMenu.show(user);
                    parentMenu.setPageNumber(user, pageNumber);
                    this.destroy();
                }
                return true;
            }, plugin.getLocales().getLocale("back_button")));

            menu.addElement(new StaticGuiElement('c', new ItemStack(plugin.getSettings().getComingSOONButton()), (click) -> {
                // NO Code
                return true;
            }, plugin.getLocales().getLocale("coming_soon_name")));

            // INFO
            menu.addElement(new StaticGuiElement('u', new ItemStack(plugin.getSettings().getEditorEditLocationButtonIcon()), (click) -> {

                return true;
            }, plugin.getLocales().getLocale("edit_information_button"), plugin.getLocales().getLocale("edit_information_default_message-1", position instanceof Home ? plugin.getLocales().getLocale("type_home") : plugin.getLocales().getLocale("type_warp"), position.getName()), plugin.getLocales().getLocale("edit_information_default_message-2", position instanceof Home ? plugin.getLocales().getLocale("type_home") : plugin.getLocales().getLocale("type_warp"), position.getName()), plugin.getLocales().getLocale("edit_information_default_message-3", position instanceof Home ? plugin.getLocales().getLocale("type_home") : plugin.getLocales().getLocale("type_warp"), position.getName()), plugin.getLocales().getLocale("edit_information_default_message-4", position instanceof Home ? plugin.getLocales().getLocale("type_home") : plugin.getLocales().getLocale("type_warp"), position.getName())));

            // Editing name (Via anvil) - NOT USE
            menu.addElement(new StaticGuiElement('n', new ItemStack(plugin.getSettings().getEditorEditNameButtonIcon()), (click) -> {
                if (click.getWhoClicked() instanceof Player player) {
                    this.close(api.adaptUser(player));
                    new AnvilGUI.Builder().onClose(stateSnapshot -> {
                                stateSnapshot.getPlayer().sendMessage("You closed the inventory.");
                            }).onClick((slot, stateSnapshot) -> { // Either use sync or async variant, not both
                                if (slot != AnvilGUI.Slot.OUTPUT) {
                                    return Collections.emptyList();
                                }

                                if (stateSnapshot.getText().equalsIgnoreCase("you")) {
                                    stateSnapshot.getPlayer().sendMessage("You have magical powers!");
                                    return Arrays.asList(AnvilGUI.ResponseAction.close());
                                } else {
                                    return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText("Try again"));
                                }
                            }).preventClose()                                                    //prevents the inventory from being closed
                            .text("What is the meaning of life?")                              //sets the text the GUI should start with
                            .title("Enter your answer.")                                       //set the title of the GUI (only works in 1.14+)
                            .plugin(plugin)                                          //set the plugin instance
                            .open(player);
                }
                return true;
            }, plugin.getLocales().getLocale("edit_name_button")));

            // Editing description (Via anvil) - NOT USE
            menu.addElement(new StaticGuiElement('d', new ItemStack(plugin.getSettings().getEditorEditDescriptionButtonIcon()), (click) -> {
                if (click.getWhoClicked() instanceof Player player) {
                    this.close(api.adaptUser(player));

                }
                return true;
            }, plugin.getLocales().getLocale("edit_description_button"),

                    // description
                    (!position.getMeta().getDescription().isBlank() ? plugin.getLocales().getLocale("edit_description_default_message", textWrap(plugin, position.getMeta().getDescription())) : plugin.getLocales().getLocale("edit_description_default_message_blank"))));

            // Editing home privacy - NOT USE
            if (position instanceof Home home) {
                menu.addElement(new StaticGuiElement('p', new ItemStack(plugin.getSettings().getEditorEditPrivacyButtonIcon()), (click) -> {
                    if (click.getWhoClicked() instanceof Player player) {
                        try {
                            api.setHomePrivacy(home, !home.isPublic());
                            // Update the status display on the menu
                            home.setPublic(!home.isPublic());
                            this.show(api.adaptUser(player));
                        } catch (ValidationException e) {
                            return true;
                        }
                    }
                    return true;
                }, plugin.getLocales().getLocale("edit_privacy_button"), plugin.getLocales().getLocale("edit_privacy_message", (home.isPublic() ? plugin.getLocales().getLocale("edit_privacy_message_public") : plugin.getLocales().getLocale("edit_privacy_message_private")))));
            }

            // Deleting
            menu.addElement(new StaticGuiElement('r', new ItemStack(plugin.getSettings().getEditorDeleteButtonIcon()), (click) -> {
                switch (click.getType()) {
                    case RIGHT, DROP -> { // DROP: geyser player throw item
                        if (click.getWhoClicked() instanceof Player player) {
                            this.close(api.adaptUser(player));
                            try {
                                if (position instanceof Home home) {
                                    api.deleteHome(home);
                                    home.getMeta().setName(plugin.getLocales().getLocale("item_deleted_name", home.getName())); // update listMenu
                                } else if (position instanceof Warp warp) {
                                    api.deleteWarp(warp);
                                    warp.getMeta().setName(plugin.getLocales().getLocale("item_deleted_name", warp.getName())); // update listMenu
                                }
                            } catch (ValidationException e) {
                                return true;
                            }

                            // Return to the parent list menu
                            final OnlineUser user = api.adaptUser(player);
                            this.close(user);
                            parentMenu.show(user);
                            parentMenu.setPageNumber(user, pageNumber);
                            this.destroy();
                        }
                    }
                }
                return true;
            }, plugin.getLocales().getLocale("delete_button"), plugin.getLocales().getLocale("delete_button_describe")));

            // Controls display
            menu.addElement(new StaticGuiElement('i', new ItemStack(Material.KNOWLEDGE_BOOK),
                    // Name
                    plugin.getLocales().getLocale("item_info_name", position.getName()),
                    // World name
                    plugin.getLocales().getLocale("item_info_world", position.getWorld().getName()),
                    // Server name
                    plugin.getLocales().getLocale("item_info_server", position.getServer()),
                    // Coordinates
                    plugin.getLocales().getLocale("item_info_coordinates", Integer.toString((int) Math.floor(position.getX())), Integer.toString((int) Math.floor(position.getY())), Integer.toString((int) Math.floor(position.getZ()))),
                    // Owner name (Only for homes)
                    position instanceof Home home ? plugin.getLocales().getLocale("home_owner_name", home.getOwner().getUsername()) : ""));
        };
    }

}
