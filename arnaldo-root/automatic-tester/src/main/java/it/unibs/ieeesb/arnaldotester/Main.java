package it.unibs.ieeesb.arnaldotester;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.core.LoggerContext;
import org.apache.logging.log4j.core.config.Configuration;
import org.apache.logging.log4j.core.config.LoggerConfig;
import org.clapper.util.classutil.AbstractClassFilter;
import org.clapper.util.classutil.AndClassFilter;
import org.clapper.util.classutil.ClassFilter;
import org.clapper.util.classutil.ClassFinder;
import org.clapper.util.classutil.ClassInfo;
import org.clapper.util.classutil.InterfaceOnlyClassFilter;
import org.clapper.util.classutil.NotClassFilter;
import org.clapper.util.classutil.SubclassClassFilter;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.runner.Computer;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

import it.unibs.ieeesb.arnaldoproject.ArnaldoProjectException;
import it.unibs.ieeesb.arnaldoproject.Greeting;
import it.unibs.ieeesb.arnaldotester.tests.TestGreeting;

public class Main {
	
	private static final Logger LOGGER = LogManager.getLogger();

	private String jarName;
	private String interfaceName;
	private String testToExecute;
	private Level logLevel;

	public static void main(String[] args) {
		Main m = new Main();
		m.doWork(args);
		System.exit(0);
	}

	private void doWork(String[] args) {
		//parse command line information
		try {
			this.parseCommandLine(args);
		} catch (ParseException | ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		//set log level (see http://stackoverflow.com/a/23434603/1887602)
		LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
		Configuration config = ctx.getConfiguration();
		LoggerConfig loggerConfig = config.getLoggerConfig(LogManager.ROOT_LOGGER_NAME); 
		loggerConfig.setLevel(this.logLevel);
		ctx.updateLoggers(); 
		
		//do the dirty work
		try {
			System.out.println(this.doTest());
		} catch (MalformedURLException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			e.printStackTrace();
			System.exit(2);
		} catch (ArnaldoProjectException e) {
			System.err.println(e.getMessage());
			LOGGER.fatal(e.getMessage());
			System.exit(3);
		}
	}

	private void parseCommandLine(String[] args) throws ParseException, ClassNotFoundException {
		Options options = new Options();

		options.addOption("j", "jar", true, "The jar you want to test");
		options.addOption("l", "log", true, "The log level. Can be DEBUG, INFO, WARN, ERROR, CRITICAL. Default to INFO.");
		options.addOption("i", "interface", true, "The symbolic name (ie. it.unibs.ieeesb.arnaldotester.Greeting) of the interface you want to test.");
		options.addOption("t", "test-case", true, "The symbolic name (ie. it.unibs.ieeesb.arnaldotester.tests.TestGreeting) of the test you want to execute");
		options.addOption("h", "help", false, "Print an help information");
		
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse(options, args);

		if (cmd.hasOption('h')) {
			HelpFormatter formatter = new HelpFormatter();
			formatter.printHelp( "arnaldo tester", options );
			System.exit(0);
		}
		this.jarName = cmd.getOptionValue('j');
		this.interfaceName = cmd.getOptionValue('i', "it.unibs.ieeesb.arnaldoproject.Greeting");
		this.testToExecute = cmd.getOptionValue('t', "it.unibs.ieeesb.arnaldotester.tests.TestGreeting");
		this.logLevel = Level.valueOf(cmd.getOptionValue('l', "INFO"));
		
	}

	private JSONObject doTest() throws MalformedURLException, InstantiationException, IllegalAccessException, ClassNotFoundException, ArnaldoProjectException {
		TestHelper th = TestHelper.instance();
		th.clear();
		th.setHelper(this.jarName, this.interfaceName);
		if (!th.checkHelper()) {
			throw new ArnaldoProjectException(MessageFormat.format("Can''t fetch an implementation of class {0} inside the jar {1}!", this.interfaceName, this.jarName));
		}
		
		Computer computer = new Computer();
		JUnitCore jUnitCore = new JUnitCore();
		jUnitCore.addListener(new RunListener() {

			@Override
			public void testRunStarted(Description description) throws Exception {
				super.testRunStarted(description);
			}

			@Override
			public void testRunFinished(Result result) throws Exception {
				super.testRunFinished(result);
			}

			@Override
			public void testStarted(Description description) throws Exception {
				super.testStarted(description);
				LOGGER.info("Test {0} started", description.getMethodName());
			}

			@Override
			public void testFinished(Description description) throws Exception {
				super.testFinished(description);
			}

			@Override
			public void testFailure(Failure failure) throws Exception {
				LOGGER.warn("Test {0} has encountered the following general failure {1}", failure.getDescription().getMethodName(), failure.getMessage());
				super.testFailure(failure);
			}

			@Override
			public void testAssumptionFailure(Failure failure) {
				LOGGER.warn("Test {0} has encountered a failing assertion {1}", failure.getDescription().getMethodName(), failure.getException().getMessage());
				super.testAssumptionFailure(failure);
			}

			@Override
			public void testIgnored(Description description) throws Exception {
				super.testIgnored(description);
			}
			
		});
		
		if (!this.checkTestCase(this.testToExecute)) {
			throw new ArnaldoProjectException(MessageFormat.format("Can''t find test class {0} in order to test an implementation of {1}", this.testToExecute, this.interfaceName));
		}
		
		Result result = jUnitCore.run(computer, Class.forName(this.testToExecute));
		LOGGER.info("Unit test executed. generating output");
		
		return this.generateOutput(result);
	}
	
	private boolean checkTestCase(String name) {
		try {
			Class<?> tc = Class.forName(name);
			if (tc == null) {
				return false;
			}
			if (tc.newInstance() == null) {
				return false;
			}
		} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
			return false;
		}
		return true;
	}
	
	private static Map<String, Object> createMap(Object... keyValueMapping) {
		Map<String, Object> retVal = new HashMap<>();
		String key = "";
		Object value = null;
		
		for(int i=0; i<keyValueMapping.length; i++) {
			if ((i % 2) == 0) {
				//this is a key
				key = keyValueMapping[i].toString();
			} else {
				value = keyValueMapping[i];
				//add the key-value inside the map
				retVal.put(key, value);
			}
		}
		return retVal;
	}
	
	private JSONObject generateOutput(Result result) {
		JSONObject retVal = new JSONObject();
		retVal.put("failure-count", Integer.toString(result.getFailureCount()));
		retVal.put("total-ms-used", Long.toString(result.getRunTime()));
		retVal.put("total-tests-run", Integer.toString(result.getRunCount()));
		retVal.put("was-successful", Boolean.toString(result.wasSuccessful()));
		
		JSONArray failureList = new JSONArray();
		result.getFailures().forEach(f -> {
			failureList.put(new JSONObject(createMap("name", f.getDescription().getMethodName(), "cause", f.getMessage())));
		});
		retVal.put("failures", failureList);
		
		return retVal;
	}
}
