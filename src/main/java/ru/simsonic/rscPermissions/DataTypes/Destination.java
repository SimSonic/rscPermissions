package ru.simsonic.rscPermissions.DataTypes;
import java.util.ArrayList;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bukkit.Location;
import org.bukkit.World;
import ru.simsonic.rscPermissions.Settings;
import ru.simsonic.utilities.LanguageUtility;

public class Destination
{
	private final String region;
	private final String world;
	private final String serverId;
	private Destination()
	{
		this.region   = null;
		this.world    = null;
		this.serverId = null;
	}
	private Destination(String region, String world, String serverId)
	{
		this.region   = region;
		this.world    = world;
		this.serverId = serverId;
	}
	public boolean isServerIdApplicable(String serverId)
	{
		return wildcardTest(serverId, this.serverId);
	}
	public boolean isLocationApplicable(Location location, Set<String> regions, String instantiator)
	{
		if(location != null)
		{
			if(location.getWorld() != null)
				if(isWorldApplicable(location.getWorld(), instantiator))
					return isRegionApplicable(regions, instantiator);
		} else {
			if(this.world == null)
				return isRegionApplicable(regions, instantiator);
		}
		return false;
	}
	public boolean isWorldApplicable(World world, String instantiator)
	{
		if(this.world == null || this.world.isEmpty() || "*".equals(this.world))
			return true;
		final String instantiated = (instantiator != null && !instantiator.isEmpty())
			? this.world.replace(Settings.instantiator, instantiator)
			: this.world;
		return wildcardTest(world.getName(), instantiated);
	}
	public boolean isWorldApplicable(String world, String instantiator)
	{
		if(this.world == null || this.world.isEmpty() || "*".equals(this.world))
			return true;
		final String instantiated = (instantiator != null && !instantiator.isEmpty())
			? this.world.replace(Settings.instantiator, instantiator)
			: this.world;
		return wildcardTest(world, instantiated);
	}
	public boolean isRegionApplicable(Set<String> regions, String instantiator)
	{
		if(this.region == null || "".equals(this.region) || "*".equals(this.region))
			return true;
		final String instantiated = (instantiator != null && !"".equals(instantiator))
			? this.region.replace(Settings.instantiator, instantiator)
			: this.region;
		for(String regionId : regions)
			if(wildcardTest(regionId, instantiated))
				return true;
		return false;
	}
	public boolean isRegionApplicable(String[] regions, String instantiator)
	{
		if(this.region == null || "".equals(this.region) || "*".equals(this.region))
			return true;
		final String instantiated = (instantiator != null && !"".equals(instantiator))
			? this.region.replace(Settings.instantiator, instantiator)
			: this.region;
		for(String regionId : regions)
			if(wildcardTest(regionId, instantiated))
				return true;
		return false;
	}
	private static boolean wildcardTest(String testing, String pattern)
	{
		if(pattern == null || "".equals(pattern))
			return true;
		if(testing == null || "".equals(testing))
			return false;
		return LanguageUtility.wildcardMatch(
			"<wildcard>" + testing.toLowerCase() + "</wildcard>",
			"<wildcard>" + pattern.toLowerCase() + "</wildcard>");
	}
	private static final String  destinationSplitter = "\\s*[;,\\r\\n]+\\s*";
	public static Destination[] parseDestinations(String destinations)
	{
		if(destinations == null || destinations.isEmpty())
			return new Destination[] { new Destination() };
		final String[] destinationsList = destinations.split(destinationSplitter);
		final ArrayList<Destination> result = new ArrayList(destinationsList.length);
		for(String inList : destinationsList)
			if(inList != null && !inList.isEmpty())
				result.add(parseDestination(inList));
		return result.toArray(new Destination[result.size()]);
	}
	private static final Pattern destinationPattern = Pattern.compile(
		"^(?:((?:\\w|\\*|\\?)*):)?((?:\\w|\\*|\\?)*)?(?:@((?:\\w|\\*|\\?)*))?$");
	private static Destination parseDestination(String destination)
	{
		final Matcher match = destinationPattern.matcher(destination);
		if(match.find())
		{
			final String groupR = match.group(1);
			final String groupW = match.group(2);
			final String groupS = match.group(3);
			final String region   = (groupR == null || "".equals(groupR)) ? "*" : groupR;
			final String world    = (groupW == null || "".equals(groupW)) ? "*" : groupW;
			final String serverId = (groupS == null || "".equals(groupS)) ? "*" : groupS;
			return new Destination(region, world, serverId);
		}
		return new Destination();
	}
}
