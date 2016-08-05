package ru.simsonic.rscPermissions.Bukkit.Commands;

import ru.simsonic.rscCommonsLibrary.TimeIntervalParser;
import ru.simsonic.rscMinecraftLibrary.Bukkit.CommandAnswerException;
import ru.simsonic.rscPermissions.API.Destination;

public class ArgumentUtilities
{
	public static class CommandParams
	{
		public Integer     expirience;
		public Destination destination;
		public Integer     lifetime;
	}
	public static CommandParams parseCommandParams(String[] args) throws CommandAnswerException
	{
		// /rscp <target> <action> <object> [params]
		final CommandParams result = new CommandParams();
		for(int index = 0; index < args.length && args[index] != null; index += 1)
		{
			final boolean isLastArg = (index == args.length - 1);
			switch(args[index].toLowerCase())
			{
				case "":
					break;
				case "e":
				case "exp":
				case "expirience":
					if(isLastArg)
						throw new CommandAnswerException("RequiresOneMoreArg");
					try
					{
						index += 1;
						result.expirience = Integer.parseInt(args[index]);
					} catch(NumberFormatException ex) {
						throw new CommandAnswerException("NumberFormatException: " + args[index]);
					} catch(NullPointerException ex) {
						throw new CommandAnswerException("NullPointerException: args #" + index);
					}
					break;
				case "d":
				case "dest":
				case "destination":
					if(isLastArg)
						throw new CommandAnswerException("RequiresOneMoreArg");
					index += 1;
					if(args[index] == null || "".equals(args[index]))
						throw new CommandAnswerException("NotEnoughArguments: args #" + index);
					result.destination = Destination.parseDestination(args[index]);
					break;
				case "l":
				case "lifetime":
					if(isLastArg)
						throw new CommandAnswerException("RequiresOneMoreArg");
					index += 1;
					result.lifetime = TimeIntervalParser.parseTimeInterval(args[index]);
					if(result.lifetime < 0)
						result.lifetime = Integer.MAX_VALUE;
					break;
				default:
					throw new CommandAnswerException("UnknownCommandParam: " + args[index]);
			}
		}
		return result;
	}
	public static boolean argumentToBoolean(String arg, Boolean prevForToggle) throws IllegalArgumentException
	{
		if(arg == null || "".equals(arg))
			throw new IllegalArgumentException("Argument is null or empty.");
		switch(arg.toLowerCase())
		{
			case "enable":
			case "true":
			case "yes":
			case "on":
			case "1":
				return true;
			case "disable":
			case "false":
			case "no":
			case "off":
			case "0":
				return false;
			case "toggle":
				if(prevForToggle != null)
					return !prevForToggle;
				else
					throw new IllegalArgumentException("Previous value is unknown.");
		}
		throw new IllegalArgumentException("Cannot understand boolean value.");
	}
	public static int argumentToInteger(String arg) throws IllegalArgumentException
	{
		if(arg == null || "".equals(arg))
			throw new IllegalArgumentException("Argument is null or empty.");
		try
		{
			return Integer.parseInt(arg);
		} catch(NumberFormatException ex) {
			throw new IllegalArgumentException(ex.getMessage());
		}
	}
}
