package it.unibs.ieeesb.arnaldotester;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
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

import it.unibs.ieeesb.arnaldoproject.Greeting;

public class Main {
	
	private static final Logger LOGGER = LogManager.getLogger(Main.class.getName());

	private String jarName;
	
	private ClassLoader jarClassLoader;

	public static void main(String[] args) {
		//nel classpath ci deve essere il jar da testare
		Main m = new Main();
		m.doWork(args);
	}

	private void doWork(String[] args) {
		Greeting greetingImplementation = null;

		try {
			this.parseCommandLine(args);
			LOGGER.info("jar Name is {}", this.jarName);
		} catch (ParseException e1) {
			e1.printStackTrace();
		}

		try {
			this.jarClassLoader = this.getJarFileClassLoader(this.jarName);
//			Class<?> clazz = this.jarClassLoader.loadClass("it.unibs.ieeesb.arnaldoproject.simpleimpl.GreetingImpl");
//			System.out.println("clazz is " + clazz);
		} catch (MalformedURLException e1) {
			e1.printStackTrace();
		}

		try {
			greetingImplementation = this.findInterfaceInJar(Greeting.class).newInstance();
		} catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		this.doTest(greetingImplementation);
	}

	private ClassLoader getJarFileClassLoader(String jarFile) throws MalformedURLException {
		File f = new File(jarFile);
		URLClassLoader child = new URLClassLoader(new URL[]{f.toURI().toURL()}, this.getClass().getClassLoader());
		return child;
	}

	private void parseCommandLine(String[] args) throws ParseException {
		Options options = new Options();

		options.addOption("j", "--jar", true, "The jar you want to test");
		options.addOption("l", "--log", true, "The log level. Can be DEBUG, INFO, WARN, ERROR, CRITICAL");
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse( options, args);

		this.jarName = cmd.getOptionValue('j');
		
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


	private void doTest(Greeting implementation) {
		System.out.println("TUTTO OK");
	}
}
