package net.william278.huskhomes.gui;

import de.themoep.inventorygui.*;
import de.themoep.minedown.adventure.MineDown;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.wesjd.anvilgui.AnvilGUI;
import net.william278.huskhomes.api.HuskHomesAPI;
import net.william278.huskhomes.player.OnlineUser;
import net.william278.huskhomes.position.*;
import net.william278.huskhomes.util.Permission;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static net.william278.huskhomes.gui.Util.*;

/**
 * A menu for displaying a list of saved positions
 */
public class SavedPositionMenu {

//    private static final String[] MENU_LAYOUT = {
//            "ppppppppp",
//            "ppppppppp",
//            "ppppppppp",
//            "bl  i  ne",
//    };

    // 传送点列表GUI // 自定义箱子尺寸
    private static final String[] MENU_LAYOUT = Arrays.copyOfRange(
            new String[] {
                    "ppppppppp",
                    "ppppppppp",
                    "ppppppppp",
                    "ppppppppp",
                    "ppppppppp",
                    "bl  i  ne"},
            6 - getIntFromConfig("menu.size"), // 2 ~ 6, 为1时只显示操作栏
            6);

    // 编辑传送点GUI
    private static final String[] EDIT_MENU_LAYOUT = new String[] {
            "aa     ab",
            "aundip ra",
            "aa     aa"};


    private static final String TAG_KEY = "huskhomesgui:icon";
    private final InventoryGui menu;
    private final InventoryGui edit_menu;
    private final HuskHomesAPI huskHomesAPI;
    private final MenuType menuType;

    protected static SavedPositionMenu create(@NotNull HuskHomesGui plugin,
                                              @NotNull List<? extends SavedPosition> savedPositions,
                                              @NotNull MenuType type, @NotNull String title) {
        return new SavedPositionMenu(plugin, HuskHomesAPI.getInstance(), savedPositions, type, title);
    }

    protected void show(@NotNull OnlineUser onlineUser) {
        menu.show(huskHomesAPI.getPlayer(onlineUser));
    }

    private SavedPositionMenu(@NotNull HuskHomesGui plugin,
                              @NotNull HuskHomesAPI huskHomesAPI,
                              @NotNull List<? extends SavedPosition> positionList,
                              @NotNull MenuType menuType,
                              @NotNull String title) {

        this.menu = new InventoryGui(plugin, title, MENU_LAYOUT);
        this.edit_menu = new InventoryGui(plugin, "编辑GUI", EDIT_MENU_LAYOUT); // ?
        this.menuType = menuType;
        this.huskHomesAPI = huskHomesAPI;

        // Add filler items
        this.menu.setFiller(new ItemStack(menuType.fillerMaterial, 1));

        // Add pagination handling
        this.menu.addElement(getPositionGroup(positionList));
        this.menu.addElement(new GuiPageElement('b',
                new ItemStack(getItemFromConfig("menu.pagination.FIRST.item")),
                GuiPageElement.PageAction.FIRST,
                getLegacyText(getMessageFromConfig("menu.pagination.FIRST.title"))));
        this.menu.addElement(new GuiPageElement('l',
                new ItemStack(getItemFromConfig("menu.pagination.PREVIOUS.item")),
                GuiPageElement.PageAction.PREVIOUS,
                getLegacyText(getMessageFromConfig("menu.pagination.PREVIOUS.title"))));
        this.menu.addElement(new GuiPageElement('n',
                new ItemStack(getItemFromConfig("menu.pagination.NEXT.item")),
                GuiPageElement.PageAction.NEXT,
                getLegacyText(getMessageFromConfig("menu.pagination.NEXT.title"))));
        this.menu.addElement(new GuiPageElement('e',
                new ItemStack(getItemFromConfig("menu.pagination.LAST.item")),
                GuiPageElement.PageAction.LAST,
                getLegacyText(getMessageFromConfig("menu.pagination.LAST.title"))));
        // 信息显示
        if(getBooleanFromConfig("menu.pagination.INFO.enable")){
            this.menu.addElement(new StaticGuiElement('i',
                    new ItemStack(getItemFromConfig("menu.pagination.INFO.item")),
                    getLegacyText(getMessageFromConfig("menu.pagination.INFO.title")),
                    getLegacyText(getMessageFromConfig("menu.pagination.INFO.message.A")),
                    getLegacyText(getMessageFromConfig("menu.pagination.INFO.message.B")),
                    getLegacyText(getMessageFromConfig("menu.pagination.INFO.message.C"))));
        }

    }

