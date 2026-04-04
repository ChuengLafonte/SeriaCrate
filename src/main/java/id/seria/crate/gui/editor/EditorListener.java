package id.seria.crate.gui.editor;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.persistence.PersistentDataType;

import id.seria.crate.SeriaCrate;

public class EditorListener implements Listener {

    public static class ChatPrompt {
        public String type; public String crateId; public String tierId; public int dataIndex;
        public ChatPrompt(String type, String crateId, String tierId, int dataIndex) {
            this.type = type; this.crateId = crateId; this.tierId = tierId; this.dataIndex = dataIndex;
        }
    }
    private final Map<UUID, ChatPrompt> activePrompts = new HashMap<>();

    @EventHandler
    public void onEditorClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof EditorHolder)) return;
        EditorHolder holder = (EditorHolder) event.getInventory().getHolder();
        Player player = (Player) event.getWhoClicked();

        if (holder.getType() == EditorHolder.MenuType.REWARD_EDIT) {
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
                if (event.isShiftClick()) { event.setCancelled(true); player.sendMessage("§cTarik dan jatuhkan item manual ke slot 4!"); }
                return;
            }
            if (event.getRawSlot() == 4) {
                event.setCancelled(true);
                ItemStack cursorItem = event.getCursor();
                if (cursorItem != null && cursorItem.getType() != Material.AIR) {
                    SeriaCrate.getInstance().getRewardManager().updateRewardItem(holder.getCrateId(), holder.getTierId(), holder.getRewardIndex(), cursorItem.clone());
                    player.sendMessage("§aConfig bersih berhasil di-generate!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    EditorMenuManager.openRewardEdit(player, holder.getCrateId(), holder.getTierId(), holder.getRewardIndex());
                } else player.sendMessage("§cKamu harus memegang item di kursor (mouse) lalu klik kotak ini!");
                return;
            }
        } 
        else if (holder.getType() == EditorHolder.MenuType.REWARD_WIN_ITEMS) {
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(player.getInventory())) {
                if (event.isShiftClick()) { event.setCancelled(true); player.sendMessage("§cTarik itemnya manual (Drag & Drop) ke kotak atas."); }
                return;
            }
            if (event.getRawSlot() < 45) {
                if (event.getCursor() != null && event.getCursor().getType() != Material.AIR) {
                    event.setCancelled(true);
                    SeriaCrate.getInstance().getRewardManager().addRewardWinItem(holder.getCrateId(), holder.getTierId(), holder.getRewardIndex(), event.getCursor().clone());
                    player.sendMessage("§aItem fisik berhasil ditambahkan!");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    EditorMenuManager.openRewardWinItemsMenu(player, holder.getCrateId(), holder.getTierId(), holder.getRewardIndex());
                    return;
                }
            }
        }

        event.setCancelled(true); 
        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) return;
        String itemName = "";
        if (event.getCurrentItem().hasItemMeta() && event.getCurrentItem().getItemMeta().hasDisplayName()) {
            itemName = ChatColor.stripColor(event.getCurrentItem().getItemMeta().getDisplayName());
        }

        switch (holder.getType()) {
            case MAIN_MENU:
                if (itemName.contains("Buat Crate Baru")) {
                    player.closeInventory();
                    player.sendMessage("§eKetik nama Crate baru di chat.");
                    activePrompts.put(player.getUniqueId(), new ChatPrompt("CREATE_CRATE", null, null, -1));
                } else if (!itemName.isEmpty() && event.getRawSlot() < 45) {
                    EditorMenuManager.openCrateSettings(player, itemName.toLowerCase());
                }
                break;

            case CRATE_SETTINGS:
                File file = new File(SeriaCrate.getInstance().getConfigManager().getRewardsFolder(), holder.getCrateId() + ".yml");
                FileConfiguration config = YamlConfiguration.loadConfiguration(file);
                
                if (itemName.contains("Tipe Crate")) {
                    config.set("crate-settings.is-temporary", !config.getBoolean("crate-settings.is-temporary", false));
                    save(config, file); EditorMenuManager.openCrateSettings(player, holder.getCrateId());
                }
                else if (itemName.contains("Durasi")) {
                    player.closeInventory(); player.sendMessage("§eKetik durasi timer baru (dalam detik).");
                    activePrompts.put(player.getUniqueId(), new ChatPrompt("EDIT_DURATION", holder.getCrateId(), null, -1));
                }
                else if (itemName.contains("Rewards")) {
                    EditorMenuManager.openTierSelection(player, holder.getCrateId());
                }
                else if (itemName.equals("Hologram")) { // <-- UBAH BAGIAN INI MENJADI .equals
                    boolean currentHolo = config.getBoolean("crate-settings.hologram", true);
                    config.set("crate-settings.hologram", !currentHolo); 
                    save(config, file);
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
                    SeriaCrate.getInstance().getLocationManager().loadLocations(); 
                    EditorMenuManager.openCrateSettings(player, holder.getCrateId());
                }
                else if (itemName.contains("Blok Fisik")) {
                    EditorMenuManager.openBlockEditor(player, holder.getCrateId(), 0); 
                } 
                else if (itemName.contains("Biaya Resin")) {
                    player.closeInventory();
                    player.sendMessage("§eKetik jumlah Biaya Resin untuk crate ini di chat (ketik 0 untuk gratis):");
                    activePrompts.put(player.getUniqueId(), new ChatPrompt("EDIT_RESIN_COST", holder.getCrateId(), null, -1));
                }
                // --- AWAL KODE BARU ---
                else if (itemName.contains("Tinggi Teks Hologram")) {
                    player.closeInventory();
                    player.sendMessage("§eKetik tinggi Teks Hologram baru (gunakan titik, misal: 1.2):");
                    activePrompts.put(player.getUniqueId(), new ChatPrompt("EDIT_TEXT_OFFSET", holder.getCrateId(), null, -1));
                }
                else if (itemName.contains("Tinggi Item Melayang")) {
                    player.closeInventory();
                    player.sendMessage("§eKetik tinggi Item Melayang baru (gunakan titik, misal: 2.0):");
                    activePrompts.put(player.getUniqueId(), new ChatPrompt("EDIT_ITEM_OFFSET", holder.getCrateId(), null, -1));
                }
                // --- AKHIR KODE BARU ---
                else if (itemName.contains("Ambil Crate")) {
                    player.getInventory().addItem(id.seria.crate.util.ItemUtils.getCrateItem(holder.getCrateId()));
                } else if (itemName.contains("Kembali")) {
                    EditorMenuManager.openMainMenu(player);
                }
                break;

            case TIER_SELECTION:
                if (itemName.contains("Kembali")) EditorMenuManager.openCrateSettings(player, holder.getCrateId());
                else if (itemName.startsWith("TIER ")) EditorMenuManager.openRewardList(player, holder.getCrateId(), itemName.replace("TIER ", "").toLowerCase());
                break;

            case REWARD_LIST:
                if (itemName.contains("Kembali")) EditorMenuManager.openTierSelection(player, holder.getCrateId());
                else if (itemName.contains("Refresh") || event.getCurrentItem().getType() == Material.REPEATER) EditorMenuManager.openRewardList(player, holder.getCrateId(), holder.getTierId());
                else if (itemName.contains("Tambah Reward") || event.getCurrentItem().getType() == Material.LIME_STAINED_GLASS_PANE) {
                    int newIndex = SeriaCrate.getInstance().getRewardManager().createDefaultReward(holder.getCrateId(), holder.getTierId());
                    EditorMenuManager.openRewardEdit(player, holder.getCrateId(), holder.getTierId(), newIndex);
                } else if (event.getRawSlot() >= 9 && event.getRawSlot() <= 44) {
                    NamespacedKey idKey = new NamespacedKey(SeriaCrate.getInstance(), "gui_reward_id");
                    if (event.getCurrentItem().getItemMeta() != null && event.getCurrentItem().getItemMeta().getPersistentDataContainer().has(idKey, PersistentDataType.STRING)) {
                        int rewardIndex = Integer.parseInt(event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(idKey, PersistentDataType.STRING));
                        if (event.getClick() == ClickType.SHIFT_RIGHT) {
                            SeriaCrate.getInstance().getRewardManager().deleteReward(holder.getCrateId(), holder.getTierId(), rewardIndex);
                            EditorMenuManager.openRewardList(player, holder.getCrateId(), holder.getTierId());
                        } else if (event.getClick() == ClickType.LEFT) EditorMenuManager.openRewardEdit(player, holder.getCrateId(), holder.getTierId(), rewardIndex);
                    }
                }
                break;

            case REWARD_EDIT:
                if (event.getRawSlot() == 20) { 
                    EditorMenuManager.openRewardWinItemsMenu(player, holder.getCrateId(), holder.getTierId(), holder.getRewardIndex());
                } 
                else if (event.getRawSlot() == 21) { 
                    EditorMenuManager.openRewardCommandsMenu(player, holder.getCrateId(), holder.getTierId(), holder.getRewardIndex());
                } 
                else if (event.getRawSlot() == 22) { 
                    File f = new File(SeriaCrate.getInstance().getConfigManager().getRewardsFolder(), holder.getCrateId() + ".yml");
                    FileConfiguration c = YamlConfiguration.loadConfiguration(f);
                    String path = "tiers." + holder.getTierId() + "." + holder.getRewardIndex() + ".broadcast";
                    c.set(path, !c.getBoolean(path, false));
                    save(c, f); SeriaCrate.getInstance().getRewardManager().loadRewards();
                    EditorMenuManager.openRewardEdit(player, holder.getCrateId(), holder.getTierId(), holder.getRewardIndex());
                    player.playSound(player.getLocation(), org.bukkit.Sound.UI_BUTTON_CLICK, 1f, 1f);
                }
                else if (event.getRawSlot() == 23) { 
                    player.closeInventory(); player.sendMessage("§e[SeriaCrate] Ketik Peluang (Weight) baru:");
                    activePrompts.put(player.getUniqueId(), new ChatPrompt("EDIT_WEIGHT", holder.getCrateId(), holder.getTierId(), holder.getRewardIndex()));
                } 
                else if (event.getRawSlot() == 24) { 
                    player.closeInventory(); player.sendMessage("§e[SeriaCrate] Ketik Jumlah (Amount) UI baru:");
                    activePrompts.put(player.getUniqueId(), new ChatPrompt("EDIT_AMOUNT", holder.getCrateId(), holder.getTierId(), holder.getRewardIndex()));
                } 
                else if (event.getRawSlot() == 51 || itemName.contains("Kembali")) { 
                    EditorMenuManager.openRewardList(player, holder.getCrateId(), holder.getTierId());
                }
                break;

            case REWARD_WIN_ITEMS:
                if (itemName.contains("Kembali")) EditorMenuManager.openRewardEdit(player, holder.getCrateId(), holder.getTierId(), holder.getRewardIndex());
                else if (event.getRawSlot() < 45) {
                    NamespacedKey idxKey = new NamespacedKey(SeriaCrate.getInstance(), "gui_list_index");
                    if (event.getCurrentItem().getItemMeta() != null && event.getCurrentItem().getItemMeta().getPersistentDataContainer().has(idxKey, PersistentDataType.INTEGER)) {
                        if (event.getClick() == ClickType.SHIFT_RIGHT) {
                            int targetIdx = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(idxKey, PersistentDataType.INTEGER);
                            SeriaCrate.getInstance().getRewardManager().removeRewardWinItem(holder.getCrateId(), holder.getTierId(), holder.getRewardIndex(), targetIdx);
                            EditorMenuManager.openRewardWinItemsMenu(player, holder.getCrateId(), holder.getTierId(), holder.getRewardIndex());
                        }
                    }
                }
                break;

            case REWARD_COMMANDS:
                if (itemName.contains("Kembali")) EditorMenuManager.openRewardEdit(player, holder.getCrateId(), holder.getTierId(), holder.getRewardIndex());
                else if (itemName.contains("Tambah Command")) {
                    player.closeInventory(); player.sendMessage("§eKetik perintah console baru (tanpa /):");
                    activePrompts.put(player.getUniqueId(), new ChatPrompt("ADD_MULTI_COMMAND", holder.getCrateId(), holder.getTierId(), holder.getRewardIndex()));
                } else if (event.getRawSlot() < 45) {
                    NamespacedKey idxKey = new NamespacedKey(SeriaCrate.getInstance(), "gui_list_index");
                    if (event.getCurrentItem().getItemMeta() != null && event.getCurrentItem().getItemMeta().getPersistentDataContainer().has(idxKey, PersistentDataType.INTEGER)) {
                        if (event.getClick() == ClickType.LEFT) {
                            int targetIdx = event.getCurrentItem().getItemMeta().getPersistentDataContainer().get(idxKey, PersistentDataType.INTEGER);
                            SeriaCrate.getInstance().getRewardManager().removeRewardCommand(holder.getCrateId(), holder.getTierId(), holder.getRewardIndex(), targetIdx);
                            EditorMenuManager.openRewardCommandsMenu(player, holder.getCrateId(), holder.getTierId(), holder.getRewardIndex());
                        }
                    }
                }
                break;

            case BLOCK_EDITOR:
                if (itemName.contains("Kembali")) {
                    EditorMenuManager.openCrateSettings(player, holder.getCrateId());
                } 
                else if (itemName.contains("Halaman Sebelumnya")) {
                    EditorMenuManager.openBlockEditor(player, holder.getCrateId(), holder.getRewardIndex() - 1);
                } 
                else if (itemName.contains("Halaman Berikutnya")) {
                    EditorMenuManager.openBlockEditor(player, holder.getCrateId(), holder.getRewardIndex() + 1);
                } 
                else if (event.getRawSlot() < 45 && event.getCurrentItem() != null && event.getCurrentItem().getType() != Material.AIR) {
                    Material selectedBlock = event.getCurrentItem().getType();
                    File f = new File(SeriaCrate.getInstance().getConfigManager().getRewardsFolder(), holder.getCrateId() + ".yml");
                    FileConfiguration c = YamlConfiguration.loadConfiguration(f);
                    
                    c.set("crate-settings.block", selectedBlock.name());
                    save(c, f);
                    
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    player.sendMessage("§aWujud crate berhasil diubah menjadi " + selectedBlock.name());
                    EditorMenuManager.openCrateSettings(player, holder.getCrateId());
                }
                break;
        }
    }

    private void save(FileConfiguration config, File file) { try { config.save(file); } catch (IOException ignored) {} }

    @EventHandler
    public void onChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!activePrompts.containsKey(player.getUniqueId())) return;
        event.setCancelled(true);
        ChatPrompt prompt = activePrompts.remove(player.getUniqueId());
        
        Bukkit.getScheduler().runTask(SeriaCrate.getInstance(), () -> {
            String msg = event.getMessage();
            if (msg.equalsIgnoreCase("batal")) {
                player.sendMessage("§cDibatalkan.");
                if (prompt.type.contains("WEIGHT") || prompt.type.contains("AMOUNT") || prompt.type.contains("COMMAND")) {
                    EditorMenuManager.openRewardEdit(player, prompt.crateId, prompt.tierId, prompt.dataIndex);
                } else if (prompt.type.equals("EDIT_RESIN_COST") || prompt.type.equals("EDIT_DURATION")) {
                    EditorMenuManager.openCrateSettings(player, prompt.crateId);
                }
                return;
            }

            File f = new File(SeriaCrate.getInstance().getConfigManager().getRewardsFolder(), prompt.crateId + ".yml");
            FileConfiguration c = YamlConfiguration.loadConfiguration(f);

            if (prompt.type.equals("ADD_MULTI_COMMAND")) {
                SeriaCrate.getInstance().getRewardManager().addRewardCommand(prompt.crateId, prompt.tierId, prompt.dataIndex, msg);
                EditorMenuManager.openRewardCommandsMenu(player, prompt.crateId, prompt.tierId, prompt.dataIndex);
                player.sendMessage("§aCommand ditambahkan!");
                return;
            }

            if (prompt.type.equals("EDIT_TEXT_OFFSET") || prompt.type.equals("EDIT_ITEM_OFFSET")) {
                try {
                    double val = Double.parseDouble(msg);
                    if (prompt.type.equals("EDIT_TEXT_OFFSET")) {
                        c.set("crate-settings.text-offset", val);
                    } else {
                        c.set("crate-settings.item-offset", val);
                    }
                    save(c, f);
                    player.sendMessage("§aKetinggian berhasil diubah menjadi " + val);
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    
                    // Reload langsung lokasi agar tampilan hologram di depannya otomatis bergeser
                    SeriaCrate.getInstance().getLocationManager().loadLocations();
                    EditorMenuManager.openCrateSettings(player, prompt.crateId);
                } catch (NumberFormatException e) {
                    player.sendMessage("§cInput tidak valid! Harus berupa angka desimal menggunakan titik (contoh: 1.5).");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                    EditorMenuManager.openCrateSettings(player, prompt.crateId);
                }
                return; // Hentikan eksekusi di sini agar tidak error masuk ke parsing Integer
            }

            try {
                int number = Integer.parseInt(msg);
                
                if (prompt.type.equals("EDIT_DURATION")) {
                    c.set("crate-settings.duration", number); 
                    save(c, f);
                    player.sendMessage("§aDurasi berhasil diubah menjadi " + number + " detik.");
                    EditorMenuManager.openCrateSettings(player, prompt.crateId);
                } 
                // ==========================================
                // [TAMBAHAN BARU] Simpan Biaya Resin
                // ==========================================
                else if (prompt.type.equals("EDIT_RESIN_COST")) {
                    c.set("crate-settings.resin-cost", number); 
                    save(c, f);
                    player.sendMessage("§aBiaya Resin berhasil diatur menjadi " + number + ".");
                    player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
                    EditorMenuManager.openCrateSettings(player, prompt.crateId);
                }
                // ==========================================
                else if (prompt.type.equals("EDIT_WEIGHT")) {
                    c.set("tiers." + prompt.tierId + "." + prompt.dataIndex + ".weight", number); save(c, f);
                    SeriaCrate.getInstance().getRewardManager().loadRewards();
                    EditorMenuManager.openRewardEdit(player, prompt.crateId, prompt.tierId, prompt.dataIndex);
                } else if (prompt.type.equals("EDIT_AMOUNT")) {
                    c.set("tiers." + prompt.tierId + "." + prompt.dataIndex + ".amount", number); save(c, f);
                    SeriaCrate.getInstance().getRewardManager().loadRewards();
                    EditorMenuManager.openRewardEdit(player, prompt.crateId, prompt.tierId, prompt.dataIndex);
                }
            } catch (NumberFormatException e) {
                player.sendMessage("§cInput tidak valid! Harus berupa angka bulat.");
                player.playSound(player.getLocation(), org.bukkit.Sound.ENTITY_VILLAGER_NO, 1f, 1f);
                
                // Kembalikan ke menu yang sesuai jika input gagal
                if (prompt.type.contains("WEIGHT") || prompt.type.contains("AMOUNT")) {
                    EditorMenuManager.openRewardEdit(player, prompt.crateId, prompt.tierId, prompt.dataIndex);
                } else if (prompt.type.equals("EDIT_RESIN_COST") || prompt.type.equals("EDIT_DURATION")) {
                    EditorMenuManager.openCrateSettings(player, prompt.crateId);
                }
            }
        });
    }
}