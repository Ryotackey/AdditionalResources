package net.ryotackey.additionalresources

import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.command.Command
import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.plugin.java.JavaPlugin
import java.util.*
import kotlin.collections.HashMap

class AdditionalResources : JavaPlugin(), Listener {

    val dropMap = HashMap<Material, HashMap<Double, ResourceInfo>>()

    var flag = true

    override fun onEnable() { // Plugin startup logic

        saveDefaultConfig()

        server.pluginManager.registerEvents(this, this)

        loadConfig()

    }

    override fun onDisable() { // Plugin shutdown logic
    }

    fun loadConfig(){

        reloadConfig()

        val config = config

        dropMap.clear()

        if (config.contains("enable"))flag = config.getBoolean("enable")

        for (key in config.getConfigurationSection("resources")!!.getKeys(false)){

            val m = Material.matchMaterial(key) ?: return

            val map = HashMap<Double, ResourceInfo>()

            for (i in config.getConfigurationSection("resources.$key")!!.getKeys(false)){

                val ri = ResourceInfo()

                val mate = Material.matchMaterial(config.getString("resources.$key.$i.type")!!) ?: return

                val item = ItemStack(mate)

                if (config.getString("resources.$key.$i.lore") != null){
                    val meta = item.itemMeta
                    meta.lore = config.getStringList("resources.$key.$i.lore")
                    item.itemMeta = meta
                }

                if (config.getString("resources.$key.$i.name") != null){
                    val meta = item.itemMeta
                    meta.setDisplayName(config.getString("resources.$key.$i.name")!!)
                    item.itemMeta = meta
                }

                val permission = config.getString("resources.$key.$i.permission")!!

                val chance = config.getString("resources.$key.$i.chance")!!.toDouble()

                        ri.item = item
                ri.permission = permission

                map[chance] = ri

            }

            dropMap[m] = map

        }

        Bukkit.getLogger().info(dropMap.toString())

    }

    @EventHandler
    fun breakBlock(e: BlockBreakEvent){

        if (!flag) return

        val b = e.block
        val p = e.player

        if (!dropMap.containsKey(b.type) || p.gameMode != GameMode.SURVIVAL){
            return
        }

        val list = dropMap[b.type]!!

        for (i in list){

            val r = Random().nextDouble()

            if (i.key < r) continue

            if (i.value.item == null || i.value.permission == null) continue

            if (!p.hasPermission(i.value.permission!!)) continue

            e.isDropItems = false

            b.location.world.dropItemNaturally(b.location, i.value.item!!)
        }

    }

    override fun onCommand(cs: CommandSender, command: Command, label: String, args: Array<out String>): Boolean {

        if (!cs.hasPermission("adr.op")) return true

        when(args.size){

            0->{
                sendHelp(cs)
                return true
            }
            1->{
                when(args[0]){

                    "on"->{
                        flag = true
                        config.set("enable", true)
                        cs.sendMessage("§aAdditionalResources turned on")
                        saveConfig()
                        return true
                    }

                    "off" ->{
                        flag = false
                        config.set("enable", false)
                        cs.sendMessage("§aAdditionalResources turned off")
                        saveConfig()
                        return true
                    }

                    "reload" ->{
                        loadConfig()
                        cs.sendMessage("§areloaded")
                        return true
                    }

                    "list" ->{
                        cs.sendMessage(dropMap.toString())
                    }
                }
            }
        }

        return false

    }

    fun sendHelp(cs: CommandSender){

        cs.sendMessage("§b[AdditionalResources]")
        cs.sendMessage("§6/adr on/off §f: enable/disable AdditionalResources")
        cs.sendMessage("§6/adr reload §f: reload config file")
        cs.sendMessage("§bVer: 1.0")

    }

    class ResourceInfo{

        var permission: String? = null
        var item: ItemStack? = null

    }

}