    // 遍历传送点, 并添加按钮
    @NotNull
    private GuiElementGroup getPositionGroup(@NotNull List<? extends SavedPosition> positions) {
        final GuiElementGroup group = new GuiElementGroup('p');
        for (SavedPosition position : positions) {
            group.addElement(getPositionButton(position));
        }
        return group;
    }

    // 创建一个传送点按钮
    @NotNull
    private StaticGuiElement getPositionButton(@NotNull SavedPosition position) {
        ItemStack position_item = new ItemStack(getPositionMaterial(position).orElse(getItemFromConfig("menu.item.default-item")));
        return new StaticGuiElement('e',
                position_item,
                // 点击传送点物品时
                click -> {
                    if (click.getWhoClicked() instanceof Player player) {
                        final OnlineUser onlineUser = huskHomesAPI.adaptUser(player);
                        System.out.println("点击图标: "+ click.getType()); // test
                        switch (click.getType()) {
                            case LEFT -> { // 左键传送
                                menu.close(true);
                                huskHomesAPI.teleportPlayer(onlineUser, position, true);
                            }
                            case RIGHT -> { // 右键编辑
                                menu.close(true);
//                                player.performCommand(switch (menuType) {
//                                    case HOME, PUBLIC_HOME ->
//                                            "huskhomes:edithome " + ((Home) position).owner.username + "." + position.meta.name;
//                                    case WARP -> "huskhomes:editwarp " + position.meta.name;
//                                });

                                switch (menuType) {
                                    case HOME, PUBLIC_HOME -> {
                                        getEditGui(position, position_item).show(player);
                                    }
                                    case WARP -> {

                                    }
                                }
                            }
                            case SHIFT_LEFT -> { // 设置物品
                                if (canEditPosition(position, player) && player.getInventory().getItemInMainHand().getType() != Material.AIR) {
                                    menu.close(true);
                                    setPositionMaterial(position, player.getInventory().getItemInMainHand().getType())
                                            .thenRun(() -> player.sendMessage(getLegacyText( getMessageFromConfig("chat.updated-icon") )
                                                    .replaceAll("%1%", position.meta.name)));
                                }
                            }
                        }
                    }
                    return true;
                },
                getLegacyText(getMessageFromConfig("menu.item.name").replace("%1%", position.meta.name)),
                getLegacyText(getMessageFromConfig("menu.item.description").replace("%1%", position.meta.description.isBlank()
                        ? huskHomesAPI.getRawLocale("menu.item_no_description").orElse(getMessageFromConfig("menu.item.description-var1-no"))
                        : position.meta.description)),
                getMessageFromConfig("menu.item.space"),
                getLegacyText(getMessageFromConfig("menu.item.Left")),
                getLegacyText(getMessageFromConfig("menu.item.Right")),
                getLegacyText(getMessageFromConfig("menu.item.Shift")));
    }

    // 输出编辑菜单
    private InventoryGui getEditGui(SavedPosition position, ItemStack item) {
        // a = 背景
        // b = ~~返回~~ 关闭 (不会写返回= =
        // u = 更新位置
        // -n = 更新名称
        // d = 更新描述
        // i = 显示信息
        // p = 开放 (phome)
        // r = 删除
        // 背景
        this.edit_menu.addElement(new StaticGuiElement('a',
                new ItemStack(Material.LIME_STAINED_GLASS_PANE)));

        // 关闭按钮
        this.edit_menu.addElement(new StaticGuiElement('b',
                new ItemStack(Material.ORANGE_STAINED_GLASS_PANE),
                click -> {
                    if (click.getWhoClicked() instanceof Player player) {
                        System.out.println("点击编辑页面的图标: "+ click.getType()); // test
                        edit_menu.close(true);
                    }
                    return true;
                },
                getLegacyText("关闭按钮")));

        // 更新位置
        this.edit_menu.addElement(new StaticGuiElement('u',
                new ItemStack(Material.OAK_BOAT),
                click -> {
                    if (click.getWhoClicked() instanceof Player player) {
                        System.out.println("点击编辑页面的图标: "+ click.getType()); // test
//                      edit_menu.close(true);
                        player.performCommand("huskhomes:edithome " + ((Home) position).owner.username +"."+ position.meta.name +"relocate");
                    }
                    return true;
                },
                getLegacyText("更新位置")));

        // 更新名称
        this.edit_menu.addElement(new StaticGuiElement('n',
                new ItemStack(Material.NAME_TAG),
                click -> {
                    Player player = (Player) click.getWhoClicked();
                    if (player != null) {
                        System.out.println("点击编辑页面的图标: "+ click.getType()); // test
                        edit_menu.close(true);
                        new AnvilGUI.Builder()
                                .onClose(playerInAnvil -> {
                                    playerInAnvil.sendMessage("You closed the inventory.");
                                })
                                .onComplete((completion) -> {
                                    if(completion.getText().equalsIgnoreCase("you")) {
                                        completion.getPlayer().sendMessage("You have magical powers!");
                                        return Arrays.asList(AnvilGUI.ResponseAction.close());
                                    } else {
                                        return Arrays.asList(AnvilGUI.ResponseAction.replaceInputText("Try again"));
                                    }
                                })
                                .itemLeft(new ItemStack(item))
                                .itemRight(new ItemStack(Material.NAME_TAG))
                                .title("编辑名称")
                                // 使玩家打开它
                                .open(player);
                    }
                    return true;
                },
                getLegacyText("编辑名称")));


        return this.edit_menu;
    }

