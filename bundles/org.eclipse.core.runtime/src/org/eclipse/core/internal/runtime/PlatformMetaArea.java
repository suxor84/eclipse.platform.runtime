package org.eclipse.core.internal.runtime;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import org.eclipse.core.runtime.*;
import java.util.*;
import java.io.*;

public class PlatformMetaArea {
	IPath location;
	
	/* package */ static final String F_DESCRIPTION = ".platform";
	/* package */ static final String F_META_AREA = ".metadata";
	/* package */ static final String F_PLUGIN_PATH = ".plugin-path";
	/* package */ static final String F_PLUGIN_DATA = ".plugins";
	/* package */ static final String F_REGISTRY = ".registry";
	/* package */ static final String F_SNAP = ".snap";
	/* package */ static final String F_LOG = ".log";
	/* package */ static final String F_BACKUP = ".bak";
	/* package */ static final String F_OPTIONS = ".options";	
	/* package */ static final String F_KEYRING = ".keyring";	
	/* package */ static final String F_LOCK_FILE = ".lock";
	
/**
 * 
 */
public PlatformMetaArea(IPath location) {
	super();
	this.location = location;
}
private Properties buildPathProperties(Hashtable paths) {
	Properties result = new Properties();
	for (Enumeration keys = paths.keys(); keys.hasMoreElements();) {
		String key = (String) keys.nextElement();
		StringBuffer entry = new StringBuffer(100);
		IPath[] list = (IPath[]) paths.get(key);
		for (int i = 0; i < list.length; i++) {
			entry.append(list[i].toOSString());
			entry.append(";");
		}
		result.put(key, entry.toString());
	}
	return result;
}
/**
 * 
 */
public void createLocation() throws CoreException {
	File file = getLocation().toFile();
	try {
		file.mkdirs();
	} catch (Exception e) {
		String message = Policy.bind("meta.couldNotCreate", file.getAbsolutePath());
		throw new CoreException(new Status(IStatus.ERROR, Platform.PI_RUNTIME, Platform.FAILED_WRITE_METADATA, message, e));
	}
	if (!file.canWrite()) {
		String message = Policy.bind("meta.readonly", file.getAbsolutePath());
		throw new CoreException(new Status(IStatus.ERROR, Platform.PI_RUNTIME, Platform.FAILED_WRITE_METADATA, message, null));
	}
}
public IPath getBackupFilePathFor(IPath file) {
	return file.removeLastSegments(1).append(file.lastSegment() + F_BACKUP);
}
/**
 * Returns the location of the platform's meta area.
 */
public IPath getLocation() {
	return location.append(F_META_AREA);
}
/**
 * 
 */
public IPath getLogLocation() {
	return getLocation().append(F_LOG);
}
/**
 * 
 */
public IPath getOptionsLocation() {
	return getLocation().append(F_OPTIONS);
}
/**
 * Returns the read/write location in which the given plugin can manage
 * private state.  
 */
public IPath getPluginStateLocation(Plugin plugin) {
	IPath result = getLocation().append(F_PLUGIN_DATA);
	String id = plugin.getDescriptor().getUniqueIdentifier();
	return result.append(id);
}
/**
 * 
 */
public IPath getRegistryPath() {
	return getLocation().append(F_REGISTRY);
}
/**
 * 
 */
public IPath getSnapshotPath() {
	return getLocation().append(F_SNAP);
}
private Hashtable parsePathProperties(Properties props) {
	Hashtable result = new Hashtable(5);
	for (Enumeration keys = props.propertyNames(); keys.hasMoreElements();) {
		String key = (String) keys.nextElement();
		String entry = props.getProperty(key);
		Vector list = new Vector(4);
		for (StringTokenizer tokens = new StringTokenizer(entry, ";"); tokens.hasMoreTokens();)
			list.addElement(new Path(tokens.nextToken()));
		IPath[] paths = new IPath[list.size()];
		list.copyInto(paths);
		result.put(key, paths);
	}
	return result;
}
public Hashtable readPluginPath(IPath location) throws CoreException {
	Properties props = new Properties();
	location = location.append(F_PLUGIN_PATH);
	IPath tempLocation = getBackupFilePathFor(location);
	SafeFileInputStream stream = null;
	try {
		try {
			stream = new SafeFileInputStream(location.toOSString(), tempLocation.toOSString());
			props.load(stream);
		} finally {
			if (stream != null)
				stream.close();
		}
	} catch (IOException e) {
		String message = Policy.bind("meta.readPlatformMeta", location.toString());
		IStatus status = new Status(IStatus.ERROR, Platform.PI_RUNTIME, Platform.FAILED_READ_METADATA, message, e);
		throw new CoreException (status);
}
	return parsePathProperties(props);
}
/**
 * @see IWorkbenchProtected#setPluginPath
 */
public void writePluginPath(Hashtable paths, IPath location) throws CoreException {
	Properties props = buildPathProperties(paths);
	location = location.append(F_PLUGIN_PATH);
	IPath tempLocation = getBackupFilePathFor(location);
	SafeFileOutputStream stream = null;
	try {
		try {
			stream = new SafeFileOutputStream(location.toOSString(), tempLocation.toOSString());
			props.store(stream, null);
		} finally {
			if (stream != null)
				stream.close();
		}
	} catch (IOException e) {
		String message = Policy.bind("meta.writePlatformMeta", location.toString());
		IStatus status = new Status(IStatus.ERROR, Platform.PI_RUNTIME, Platform.FAILED_WRITE_METADATA, message, e);
		throw new CoreException (status);
	}
}
}
