package br.com.purplemc.purpleesconde.utils;

import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;

import java.util.Arrays;
import java.util.List;

public class ItemUtils {

    public static ItemStack createItem(Material material, String displayName, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            if (displayName != null && !displayName.isEmpty()) {
                meta.setDisplayName(displayName);
            }
            if (lore != null && !lore.isEmpty()) {
                meta.setLore(lore);
            }
            item.setItemMeta(meta);
        }
        return item;
    }

    public static ItemStack createGUIItem(Material material, String displayName, List<String> lore) {
        return createItem(material, displayName, lore);
    }

    public static ItemStack[] getSeekerArmor() {
        ItemStack[] armor = new ItemStack[4];

        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta helmetMeta = (LeatherArmorMeta) helmet.getItemMeta();
        helmetMeta.setColor(Color.RED);
        helmetMeta.setDisplayName("§cCapacete do Procurador");
        helmet.setItemMeta(helmetMeta);

        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta chestMeta = (LeatherArmorMeta) chestplate.getItemMeta();
        chestMeta.setColor(Color.RED);
        chestMeta.setDisplayName("§cPeitoral do Procurador");
        chestplate.setItemMeta(chestMeta);

        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta legsMeta = (LeatherArmorMeta) leggings.getItemMeta();
        legsMeta.setColor(Color.RED);
        legsMeta.setDisplayName("§cCalças do Procurador");
        leggings.setItemMeta(legsMeta);

        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta bootsMeta = (LeatherArmorMeta) boots.getItemMeta();
        bootsMeta.setColor(Color.RED);
        bootsMeta.setDisplayName("§cBotas do Procurador");
        boots.setItemMeta(bootsMeta);

        armor[3] = helmet;
        armor[2] = chestplate;
        armor[1] = leggings;
        armor[0] = boots;

        return armor;
    }

    public static ItemStack[] getHiderArmor() {
        ItemStack[] armor = new ItemStack[4];

        ItemStack helmet = new ItemStack(Material.LEATHER_HELMET);
        LeatherArmorMeta helmetMeta = (LeatherArmorMeta) helmet.getItemMeta();
        helmetMeta.setColor(Color.GREEN);
        helmetMeta.setDisplayName("§aCapacete do Escondedor");
        helmet.setItemMeta(helmetMeta);

        ItemStack chestplate = new ItemStack(Material.LEATHER_CHESTPLATE);
        LeatherArmorMeta chestMeta = (LeatherArmorMeta) chestplate.getItemMeta();
        chestMeta.setColor(Color.GREEN);
        chestMeta.setDisplayName("§aPeitoral do Escondedor");
        chestplate.setItemMeta(chestMeta);

        ItemStack leggings = new ItemStack(Material.LEATHER_LEGGINGS);
        LeatherArmorMeta legsMeta = (LeatherArmorMeta) leggings.getItemMeta();
        legsMeta.setColor(Color.GREEN);
        legsMeta.setDisplayName("§aCalças do Escondedor");
        leggings.setItemMeta(legsMeta);

        ItemStack boots = new ItemStack(Material.LEATHER_BOOTS);
        LeatherArmorMeta bootsMeta = (LeatherArmorMeta) boots.getItemMeta();
        bootsMeta.setColor(Color.GREEN);
        bootsMeta.setDisplayName("§aBotas do Escondedor");
        boots.setItemMeta(bootsMeta);

        armor[3] = helmet;
        armor[2] = chestplate;
        armor[1] = leggings;
        armor[0] = boots;

        return armor;
    }

    public static ItemStack getWaitingBed() {
        return createItem(Material.BED, "§cSair da partida", Arrays.asList("§7Clique para sair da arena"));
    }

    public static ItemStack getSeekerWeapon() {
        ItemStack weapon = new ItemStack(Material.STICK);
        weapon.addUnsafeEnchantment(Enchantment.FIRE_ASPECT, 1);
        weapon.addUnsafeEnchantment(Enchantment.KNOCKBACK, 1);
        weapon.addUnsafeEnchantment(Enchantment.DAMAGE_ALL, 2);
        ItemMeta meta = weapon.getItemMeta();
        meta.setDisplayName("§cBastão do Procurador");
        meta.setLore(Arrays.asList("§7Arma especial do procurador"));
        weapon.setItemMeta(meta);
        return weapon;
    }

    public static ItemStack getHiderWeapon() {
        ItemStack weapon = new ItemStack(Material.STICK);
        ItemMeta meta = weapon.getItemMeta();
        meta.setDisplayName("§aBastão do Escondedor");
        meta.setLore(Arrays.asList("§7Para se defender"));
        weapon.setItemMeta(meta);
        return weapon;
    }

    public static ItemStack getFirework() {
        return createItem(Material.FIREWORK, "§eFogos de Artifício", Arrays.asList("§7Use para criar distrações!"));
    }

    public static ItemStack getLobbyJoinItem() {
        return createItem(Material.ENDER_PEARL, "§aJogar Esconde-Esconde", Arrays.asList("§7Clique para entrar em uma partida!"));
    }

    public static ItemStack getMapSelectorItem() {
        return createItem(Material.SIGN, "§eSelecionar Mapa", Arrays.asList("§7Escolha seu mapa favorito!"));
    }

    public static ItemStack getSpectatorCompass() {
        return createItem(Material.COMPASS, "§bTeleporte de Espectador", Arrays.asList("§7Clique para teleportar para jogadores"));
    }

    public static ItemStack getBackItem() {
        return createItem(Material.ARROW, "§cVoltar", null);
    }

    public static boolean isSpecialItem(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        String displayName = item.getItemMeta().getDisplayName();
        return displayName != null && (
                displayName.contains("Sair da partida") ||
                        displayName.contains("Jogar Esconde-Esconde") ||
                        displayName.contains("Selecionar Mapa") ||
                        displayName.contains("Teleporte de Espectador") ||
                        displayName.contains("Voltar")
        );
    }
}