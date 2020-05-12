package me.SuperPyroManiac.GPR;

import me.SuperPyroManiac.GPR.feature.BroadcastNewListings;
import me.SuperPyroManiac.GPR.feature.MailSellerOnSale;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.logging.Logger;

public class GPRealEstate extends JavaPlugin
{

    public static boolean    vaultPresent = false;
    public static Economy    econ         = null;
    public static Permission perms        = null;
    Logger    log;
    DataStore dataStore;

    public void onEnable()
    {
        this.log = getLogger();
        this.dataStore = new DataStore(this);

        new GPREListener(this).registerEvents();
        if (checkVault())
        {
            this.log.info("Vault has been detected and enabled.");
            if (setupEconomy())
            {
                this.log.info("Vault is using " + econ.getName() + " as the economy plugin.");
            }
            else
            {
                this.log.warning("No compatible economy plugin detected [Vault].");
                this.log.warning("Disabling plugin.");
                getPluginLoader().disablePlugin(this);
                return;
            }
            if (setupPermissions())
            {
                this.log.info("Vault is using " + perms.getName() + " for the permissions.");
            }
            else
            {
                this.log.warning("No compatible permissions plugin detected [Vault].");
                this.log.warning("Disabling plugin.");
                getPluginLoader().disablePlugin(this);
                return;
            }
        }
        loadConfig(false);
        new MailSellerOnSale(this);
        new BroadcastNewListings(this, econ);
    }

    public boolean onCommand(CommandSender sender, Command command, String label, String[] args)
    {
        if ((command.getName().equalsIgnoreCase("gpre")) && (sender.hasPermission("gprealestate.command")))
        {
            if (args.length == 0)
            {
                sender.sendMessage(this.dataStore.chatPrefix + ChatColor.GREEN + "Unknown. Use 'gpre help' for info.");
                return true;
            }
            if (args.length == 1)
            {
                if ((args[0].equalsIgnoreCase("version")) && (sender.hasPermission("gprealestate.admin")))
                {
                    sender.sendMessage(this.dataStore.chatPrefix + ChatColor.GREEN + "You are running " + ChatColor.RED + this.dataStore.pdf.getName() + ChatColor.GREEN + " version " + ChatColor.RED + this.dataStore.pdf.getVersion());
                    return true;
                }
                if ((args[0].equalsIgnoreCase("reload")) && (sender.hasPermission("gprealestate.admin")))
                {
                    loadConfig(true);
                    sender.sendMessage(this.dataStore.chatPrefix + ChatColor.GREEN + "The config file was succesfully reloaded.");
                    return true;
                }
                if (args[0].equalsIgnoreCase("help"))
                {
                    sender.sendMessage(this.dataStore.chatPrefix + ChatColor.GREEN + "Commands: -Permission");
                    sender.sendMessage(this.dataStore.chatPrefix + ChatColor.GREEN + "gpre version | -gprealestate.admin");
                    sender.sendMessage(this.dataStore.chatPrefix + ChatColor.GREEN + "gpre reload: | -gprealestate.admin");

                    return true;
                }
                sender.sendMessage(this.dataStore.chatPrefix + ChatColor.GREEN + "Unknown. Use 'gpre help' for info");
                return true;
            }
        }
        else
        {
            sender.sendMessage(ChatColor.RED + "You do not have permissions to use this command.");
            return false;
        }
        return false;
    }

    public DataStore getDataStore()
    {
        return dataStore;
    }

