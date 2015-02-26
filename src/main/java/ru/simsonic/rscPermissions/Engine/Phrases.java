package ru.simsonic.rscPermissions.Engine;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import ru.simsonic.rscPermissions.API.TranslationProvider;
import ru.simsonic.rscPermissions.BukkitPluginMain;

public enum Phrases
{
	PLUGIN_ENABLED    ("generic.enabled"),
	PLUGIN_DISABLED   ("generic.disabled"),
	PLUGIN_METRICS    ("generic.metrics"),
	PLUGIN_RELOADED   ("generic.reloaded"),
	INTEGRATION_V_Y   ("integration.vault-yes"),
	INTEGRATION_V_N   ("integration.vault-no"),
	INTEGRATION_WG_Y  ("integration.worldguard-yes"),
	INTEGRATION_WG_N  ("integration.worldguard-no"),
	INTEGRATION_R_Y   ("integration.residence-yes"),
	INTEGRATION_R_N   ("integration.residence-no"),
	MYSQL_FETCHED     ("mysql.fetched"),
	;
	private final String node;
	private String phrase;
	private Phrases(String node)
	{
		this.node = node;
	}
	@Override
	public String toString()
	{
		return phrase;
	}
	public static void translate(TranslationProvider provider)
	{
		for(Phrases value : Phrases.values())
			value.phrase = provider.getString(value.node);
	}
	public static void extractAll(File workingDir)
	{
		extract(workingDir, "english");
		extract(workingDir, "russian");
	}
	public static void extract(File workingDir, String langName)
	{
		try
		{
			final File langFile = new File(workingDir, langName + ".yml");
			if(!langFile.isFile())
			{
				final FileChannel fileChannel = new FileOutputStream(langFile).getChannel();
				final InputStream langStream = BukkitPluginMain.class.getResourceAsStream("/languages/" + langName + ".yml");
				fileChannel.transferFrom(Channels.newChannel(langStream), 0, Long.MAX_VALUE);
			}
		} catch(IOException ex) {
		}
	}
}