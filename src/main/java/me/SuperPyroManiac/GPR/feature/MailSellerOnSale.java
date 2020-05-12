package me.SuperPyroManiac.GPR.feature;

import me.SuperPyroManiac.GPR.GPRealEstate;
import me.SuperPyroManiac.GPR.events.GPRSaleEvent;
import me.SuperPyroManiac.GPR.util.Color;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Created on 3/11/2020.
 * A class for each method? I guess
 * @author RoboMWM
 */
public class MailSellerOnSale implements Listener
{
    private final GPRealEstate plugin;
    public MailSellerOnSale(final GPRealEstate plugin)
    {
        this.plugin = plugin;
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
    }

    @EventHandler
    public void onSale(GPRSaleEvent event)
    {
        //ignore admin claim sales
        if (event.getClaim().ownerID == null)
            return;

        final String owner = event.getClaim().getOwnerName();
        final String buyer = event.getBuyer().getName();
        final String location = getfriendlyLocationString(event.getClaim().getLesserBoundaryCorner());
        final double price = event.getPrice();

        final String message = plugin.getDataStore().mailSeler.replace("{buyer}", buyer).replace("{owner}", owner).replace("{location}", location).replace("{price}", String.valueOf(price));
        plugin.getServer().dispatchCommand(plugin.getServer().getConsoleSender(), Color.colorize("mail send " + owner + " " + message));
    }

    public static String getfriendlyLocationString(Location location)
    {
        return location.getWorld().getName() + ": x" + location.getBlockX() + ", z" + location.getBlockZ();
    }

}
