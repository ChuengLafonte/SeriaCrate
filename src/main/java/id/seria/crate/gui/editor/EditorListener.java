package id.seria.crate.gui.editor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;

import id.seria.crate.SeriaCrate;

public class EditorListener implements Listener {

    public static class ChatPrompt {
        public String type; 
        public String crateId;
        public int lineIndex;

        public ChatPrompt(String type, String crateId, int lineIndex) {
            this.type = type; this.crateId = crateId; this.lineIndex = lineIndex;
        }
    }

    private final Map<UUID, ChatPrompt> activePrompts = new HashMap<>();

    @EventHandler
    public void onEditorClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof EditorHolder)) return;

        EditorHolder holder = (EditorHolder) event.getInventory().getHolder();
        Player player = (Player) event.getWhoClicked();

        if (holder.getType() == EditorHolder.MenuType.ITEM_EDITOR && event.getRawSlot() < 45) return;
        if (holder.getType() == EditorHolder.MenuType.BLOCK_EDITOR && event.getRawSlot() == 13) return;

        event.setCancelled(true);
        if (event.getCurrentItem() == null) return;
        String itemName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());

        switch (holder.getType()) {
            case MAIN_MENU:
                if (itemName.equals("+ Buat Crate Baru")) {
                    player.closeInventory();
                    player.sendMessage("§e[SeriaCrate] Ketik nama Crate baru di chat (tanpa spasi).");
                    activePrompts.put(player.getUniqueId(), new ChatPrompt("CREATE_CRATE", null, -1));
                } else if (!itemName.isEmpty() && !itemName.equals(" ")) {
                    EditorMenuManager.openCrateSettings(player, itemName.toLowerCase());
                }
                break;

            case CRATE_SETTINGS:
                if (itemName.equals("Edit Rewards")) EditorMenuManager.openTierSelection(player, holder.getCrateId());
                else if (itemName.equals("Ubah Blok Fisik")) EditorMenuManager.openBlockEditor(player, holder.getCrateId());
                else if (itemName.equals("Edit Hologram")) EditorMenuManager.openHologramEditor(player, holder.getCrateId());
                else if (itemName.equals("Ambil Crate Item")) {
                    player.getInventory().addItem(id.seria.crate.util.ItemUtils.getCrateItem(holder.getCrateId()));
                    player.sendMessage("§a[SeriaCrate] Item Crate ditambahkan ke inventory!");
                }
                else if (itemName.equals("Kembali")) EditorMenuManager.openMainMenu(player);
                break;

            case HOLOGRAM_EDITOR:
                if (itemName.equals("Kembali")) {
                    EditorMenuManager.openCrateSettings(player, holder.getCrateId());
                } else if (itemName.equals("+ Tambah Baris")) {
                    player.closeInventory();
                    player.sendMessage("§eKetik teks untuk baris baru di chat. (Gunakan & untuk warna)");
                    activePrompts.put(player.getUniqueId(), new ChatPrompt("ADD_LINE", holder.getCrateId(), -1));
                } else if (itemName.startsWith("Baris ")) {
                    int lineIdx = Integer.parseInt(itemName.replace("Baris ", "")) - 1;
                    if (event.isRightClick()) {
                        deleteHologramLine(holder.getCrateId(), lineIdx);
                        EditorMenuManager.openHologramEditor(player, holder.getCrateId()); 
                    } else if (event.isLeftClick()) {
                        player.closeInventory();
                        player.sendMessage("§eKetik teks pengganti untuk baris " + (lineIdx + 1) + ". (Ketik 'batal')");
                        activePrompts.put(player.getUniqueId(), new ChatPrompt("EDIT_LINE", holder.getCrateId(), lineIdx));
                    }
                }
                break;

            case TIER_SELECTION:
                if (itemName.equals("Kembali")) EditorMenuManager.openCrateSettings(player, holder.getCrateId());
                else if (itemName.startsWith("TIER ")) {
                    EditorMenuManager.openItemEditor(player, holder.getCrateId(), itemName.replace("TIER ", "").toLowerCase());
                }
                break;

            case ITEM_EDITOR:
                if (itemName.equals("Simpan & Kembali")) {
                    player.sendMessage("§eMenyimpan Rewards...");
                    ItemStack[] itemsToSave = new ItemStack[45];
                    for (int i = 0; i < 45; i++) itemsToSave[i] = event.getInventory().getItem(i);
                    SeriaCrate.getInstance().getRewardManager().saveRewardsFromEditor(holder.getCrateId(), holder.getTierId(), itemsToSave);
                    player.sendMessage("§aTersimpan!");
                    EditorMenuManager.openTierSelection(player, holder.getCrateId());
                }
                break;

            case BLOCK_EDITOR:
                if (itemName.equals("Simpan & Kembali")) {
                    ItemStack blockItem = event.getInventory().getItem(13);
                    if (blockItem != null && blockItem.getType().isBlock()) {
                        File file = new File(SeriaCrate.getInstance().getConfigManager().getRewardsFolder(), holder.getCrateId() + ".yml");
                        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                        config.set("crate-settings.block", blockItem.getType().name());
                        try { config.save(file); } catch (IOException ignored) {}
                        player.sendMessage("§aBlok tampilan diubah!");
                    }
                    EditorMenuManager.openCrateSettings(player, holder.getCrateId());
                }
                break;
        }
    }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!activePrompts.containsKey(player.getUniqueId())) return;

        event.setCancelled(true);
        ChatPrompt prompt = activePrompts.remove(player.getUniqueId());
        String msg = event.getMessage();

        Bukkit.getScheduler().runTask(SeriaCrate.getInstance(), () -> {
            if (msg.equalsIgnoreCase("batal")) {
                if (prompt.crateId != null) EditorMenuManager.openHologramEditor(player, prompt.crateId);
                return;
            }

            FileConfiguration holoConfig = SeriaCrate.getInstance().getConfigManager().getHologram();

            if (prompt.type.equals("CREATE_CRATE")) {
                String newCrateName = msg.toLowerCase().replace(" ", "_");
                
                // 1. Buat file crate baru
                File file = new File(SeriaCrate.getInstance().getConfigManager().getRewardsFolder(), newCrateName + ".yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                config.set("crate-settings.block", "ENDER_CHEST");
                try { config.save(file); } catch (IOException ignored) {}

                // 2. Set default hologram
                holoConfig.set("holograms." + newCrateName, java.util.Arrays.asList("&e&l" + newCrateName.toUpperCase() + " CRATE", "&7Menghilang dalam: &c%timer%", "&fKlik Kiri: &aPreview", "&fKlik Kanan: &eBuka"));
                SeriaCrate.getInstance().getConfigManager().saveHologram();

                SeriaCrate.getInstance().getRewardManager().loadRewards();
                player.sendMessage("§aCrate " + newCrateName + " berhasil dibuat!");
                EditorMenuManager.openCrateSettings(player, newCrateName);
            } 
            else if (prompt.type.equals("ADD_LINE")) {
                List<String> lines = holoConfig.getStringList("holograms." + prompt.crateId);
                lines.add(msg);
                holoConfig.set("holograms." + prompt.crateId, lines);
                SeriaCrate.getInstance().getConfigManager().saveHologram();
                EditorMenuManager.openHologramEditor(player, prompt.crateId);
            } 
            else if (prompt.type.equals("EDIT_LINE")) {
                List<String> lines = holoConfig.getStringList("holograms." + prompt.crateId);
                if (prompt.lineIndex < lines.size()) {
                    lines.set(prompt.lineIndex, msg);
                    holoConfig.set("holograms." + prompt.crateId, lines);
                    SeriaCrate.getInstance().getConfigManager().saveHologram();
                }
                EditorMenuManager.openHologramEditor(player, prompt.crateId);
            }
        });
    }

    private void deleteHologramLine(String crateId, int index) {
        FileConfiguration config = SeriaCrate.getInstance().getConfigManager().getHologram();
        List<String> lines = config.getStringList("holograms." + crateId);
        if (index < lines.size()) {
            lines.remove(index);
            config.set("holograms." + crateId, lines);
            SeriaCrate.getInstance().getConfigManager().saveHologram();
        }
    }
}