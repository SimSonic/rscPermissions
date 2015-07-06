package ru.simsonic.rscPermissions.Engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import ru.simsonic.rscMinecraftLibrary.Bukkit.GenericChatCodes;
import ru.simsonic.rscPermissions.API.EntityType;
import ru.simsonic.rscPermissions.API.PlayerType;
import ru.simsonic.rscPermissions.API.RowEntity;
import ru.simsonic.rscPermissions.API.RowInheritance;
import ru.simsonic.rscPermissions.API.RowPermission;
import ru.simsonic.rscPermissions.API.Settings;
import ru.simsonic.rscPermissions.Engine.Backends.DatabaseContents;

public class InternalCache
{
	private final HashMap<String, RowEntity> entities_g = new HashMap<>();
	private final HashMap<String, RowEntity> entities_u = new HashMap<>();
	private final RowInheritance defaultInheritance     = new RowInheritance();
	private boolean alwaysInheritDefaultGroup           = false;
	private RowEntity implicit_g;
	private RowEntity implicit_u;
	public void setDefaultGroup(String defaultGroup, boolean alwaysInheritDefaultGroup)
	{
		defaultInheritance.parent = defaultGroup;
		defaultInheritance.deriveInstance();
		this.alwaysInheritDefaultGroup = alwaysInheritDefaultGroup;
	}
	public synchronized void fill(DatabaseContents contents)
	{
		clear();
		importEntities(contents);
		importPermissions(contents.permissions);
		importInheritance(contents.inheritance);
		implicit_g = entities_g.get("");
		implicit_u = entities_u.get("");
	}
	private void importEntities(DatabaseContents contents)
	{
		final HashSet<String> names_u = new HashSet<>();
		final HashSet<String> names_g = new HashSet<>();
		for(RowEntity row : contents.entities)
			if(row.entityType == EntityType.GROUP)
			{
				names_g.add(row.entity);
				entities_g.put(row.entity.toLowerCase(), row);
			} else {
				names_u.add(row.entity);
				entities_u.put(row.entity, row);
			}
		for(RowPermission row : contents.permissions)
			if(row.entityType == EntityType.GROUP)
				names_g.add(row.entity);
			else
				names_u.add(row.entity);
		for(RowInheritance row : contents.inheritance)
		{
			names_g.add(row.parent);
			if(row.childType == EntityType.GROUP)
				names_g.add(row.entity);
			else
				names_u.add(row.entity);
		}
		names_g.add(defaultInheritance.parent);
		for(String name : names_g)
		{
			final String groupInternalName = name.toLowerCase();
			if(!entities_g.containsKey(groupInternalName))
			{
				final RowEntity dummy = new RowEntity();
				dummy.entity     = name;
				dummy.entityType = EntityType.GROUP;
				entities_g.put(groupInternalName, dummy);
			}
		}
		for(String name : names_u)
			if(!entities_u.containsKey(name))
			{
				final RowEntity dummy = new RowEntity();
				dummy.entity     = name;
				dummy.entityType = EntityType.PLAYER;
				entities_u.put(name, dummy);
			}
		for(RowEntity row : entities_u.values())
			row.playerType = PlayerType.scanPlayerEntity(row.entity);
	}
	private void importPermissions(RowPermission[] rows)
	{
		final ArrayList<RowPermission> permissions_p2g = new ArrayList<>();
		final ArrayList<RowPermission> permissions_p2u = new ArrayList<>();
		for(RowPermission row : rows)
			if(row.entityType == EntityType.GROUP)
			{
				row.entityObject = entities_g.get(row.entity.toLowerCase());
				permissions_p2g.add(row);
			} else {
				row.entityObject = entities_u.get(row.entity);
				permissions_p2u.add(row);
			}
		for(String entry : entities_g.keySet())
		{
			final ArrayList<RowPermission> permissions = new ArrayList<>();
			for(RowPermission row : permissions_p2g)
				if(row.entity.toLowerCase().equals(entry))
					permissions.add(row);
			entities_g.get(entry).permissions = permissions.toArray(new RowPermission[permissions.size()]);
		}
		for(String entry : entities_u.keySet())
		{
			final ArrayList<RowPermission> permissions = new ArrayList<>();
			for(RowPermission row : permissions_p2u)
				if(row.entity.equals(entry))
					permissions.add(row);
			entities_u.get(entry).permissions = permissions.toArray(new RowPermission[permissions.size()]);
		}
	}
	private void importInheritance(RowInheritance[] rows)
	{
		final ArrayList<RowInheritance> inheritance_g2g = new ArrayList<>();
		final ArrayList<RowInheritance> inheritance_g2u = new ArrayList<>();
		for(RowInheritance row : rows)
			if(row.childType == EntityType.GROUP)
			{
				row.entityChild  = entities_g.get(row.entity.toLowerCase());
				row.entityParent = entities_g.get(row.parent.toLowerCase());
				inheritance_g2g.add(row);
			} else {
				row.entityChild  = entities_u.get(row.entity);
				row.entityParent = entities_g.get(row.parent.toLowerCase());
				inheritance_g2u.add(row);
			}
		for(Entry<String, RowEntity> entry : entities_g.entrySet())
		{
			final ArrayList<RowInheritance> inheritances = new ArrayList<>();
			final String name = entry.getKey();
			for(RowInheritance row : inheritance_g2g)
				if(row.entity.toLowerCase().equals(name))
					inheritances.add(row);
			Collections.sort(inheritances);
			entry.getValue().inheritance = inheritances.toArray(new RowInheritance[inheritances.size()]);
		}
		for(Entry<String, RowEntity> entry : entities_u.entrySet())
		{
			final ArrayList<RowInheritance> inheritances = new ArrayList<>();
			final String name = entry.getKey();
			for(RowInheritance row : inheritance_g2u)
				if(row.entity.equals(name))
					inheritances.add(row);
			Collections.sort(inheritances);
			entry.getValue().inheritance = inheritances.toArray(new RowInheritance[inheritances.size()]);
		}
		defaultInheritance.childType = EntityType.PLAYER;
		defaultInheritance.entityParent = entities_g.get(defaultInheritance.parent.toLowerCase());
	}
	public synchronized ResolutionResult resolvePlayer(String player)
	{
		return resolvePlayer(new String[] { player });
	}
	public synchronized ResolutionResult resolvePlayer(String[] player)
	{
		final ResolutionParams params = new ResolutionParams();
		params.applicableIdentifiers = player;
		params.destRegions = new String[] {};
		return resolvePlayer(params);
	}
	public synchronized ResolutionResult resolvePlayer(ResolutionParams params)
	{
		final ArrayList<RowEntity>      applicableEntities    = new ArrayList<>();
		final ArrayList<RowPermission>  applicablePermissions = new ArrayList<>();
		final ArrayList<RowInheritance> applicableInheritance = new ArrayList<>();
		if(implicit_u != null && implicit_u.permissions != null)
			processPermissions(params, Arrays.asList(implicit_u.permissions));
		params.groupList    = new LinkedList<>();
		params.finalPerms   = new HashMap<>();
		params.instantiator = "";
		params.depth        = 0;
		for(RowEntity row : entities_u.values())
			for(String identifier : params.applicableIdentifiers)
				if(row.playerType.isEntityApplicable(row.entity, identifier))
				{
					// Apply direct inheritance
					if(row.inheritance != null && row.inheritance.length > 0)
						for(RowInheritance inheritance : row.inheritance)
							if(isInheritanceApplicable(params, inheritance))
								applicableInheritance.add(inheritance);
					// Apply direct permissions
					applicablePermissions.addAll(Arrays.asList(row.permissions));
					// Apply direct prefixes and suffixes
					applicableEntities.add(row);
				}
		Collections.sort(applicableEntities);
		final ArrayList<ResolutionResult> intermediateResults = new ArrayList<>();
		Collections.sort(applicableInheritance);
		// Mix into default inheritance
		if(applicableInheritance.isEmpty() || alwaysInheritDefaultGroup)
			applicableInheritance.add(0, defaultInheritance);
		for(RowInheritance row : applicableInheritance)
		{
			params.instantiator = "";
			params.parentEntity = row.entityParent;
			intermediateResults.add(resolveParent(params));
		}
		// Process all applicable prefixes using only entity id order
		ResolutionResult result = processPrefixesAndSuffixes(params, intermediateResults);
		for(RowEntity row : applicableEntities)
		{
			params.instantiator = "";
			params.parentEntity = row;
			result = processPrefixesAndSuffixes(params, Arrays.asList(new ResolutionResult[] { result }));
		}
		result.prefix = GenericChatCodes.processStringStatic(result.prefix);
		result.suffix = GenericChatCodes.processStringStatic(result.suffix);
		processPermissions(params, applicablePermissions);
		result.permissions = params.finalPerms;
		result.groups = params.groupList;
		return result;
	}
	private ResolutionResult resolveParent(ResolutionParams params)
	{
		if(implicit_g != null && implicit_g.permissions != null)
			processPermissions(params, Arrays.asList(implicit_g.permissions));
		final RowEntity currentParent = params.parentEntity;
		final String instantiator = params.instantiator;
		final ArrayList<ResolutionResult> intermediateResults = new ArrayList<>();
		params.depth += 1;
		for(RowInheritance row : params.parentEntity.inheritance)
			if(isInheritanceApplicable(params, row))
			{
				params.parentEntity = row.entityParent;
				params.instantiator = (row.instance != null && !"".equals(row.instance))
					? row.instance
					: instantiator;
				intermediateResults.add(resolveParent(params));
			}
		params.depth -= 1;
		params.groupList.add(depthPrefix(params.depth) + currentParent.entity
			+ ("".equals(instantiator) ? "" : Settings.separator + instantiator));
		// Prefixes and suffixes
		params.parentEntity = currentParent;
		params.instantiator = instantiator;
		final ResolutionResult result = processPrefixesAndSuffixes(params, intermediateResults);
		intermediateResults.clear();
		// Permissions
 		if(currentParent.permissions != null)
  			processPermissions(params, Arrays.asList(currentParent.permissions));
		return result;
	}
	private String depthPrefix(int depth)
	{
		if(depth > 0)
		{
			final char[] levelParent = new char[depth];
			levelParent[depth - 1] = Settings.groupLevelTab;
			return new String(levelParent).replace('\0', ' ');
		}
		return "";
	}
	private ResolutionResult processPrefixesAndSuffixes(ResolutionParams params, List<ResolutionResult> intermediate)
	{
		final ResolutionResult result = new ResolutionResult();
		result.prefix = params.parentEntity.prefix;
		result.suffix = params.parentEntity.suffix;
		if(result.prefix == null || "".equals(result.prefix))
			result.prefix = "%";
		if(result.suffix == null || "".equals(result.suffix))
			result.suffix = "%";
		final StringBuilder sbp = new StringBuilder();
		final StringBuilder sbs = new StringBuilder();
		for(ResolutionResult inherited : intermediate)
		{
			if(inherited.prefix != null)
				sbp.append(inherited.prefix);
			if(inherited.suffix != null)
				sbs.append(inherited.suffix);
		}
		result.prefix = result.prefix.replace(Settings.textInheriter, sbp.toString());
		result.suffix = result.suffix.replace(Settings.textInheriter, sbs.toString());
		result.prefix = result.prefix.replace(Settings.instantiator, params.instantiator);
		result.suffix = result.suffix.replace(Settings.instantiator, params.instantiator);
		return result;
	}
	private void processPermissions(ResolutionParams params, List<RowPermission> permissions)
	{
		for(RowPermission row : permissions)
			if(isPermissionApplicable(params, row))
				params.finalPerms.put(
					row.permission.replace(Settings.instantiator, params.instantiator),
					row.value);
	}
	private boolean isPermissionApplicable(ResolutionParams params, RowPermission row)
	{
		if(params.expirience < row.expirience)
			return false;
		return row.destination.isWorldApplicable(params.destWorld, params.instantiator)
			? row.destination.isRegionApplicable(params.destRegions, params.instantiator)
			: false;
	}
	private boolean isInheritanceApplicable(ResolutionParams params, RowInheritance row)
	{
		if(params.expirience < row.expirience)
			return false;
		return row.destination.isWorldApplicable(params.destWorld, params.instantiator)
			? row.destination.isRegionApplicable(params.destRegions, params.instantiator)
			: false;
	}
	public synchronized RowEntity getGroup(String group)
	{
		if(group != null && !"".equals(group))
		{
			final RowEntity row = entities_g.get(group.toLowerCase());
			if(row != null)
				return row;
		}
		return new RowEntity();
	}
	public synchronized Set<String> getGroups()
	{
		final HashSet<String> result = new HashSet<>(entities_g.size());
		for(RowEntity row : entities_g.values())
			result.add(row.entity);
		return result;
	}
	public synchronized void clear()
	{
		entities_g.clear();
		entities_u.clear();
		implicit_g = null;
		implicit_u = null;
	}
}
