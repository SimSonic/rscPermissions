package ru.simsonic.rscPermissions.API;

import java.sql.Timestamp;

public class RowPermission implements Cloneable
{
	public int         id;
	public String      splittedId;
	public String      entity;
	public EntityType  entityType;
	public String      permission;
	public boolean     value;
	public Destination destination;
	public int         expirience;
	public Timestamp   lifetime;
	public transient PlayerType playerType;
	public transient String     destinationSource;
	public transient RowEntity  entityObject;
	@Override
	public RowPermission clone() throws CloneNotSupportedException
	{
		 return (RowPermission)super.clone();
	}
}