    private void loadConfig(boolean reload)
    {
        FileConfiguration config = YamlConfiguration.loadConfiguration(new File(this.dataStore.configFilePath));
        FileConfiguration outConfig = new YamlConfiguration();

        this.dataStore.cfgSignShort = config.getString("GPRealEstate.Keywords.Signs.Short", "[RE]");
        this.dataStore.cfgSignLong = config.getString("GPRealEstate.Keywords.Signs.Long", "[RealEstate]");

        this.dataStore.cfgRentKeywords = this.dataStore.stringToList(config.getString("GPRealEstate.Keywords.Actions.Renting", "Rent;Renting;Rental;For Rent"));
        this.dataStore.cfgSellKeywords = this.dataStore.stringToList(config.getString("GPRealEstate.Keywords.Actions.Selling", "Sell;Selling;For Sale"));

        this.dataStore.cfgReplaceRent = config.getString("GPRealEstate.Keywords.Actions.ReplaceRent", "FOR LEASE");
        this.dataStore.cfgReplaceSell = config.getString("GPRealEstate.Keywords.Actions.ReplaceSell", "FOR SALE");
        this.dataStore.cfgReplaceValue = config.getInt("GPRealEstate.Keywords.Actions.BuyPrice", 5);

        this.dataStore.cfgEnableLeasing = config.getBoolean("GPRealEstate.Rules.EnableLeasing", false);
        this.dataStore.cfgAllowSellingParentAC = config.getBoolean("GPRealEstate.Rules.AllowSellingParentAC", false);
        this.dataStore.cfgIgnoreClaimSize = config.getBoolean("GPRealEstate.Rules.IgnoreSizeLimit", false);
        this.dataStore.cfgTransferClaimBlocks = config.getBoolean("GPRealEstate.Rules.TransferClaimBlocks", true);

        this.dataStore.mailSeler = config.getString("GPRealEstate.messages.mail-seller-on-sale", "Your claim ({location}) has been bought by {buyer} for {price}");
        this.dataStore.invalidSign = config.getString("GPRealEstate.messages.invalid-sign", "Invalid sign");
        this.dataStore.sellingClaim = config.getString("GPRealEstate.messages.selling-claim", "You are now selling this claim for {price}{currency}");
        this.dataStore.sellingAdminClaim = config.getString("GPRealEstate.messages.selling-admin-claim", "You are now selling this admin claim for {price}{currency}");
        this.dataStore.cantSellOthersClaims = config.getString("GPRealEstate.messages.cant-sell-others-claims", "You can't sell others claims");
        this.dataStore.sellingAdminSubClaim = config.getString("GPRealEstate.messages.selling-admin-subclaim", "Selling admin claim for {price}{currency}");
        this.dataStore.sellingSubClaim = config.getString("GPRealEstate.messages.selling-subclaim", "Selling subclaim for {price}{currency}");
        this.dataStore.cantSellSubClaims = config.getString("GPRealEstate.messages.cant-sell-subclaims", "You cant sell subclaims");
        this.dataStore.unclaimedSign = config.getString("GPRealEstate.messages.unclaimed-sign", "This sign is not in a claim");
        this.dataStore.purchasedAdminClaim = config.getString("GPRealEstate.messages.purchased-admin-claim", "Successfully purchased admin claim");
        this.dataStore.cantSellOwnClaim = config.getString("GPRealEstate.messages.cant-sell-own-claim", "You cant buy your own claim");
        this.dataStore.cantBuySubClaims = config.getString("GPRealEstate.messages.cant-buy-subclaims", "You cant buy subclaims");
        this.dataStore.misplacedSign = config.getString("GPRealEstate.messages.misplaced-sign", "Missplaced sign, removing..");
        this.dataStore.purchasedSubClaim = config.getString("GPRealEstate.messages.purchased-subclaim", "Successfully purcahsed subclaim");
        this.dataStore.purchasedClaim = config.getString("GPRealEstate.messages.purchased-claim", "Successfully purchased claim");
        this.dataStore.alreadyOwnedClaim = config.getString("GPRealEstate.messages.already-owns-claim", "You already own this claim");
        this.dataStore.cantPurchaseClaim = config.getString("GPRealEstate.messages.cant-purchase-claim", "Cant purchase claim");
        this.dataStore.insufficientClaimBlocks = config.getString("GPRealEstate.messages.insufficient-claim-blocks", "Insufficient claim blocks");
        this.dataStore.missingBuyPerms = config.getString("GPRealEstate.messages.missing-buy-perms", "Missing buy perms");
        if (!reload)
        {
            this.log.info("Signs will be using the keywords \"" + this.dataStore.cfgSignShort + "\" or \"" + this.dataStore.cfgSignLong + "\"");
        }
        outConfig.set("GPRealEstate.Keywords.Signs.Short", this.dataStore.cfgSignShort);
        outConfig.set("GPRealEstate.Keywords.Signs.Long", this.dataStore.cfgSignLong);
        outConfig.set("GPRealEstate.Keywords.Actions.Renting", this.dataStore.listToString(this.dataStore.cfgRentKeywords));
        outConfig.set("GPRealEstate.Keywords.Actions.Selling", this.dataStore.listToString(this.dataStore.cfgSellKeywords));
        outConfig.set("GPRealEstate.Keywords.Actions.ReplaceRent", this.dataStore.cfgReplaceRent);
        outConfig.set("GPRealEstate.Keywords.Actions.ReplaceSell", this.dataStore.cfgReplaceSell);
        outConfig.set("GPRealEstate.Keywords.Actions.BuyPrice", Integer.valueOf(this.dataStore.cfgReplaceValue));
        outConfig.set("GPRealEstate.Rules.EnableLeasing", Boolean.valueOf(this.dataStore.cfgEnableLeasing));
        outConfig.set("GPRealEstate.Rules.IgnoreSizeLimit", Boolean.valueOf(this.dataStore.cfgIgnoreClaimSize));
        outConfig.set("GPRealEstate.Rules.AllowSellingParentAC", Boolean.valueOf(this.dataStore.cfgAllowSellingParentAC));
        outConfig.set("GPRealEstate.Rules.TransferClaimBlocks", this.dataStore.cfgTransferClaimBlocks);

        outConfig.set("GPRealEstate.messages.mail-seller-on-sale", this.dataStore.mailSeler);
        outConfig.set("GPRealEstate.messages.invalid-sign", this.dataStore.invalidSign);
        outConfig.set("GPRealEstate.messages.selling-claim", this.dataStore.sellingClaim);
        outConfig.set("GPRealEstate.messages.selling-admin-claim", this.dataStore.sellingAdminClaim);
        outConfig.set("GPRealEstate.messages.cant-sell-others-claims", this.dataStore.cantSellOthersClaims);
        outConfig.set("GPRealEstate.messages.selling-admin-subclaim", this.dataStore.sellingAdminSubClaim);
        outConfig.set("GPRealEstate.messages.selling-subclaim", this.dataStore.sellingSubClaim);
        outConfig.set("GPRealEstate.messages.cant-sell-subclaims", this.dataStore.cantSellSubClaims);
        outConfig.set("GPRealEstate.messages.unclaimed-sign", this.dataStore.unclaimedSign);
        outConfig.set("GPRealEstate.messages.purchased-admin-claim", this.dataStore.purchasedAdminClaim);
        outConfig.set("GPRealEstate.messages.cant-sell-own-claim", this.dataStore.cantSellOwnClaim);
        outConfig.set("GPRealEstate.messages.cant-buy-subclaims", this.dataStore.cantBuySubClaims);
        outConfig.set("GPRealEstate.messages.misplaced-sign", this.dataStore.misplacedSign);
        outConfig.set("GPRealEstate.messages.purchased-subclaim", this.dataStore.purchasedSubClaim);
        outConfig.set("GPRealEstate.messages.purchased-claim", this.dataStore.purchasedClaim);
        outConfig.set("GPRealEstate.messages.already-owns-claim", this.dataStore.alreadyOwnedClaim);
        outConfig.set("GPRealEstate.messages.cant-purchase-claim", this.dataStore.cantPurchaseClaim);
        outConfig.set("GPRealEstate.messages.insufficient-claim-blocks", this.dataStore.insufficientClaimBlocks);
        outConfig.set("GPRealEstate.messages.missing-buy-perms", this.dataStore.missingBuyPerms);
        try
        {
            outConfig.save(this.dataStore.configFilePath);
        }
        catch (IOException exception)
        {
            this.log.info("Unable to write to the configuration file at \"" + this.dataStore.configFilePath + "\"");
        }
    }

    public void addLogEntry(String entry)
    {
        try
        {
            File logFile = new File(this.dataStore.logFilePath);
            if (!logFile.exists())
            {
                logFile.createNewFile();
            }
            FileWriter fw = new FileWriter(logFile, true);
            PrintWriter pw = new PrintWriter(fw);

            pw.println(entry);
            pw.flush();
            pw.close();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
    }

    private boolean checkVault()
    {
        vaultPresent = getServer().getPluginManager().getPlugin("Vault") != null;
        return vaultPresent;
    }

    private boolean setupEconomy()
    {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null)
        {
            return false;
        }
        econ = (Economy) rsp.getProvider();
        return econ != null;
    }

    private boolean setupPermissions()
    {
        RegisteredServiceProvider<Permission> rsp = getServer().getServicesManager().getRegistration(Permission.class);
        perms = (Permission) rsp.getProvider();
        return perms != null;
    }

}
