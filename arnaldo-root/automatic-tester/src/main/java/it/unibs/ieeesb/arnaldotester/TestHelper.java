package it.unibs.ieeesb.arnaldotester;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.clapper.util.classutil.AbstractClassFilter;
import org.clapper.util.classutil.AndClassFilter;
import org.clapper.util.classutil.ClassFilter;
import org.clapper.util.classutil.ClassFinder;
import org.clapper.util.classutil.ClassInfo;
import org.clapper.util.classutil.InterfaceOnlyClassFilter;
import org.clapper.util.classutil.NotClassFilter;
import org.clapper.util.classutil.SubclassClassFilter;

import junit.framework.TestCase;

public class TestHelper {
	
	private static final Logger LOGGER = LogManager.getLogger();
	private static TestHelper singleton = null;
	
	private String jarName;
	private ClassLoader jarClassLoader;
	
	public static TestHelper instance() {
		if (singleton == null) {
			singleton = new TestHelper();
		}
		return singleton;
	}
	
	private TestHelper() {
		this.clear();
	}
	
	public void clear() {
		this.jarName = null;
		this.jarClassLoader = null;
	}
	
	public void setJarName(String jarName) throws MalformedURLException {
		this.jarName = jarName;
		this.jarClassLoader = this.getJarFileClassLoader(this.jarName);
	}

	public <CLAZZ> CLAZZ getInstance(Class<CLAZZ> interfaceToFind) throws InstantiationException, IllegalAccessException, ClassNotFoundException {
		return this.findInterfaceInJar(interfaceToFind).newInstance();
	}
	
	private ClassLoader getJarFileClassLoader(String jarFile) throws MalformedURLException {
		File f = new File(jarFile);
		return new URLClassLoader(new URL[]{f.toURI().toURL()}, this.getClass().getClassLoader());
	}
	
	private <CLAZZ> Class<CLAZZ> findInterfaceInJar(Class<CLAZZ> interfaceToFind) throws ClassNotFoundException {
		ClassFinder finder = new ClassFinder();
		finder.addClassPath();
		finder.add(new File(this.jarName));

		ClassFilter filter =
				new AndClassFilter(
						// Must not be an interface
						new NotClassFilter (new InterfaceOnlyClassFilter()),
						// Must implement the ClassFilter interface
						new SubclassClassFilter(interfaceToFind),
						// Must not be abstract
						new NotClassFilter (new AbstractClassFilter())
						);

		Collection<ClassInfo> foundClasses = new ArrayList<ClassInfo>();
		finder.findClasses (foundClasses, filter);

		LOGGER.info("We have found {} classes implementing the interface {}", foundClasses.size(), interfaceToFind.getName());
		if (foundClasses.size() == 0) {
			LOGGER.error("no classes named {} found inside jar {}", interfaceToFind.getName(), this.jarName);
		}
		if (foundClasses.size() > 1) {
			LOGGER.error("too many classes named {} found inside the jar {}!", interfaceToFind.getName(), this.jarName);
		}
		
		String className = foundClasses.iterator().next().getClassName();
		LOGGER.info("The class implementing the interface {} is {}", interfaceToFind.getName(), className);
		return (Class<CLAZZ>) this.jarClassLoader.loadClass(className);
	}

}