    /**
     * Get the material to use for a saved position by icon tag
     *
     * @param position The saved position
     * @return The material to use if found
     */
    private Optional<Material> getPositionMaterial(@NotNull SavedPosition position) {
        if (position.meta.tags.containsKey(TAG_KEY)) {
            return Optional.ofNullable(Material.matchMaterial(position.meta.tags.get(TAG_KEY)));
        }
        return Optional.empty();
    }

    /**
     * Set the material to use for a {@link SavedPosition} and update it in the database
     *
     * @param position The saved position
     * @param material The {@link Material} to use
     * @return A future that completes when the saved position has been updated
     */
    private CompletableFuture<SavedPositionManager.SaveResult> setPositionMaterial(@NotNull SavedPosition position,
                                                                                   @NotNull Material material) {
        final PositionMeta meta = position.meta;
        meta.tags.put(TAG_KEY, material.getKey().toString());
        if (menuType == MenuType.WARP) {
            final Warp warp = (Warp) position;
            return huskHomesAPI.updateWarpMeta(warp, meta);
        } else {
            final Home home = (Home) position;
            return huskHomesAPI.updateHomeMeta(home, meta);
        }
    }

    /**
     * Get if a {@link Player} can edit a {@link SavedPosition}
     *
     * @param position The saved position
     * @param player   The player
     * @return {@code true} if the player can edit the position
     */
    private boolean canEditPosition(@NotNull SavedPosition position, @NotNull Player player) {
        // Validate warp permission checks
        if (menuType == MenuType.WARP) {
            if (!player.hasPermission(Permission.COMMAND_EDIT_WARP.node)) {
//                getLocale("error_no_permission").ifPresent(player::sendMessage);
                return false;
            }
            return true;
        }

        // Validate home permission checks
        if (menuType == MenuType.HOME || menuType == MenuType.PUBLIC_HOME) {
            final Home home = (Home) position;
            if (player.getUniqueId().equals(home.owner.uuid)) {
                if (!player.hasPermission(Permission.COMMAND_EDIT_HOME.node)) {
//                    getLocale("error_no_permission").ifPresent(player::sendMessage);
                    return false;
                }
            } else {
                if (!player.hasPermission(Permission.COMMAND_EDIT_HOME_OTHER.node)) {
//                    getLocale("error_no_permission").ifPresent(player::sendMessage);
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * Gets a locale as legacy text from HuskHomes
     *
     * @param localeKey    The locale key
     * @param replacements The replacements to use
     * @return The legacy text
     */
    @SuppressWarnings("SameParameterValue")
    @NotNull
    private Optional<String> getLocale(@NotNull String localeKey, String... replacements) {
        return huskHomesAPI.getLocale(localeKey, replacements)
                .map(mineDown -> LegacyComponentSerializer.builder()
                        .build().serialize(mineDown.toComponent()));
    }

    /**
     * Convert MineDown formatted text into legacy text
     *
     * @param mineDown The MineDown formatted text
     * @return The legacy text
     */
    @NotNull
    private String getLegacyText(@NotNull String mineDown) {
        return LegacyComponentSerializer.builder()
                .build().serialize(new MineDown(mineDown).toComponent());
    }

    /**
     * Represents different types of {@link SavedPosition} that a {@link SavedPositionMenu} can display
     */
    protected enum MenuType {
        HOME(getItemFromConfig("menu.theme-item.HOME")),
        PUBLIC_HOME(getItemFromConfig("menu.theme-item.PUBLIC_HOME")),
        WARP(getItemFromConfig("menu.theme-item.WARP"));

        private final Material fillerMaterial;

        MenuType(@NotNull Material fillerMaterial) {
            this.fillerMaterial = fillerMaterial;
        }
    }

}