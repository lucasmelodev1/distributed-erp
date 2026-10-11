package com.example.demo.tenant.domain.authorization;

/** How far-reaching a permission is. Every new permission must declare one (the column is NOT NULL). */
public enum PermissionLevel {
	/** Reading data. */
	VIEW,
	/** Creating or changing data. */
	EDIT,
	/** Actions that are hard to undo or that remove access, such as expelling or inactivating. */
	SENSITIVE
}
