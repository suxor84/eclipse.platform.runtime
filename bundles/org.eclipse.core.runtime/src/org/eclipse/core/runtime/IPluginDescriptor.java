/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.runtime;

import java.net.URL;
import java.util.*;

/**
 * A plug-in descriptor contains information about a plug-in
 * obtained from the plug-in's manifest (<code>plugin.xml</code>) file.
 * <p>
 * Plug-in descriptors are platform-defined objects that exist
 * in the plug-in registry independent of whether a plug-in has
 * been started. In contrast, a plug-in's runtime object 
 * (<code>getPlugin</code>) generally runs plug-in-defined code.
 * </p>
 * <p>
 * This interface is not intended to be implemented by clients.
 * </p>
 *
 * @see #getPlugin
 * @deprecated This class has been deprecated and its responsibilities are now spread across
 * several other classes. Please refer to the porting guide for a precise description.
 */
public interface IPluginDescriptor {
	/**
	 * Returns the extension with the given simple identifier declared in
	 * this plug-in, or <code>null</code> if there is no such extension.
	 * Since an extension might not have an identifier, some extensions
	 * can only be found via the <code>getExtensions</code> method.
	 *
	 * @param extensionName the simple identifier of the extension (e.g. <code>"main"</code>).
	 * @return the extension, or <code>null</code>
	 * @deprecated: Use {@link org.eclipse.core.runtime.registry.IExtensionRegistry#getExtension(String indentifier, String extensionName) IExtensionRegistry#getExtension}
	 */
	public IExtension getExtension(String extensionName);
	/**
	 * Returns the extension point with the given simple identifier
	 * declared in this plug-in, or <code>null</code> if there is no such extension point.
	 *
	 * @param extensionPointId the simple identifier of the extension point (e.g. <code>"wizard"</code>).
	 * @return the extension point, or <code>null</code>
	 * @deprecated  Use {@link org.eclipse.core.runtime.registry.IExtensionRegistry#getExtensionPoint(String elementId, String extensionPointName) IExtensionRegistry#getExtensionPoint(String elementId, String extensionPointName)}
	 */
	public IExtensionPoint getExtensionPoint(String extensionPointId);
	/**
	 * Returns all extension points declared by this plug-in.
	 * Returns an empty array if this plug-in does not declare any extension points.
	 *
	 * @return the extension points declared by this plug-in
	 * @deprecated Use {@link org.eclipse.core.runtime.registry.IExtensionRegistry#getExtensionPoints() IExtensionRegistry#getExtensionPoints()}  
	 */
	public IExtensionPoint[] getExtensionPoints();
	/**
	 * Returns all extensions declared by this plug-in.
	 * Returns an empty array if this plug-in does not declare any extensions.
	 *
	 * @return the extensions declared by this plug-in
	 * @deprecated Use {@link org.eclipse.core.runtime.registry.IExtensionRegistry#getExtensions(String) IExtensionRegistry#getExtensions(String)}
	 */
	public IExtension[] getExtensions();
	/**
	 * Returns the URL of this plug-in's install directory. 
	 * This is the directory containing
	 * the plug-in manifest file, resource bundle, runtime libraries,
	 * and any other files supplied with this plug-in. This directory is usually
	 * read-only. Plug-in relative information should be written to the location 
	 * provided by <code>Plugin.getStateLocation</code>.
	 *
	 * @return the URL of this plug-in's install directory
	 * @see #getPlugin
	 * @see Plugin#getStateLocation
	 * @deprecated This method is no longer available. To access the content of 
	 * your plugin's install directory, use the {@link org.osgi.framework.Bundle#getEntry(String) Bundle#getEntry(String)}
	 */
	public URL getInstallURL();
	/**
	 * Returns a displayable label for this plug-in.
	 * Returns the empty string if no label for this plug-in
	 * is specified in the plug-in manifest file.
	 * <p> Note that any translation specified in the plug-in manifest
	 * file is automatically applied.
	 * </p>
	 *
	 * @return a displayable string label for this plug-in,
	 *    possibly the empty string
	 * @see #getResourceString 
	 * @deprecated Use {@link org.osgi.framework.Bundle#getHeaders() Bundle.getHeaders().get(org.osgi.framework.Constants#BUNDLE_NAME)}
	 */
	public String getLabel();
	/**
	 * Returns the plug-in runtime object corresponding to this
	 * plug-in descriptor. Unlike other methods on this object,
	 * invoking this method may activate the plug-in.
	 * The returned object is an instance of the plug-in runtime class
	 * specified in the plug-in's manifest file;
	 * if a class is not specified there, the returned object
	 * is an internally-supplied one that does not react to life cycle requests.
	 *
	 * @return the plug-in runtime object
	 * @exception CoreException 
	 *   if this plug-in's runtime object could not be created.
	 * @see #isPluginActivated
	 * @deprecated The Plugin class no longer exists. Please see the porting guide for further
	 * information on the new concepts.  
	 */
	public Plugin getPlugin() throws CoreException;
	/**
	 * Returns the plug-in class loader used to load classes and resources
	 * for this plug-in. The class loader can be used to directly access
	 * plug-in resources and classes. Note that accessing a resource will
	 * <b>not activate</b> the corresponding plug-in. Successfully loading 
	 * a class will <b>always activate</b> the corresponding plug-in.
	 * <p> 
	 * The following examples illustrate the direct use of the plug-in class
	 * loader and its effect on plug-in activation (example ignores error
	 * handling).
	 *
	 * <pre>
	 *     ClassLoader loader = descriptor.getPluginClassLoader();
	 *
	 *     // Load resource by name. Will not activate the plug-in.
	 *     URL res = loader.getResource("com/example/Foo/button.gif");
	 *     InputStream is = loader.getResourceAsStream("splash.jpg");
	 *
	 *     // Load resource for class. Will activate the plug-in because
	 *     // the referenced class is loaded first and triggers activation.
	 *     URL u = com.example.Foo.class.getResource("button.gif");
	 *
	 *     // Load class by name. Will activate the plug-in.
	 *     Class c = loader.loadClass("com.example.Bar");
	 *
	 *     // Load a resource bundle. May, or may not activate the plug-in, depending
	 *     // on the bundle implementation. If implemented as a class, the plug-in
	 *     // will be activated. If implemented as a properties file, the plug-in will
	 *     // not be activated.
	 *     ResourceBundle b = 
	 *         ResourceBundle.getBundle("bundle", Locale.getDefault(), loader);
	 * </pre>
	 *
	 * @return the plug-in class loader
	 * @see IConfigurationElement#createExecutableExtension
	 * @see #isPluginActivated
	 * @see #getResourceBundle
	 * @deprecated Do to obvious security reasons, this API is no longer available.
	 * Instead you should consider using the{@link org.osgi.framework.Bundle#loadClass(String) Bundle#loadClass(String)}.
	 * Please see the porting guide for an enhanced description of how code using this feature can be reorganized. 
	 */
	public ClassLoader getPluginClassLoader();
	/**
	* Returns a list of plug-in prerequisites required
	* for correct execution of this plug-in.
	*
	* @return an array of plug-in prerequisites, or an empty array
	* if no prerequisites were specified
	* @deprecated The concept of prerequisite has been refined into two new concepts.
	* Import of packages and Requirement of bundles. See the porting guide presenting the new
	* concepts for an in depth discussion. Note that the IPluginPrerequisite class is also deprecated.
	* The import information can be obtained using {@link org.osgi.framework.Bundle#getHeaders() Bundle.getHeaders().get(org.osgi.framework.Constants#IMPORT_PACKAGE)},
	* the requirement information using {@link org.osgi.framework.Bundle#getHeaders() Bundle.getHeaders().get(org.osgi.framework.Constants#REQUIRE_BUNDLE)}).
	*/
	public IPluginPrerequisite[] getPluginPrerequisites();
	/**
	 * Returns the name of the provider of this plug-in.
	 * Returns the empty string if no provider name is specified in 
	 * the plug-in manifest file.
	 * <p> Note that any translation specified in the plug-in manifest
	 * file is automatically applied.
	 * </p>
	 *
	 * @see #getResourceString 
	 *
	 * @return the name of the provider, possibly the empty string
	 * @deprecated Use {@link org.osgi.framework.Bundle#getHeaders() Bundle.getHeaders().get(org.osgi.framework.Constants#BUNDLE_VENDOR)}
	 */
	public String getProviderName();
	/**
	 * Returns this plug-in's resource bundle for the current locale. 
	 * <p>
	 * The bundle is stored as the <code>plugin.properties</code> file 
	 * in the plug-in install directory, and contains any translatable
	 * strings used in the plug-in manifest file (<code>plugin.xml</code>)
	 * along with other resource strings used by the plug-in implementation.
	 * </p>
	 *
	 * @return the resource bundle
	 * @exception MissingResourceException if the resource bundle was not found
	 */
	public ResourceBundle getResourceBundle() throws MissingResourceException;
	/**
	 * Returns a resource string corresponding to the given argument value.
	 * If the argument value specifies a resource key, the string
	 * is looked up in the default resource bundle. If the argument does not
	 * specify a valid key, the argument itself is returned as the
	 * resource string. The key lookup is performed in the
	 * plugin.properties resource bundle. If a resource string 
	 * corresponding to the key is not found in the resource bundle
	 * the key value, or any default text following the key in the
	 * argument value is returned as the resource string.
	 * A key is identified as a string begining with the "%" character.
	 * Note, that the "%" character is stripped off prior to lookup
	 * in the resource bundle.
	 * <p>
	 * Equivalent to <code>getResourceString(value, getResourceBundle())</code>
	 * </p>
	 *
	 * @param value the value
	 * @return the resource string
	 * @see #getResourceBundle
	 */
	public String getResourceString(String value);
	/**
	 * Returns a resource string corresponding to the given argument 
	 * value and bundle.
	 * If the argument value specifies a resource key, the string
	 * is looked up in the given resource bundle. If the argument does not
	 * specify a valid key, the argument itself is returned as the
	 * resource string. The key lookup is performed against the
	 * specified resource bundle. If a resource string 
	 * corresponding to the key is not found in the resource bundle
	 * the key value, or any default text following the key in the
	 * argument value is returned as the resource string.
	 * A key is identified as a string begining with the "%" character.
	 * Note that the "%" character is stripped off prior to lookup
	 * in the resource bundle.
	 * <p>
	 * For example, assume resource bundle plugin.properties contains
	 * name = Project Name
	 * <pre>
	 *     getResourceString("Hello World") returns "Hello World"</li>
	 *     getResourceString("%name") returns "Project Name"</li>
	 *     getResourceString("%name Hello World") returns "Project Name"</li>
	 *     getResourceString("%abcd Hello World") returns "Hello World"</li>
	 *     getResourceString("%abcd") returns "%abcd"</li>
	 *     getResourceString("%%name") returns "%name"</li>
	 * </pre>
	 * </p>
	 *
	 * @param value the value
	 * @param bundle the resource bundle
	 * @return the resource string
	 * @see #getResourceBundle
	 */
	public String getResourceString(String value, ResourceBundle bundle);
	/**
	 * Returns all runtime libraries declared by this plug-in.
	 * Returns an empty array if this plug-in has no runtime libraries.
	 *
	 * @return the runtime libraries declared by this plug-in
	 * 
	 * @deprecated Use {@link org.osgi.framework.Bundle#getHeaders() Bundle.getHeaders().get(org.osgi.framework.Constants#BUNDLE_CLASSPATH)}. Note that the class 
	 * {@link ILibrary ILibrary} has been deprecated and that the value you'll get are actual strings from the manifest.
	 */
	public ILibrary[] getRuntimeLibraries();
	/**
	 * Returns the unique identifier of this plug-in.
	 * This identifier is a non-empty string and is unique 
	 * within the plug-in registry.
	 *
	 * @return the unique identifier of the plug-in 
	 *		(e.g. <code>"org.eclipse.core.runtime"</code>)
	 * @deprecated Use {@link org.osgi.framework.Bundle#getGlobalName() Bundle.getGlobalName()}
	 */
	public String getUniqueIdentifier();
	/**
	 * Returns the version identifier of this plug-in.
	 *
	 * @return the plug-in version identifier
	 * @deprecated Use {@link org.osgi.framework.Bundle#getHeaders() Bundle.getHeaders().get(org.osgi.framework.Constants#BUNDLE_VERSION)}
	 */
	public PluginVersionIdentifier getVersionIdentifier();
	/**
	 * Returns whether the plug-in described by this descriptor
	 * has been activated. Invoking this method will not cause the
	 * plug-in to be activated.
	 *
	 * @return <code>true</code> if this plug-in is activated, and
	 *   <code>false</code> otherwise
	 * @see #getPlugin
	 * @deprecated Use {@link org.osgi.framework.Bundle#getState() Bundle#getState()} and test for ACTIVE
	 */
	public boolean isPluginActivated();
	/**
	 * Returns a URL for the given path.  Returns <code>null</code> if the URL
	 * could not be computed or created.
	 * 
	 * @param file path relative to plug-in installation location 
	 * @return a URL for the given path or <code>null</code>  It is not
	 * necessary to perform a 'resolve' on this URL.
	 * 
	 * @since 2.0
	 * @deprecated {@link Platform#find(Bundle, IPath) Platform#find(Bundle, IPath)}
	 */
	public URL find(IPath path);
	/**
	 * Returns a URL for the given path.  Returns <code>null</code> if the URL
	 * could not be computed or created.
	 * 
	 * find will look for this path under the directory structure for this plugin
	 * and any of its fragments.  If this path will yield a result outside the
	 * scope of this plugin, <code>null</code> will be returned.  Note that
	 * there is no specific order to the fragments.
	 * 
	 * The following arguments may also be used
	 * 
	 *  $nl$ - for language specific information
	 *  $os$ - for operating system specific information
	 *  $ws$ - for windowing system specific information
	 * 
	 * A path of $nl$/about.properties in an environment with a default 
	 * locale of en_CA will return a URL corresponding to the first place
	 * about.properties is found according to the following order:
	 *   plugin root/nl/en/CA/about.properties
	 *   fragment1 root/nl/en/CA/about.properties
	 *   fragment2 root/nl/en/CA/about.properties
	 *   ...
	 *   plugin root/nl/en/about.properties
	 *   fragment1 root/nl/en/about.properties
	 *   fragment2 root/nl/en/about.properties
	 *   ...
	 *   plugin root/about.properties
	 *   fragment1 root/about.properties
	 *   fragment2 root/about.properties
	 *   ...
	 * 
	 * If a locale other than the default locale is desired, use an
	 * override map.
	 * 
	 * @param path file path relative to plug-in installation location
	 * @param override map of override substitution arguments to be used for
	 * any $arg$ path elements. The map keys correspond to the substitution
	 * arguments (eg. "$nl$" or "$os$"). The resulting
	 * values must be of type java.lang.String. If the map is <code>null</code>,
	 * or does not contain the required substitution argument, the default
	 * is used.
	 * @return a URL for the given path or <code>null</code>.  It is not
	 * necessary to perform a 'resolve' on this URL.
	 * 
	 * @since 2.0
	 * @deprecated {@link Platform#find(Bundle, IPath, Map) Platform#find(Bundle, IPath, Map)}
	 */
	public URL find(IPath path, Map override);
	/**
	 * Returns whether the plug-in described by this descriptor
	 * is a pre-3.0 style plugin and requires backward compatibility support.
	 * Invoking this method will not cause the
	 * plug-in to be activated.
	 *
	 * @return <code>true</code> if this plug-in requires backward compatibility, and
	 *   <code>false</code> otherwise
	 * @since 3.0
	 */
	public boolean isLegacy();
}
