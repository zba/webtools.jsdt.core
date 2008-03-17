/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.wst.jsdt.internal.core.builder;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.wst.jsdt.internal.compiler.env.AccessRuleSet;
import org.eclipse.wst.jsdt.internal.compiler.env.NameEnvironmentAnswer;
import org.eclipse.wst.jsdt.internal.compiler.util.SimpleLookupTable;

public class ClasspathDirectory extends ClasspathLocation {

IContainer binaryFolder; // includes .class files for a single directory
boolean isOutputFolder;
SimpleLookupTable directoryCache;
String[] missingPackageHolder = new String[1];
AccessRuleSet accessRuleSet;

ClasspathDirectory(IContainer binaryFolder, boolean isOutputFolder, AccessRuleSet accessRuleSet) {
	this.binaryFolder = binaryFolder;
	this.isOutputFolder = isOutputFolder;
	this.directoryCache = new SimpleLookupTable(5);
	this.accessRuleSet = accessRuleSet;
}

public void cleanup() {
	this.directoryCache = null;
}

String[] directoryList(String qualifiedPackageName) {
	String[] dirList = (String[]) directoryCache.get(qualifiedPackageName);
	if (dirList == missingPackageHolder) return null; // package exists in another classpath directory or jar
	if (dirList != null) return dirList;

	try {
		IResource container = binaryFolder.findMember(qualifiedPackageName); // this is a case-sensitive check
		if (container instanceof IContainer) {
			IResource[] members = ((IContainer) container).members();
			dirList = new String[members.length];
			int index = 0;
			for (int i = 0, l = members.length; i < l; i++) {
				IResource m = members[i];
				if (m.getType() == IResource.FILE && org.eclipse.wst.jsdt.internal.compiler.util.Util.isClassFileName(m.getName()))
					// add exclusion pattern check here if we want to hide .class files
					dirList[index++] = m.getName();
			}
			if (index < dirList.length)
				System.arraycopy(dirList, 0, dirList = new String[index], 0, index);
			directoryCache.put(qualifiedPackageName, dirList);
			return dirList;
		}
	} catch(CoreException ignored) {
		// ignore
	}
	directoryCache.put(qualifiedPackageName, missingPackageHolder);
	return null;
}

boolean doesFileExist(String fileName, String qualifiedPackageName, String qualifiedFullName) {
	String[] dirList = directoryList(qualifiedPackageName);
	if (dirList == null) return false; // most common case

	for (int i = dirList.length; --i >= 0;)
		if (fileName.equals(dirList[i]))
			return true;
	return false;
}

public boolean equals(Object o) {
	if (this == o) return true;
	if (!(o instanceof ClasspathDirectory)) return false;

	ClasspathDirectory dir = (ClasspathDirectory) o;
	if (this.accessRuleSet != dir.accessRuleSet)
		if (this.accessRuleSet == null || !this.accessRuleSet.equals(dir.accessRuleSet))
			return false;
	return this.binaryFolder.equals(dir.binaryFolder);
}

public NameEnvironmentAnswer findClass(String binaryFileName, String qualifiedPackageName, String qualifiedBinaryFileName) {

	return null;
}

public IPath getProjectRelativePath() {
	return binaryFolder.getProjectRelativePath();
}

protected boolean isExcluded(IResource resource) {
	return false;
}

public boolean isOutputFolder() {
	return isOutputFolder;
}

public boolean isPackage(String qualifiedPackageName) {
	return directoryList(qualifiedPackageName) != null;
}

public void reset() {
	this.directoryCache = new SimpleLookupTable(5);
}

public String toString() {
	String start = "Binary classpath directory " + this.binaryFolder.getFullPath().toString(); //$NON-NLS-1$
	if (this.accessRuleSet == null)
		return start;
	return start + " with " + this.accessRuleSet; //$NON-NLS-1$
}

public String debugPathString() {
	return this.binaryFolder.getFullPath().toString();
}


}