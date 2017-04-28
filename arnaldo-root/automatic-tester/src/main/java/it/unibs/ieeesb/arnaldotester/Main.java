package it.unibs.ieeesb.arnaldotester;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
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

import it.unibs.ieeesb.arnaldoproject.Greeting;
import it.unibs.ieeesb.arnaldotester.tests.TestGreeting;

public class Main {
	
	private static final Logger LOGGER = LogManager.getLogger();

	private String jarName;
	private Level logLevel;
	
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
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		try {
			System.out.println(this.doTest());
		} catch (MalformedURLException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void parseCommandLine(String[] args) throws ParseException {
		Options options = new Options();

		options.addOption("j", "--jar", true, "The jar you want to test");
		options.addOption("l", "--log", true, "The log level. Can be DEBUG, INFO, WARN, ERROR, CRITICAL. Default to INFO.");
		CommandLineParser parser = new DefaultParser();
		CommandLine cmd = parser.parse( options, args);

		this.jarName = cmd.getOptionValue('j');
		this.logLevel = Level.valueOf(cmd.getOptionValue('l', "INFO"));
	}

	private JSONObject doTest() throws MalformedURLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		TestHelper th = TestHelper.instance();
		th.clear();
		th.setJarName(this.jarName);
		System.out.println(th.getInstance(Greeting.class));
		
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
				LOGGER.info("Test {} started", description.getMethodName());
			}

			@Override
			public void testFinished(Description description) throws Exception {
				super.testFinished(description);
			}

			@Override
			public void testFailure(Failure failure) throws Exception {
				LOGGER.warn("Test {} has encountered the following general failure {}", failure.getDescription().getMethodName(), failure.getMessage());
				super.testFailure(failure);
			}

			@Override
			public void testAssumptionFailure(Failure failure) {
				LOGGER.warn("Test {} has encountered a failing assertion {}", failure.getDescription().getMethodName(), failure.getException().getMessage());
				super.testAssumptionFailure(failure);
			}

			@Override
			public void testIgnored(Description description) throws Exception {
				super.testIgnored(description);
			}
			
		});
		
		Result result = jUnitCore.run(computer, TestGreeting.class);
		LOGGER.info("Unit test executed. generating output");
		
		return this.generateOutput(result);
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
