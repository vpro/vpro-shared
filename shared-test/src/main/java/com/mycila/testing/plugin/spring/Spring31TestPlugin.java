package com.mycila.testing.plugin.spring;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import org.springframework.test.context.MergedContextConfiguration;

import com.mycila.testing.core.api.TestContext;
import com.mycila.testing.core.api.TestExecution;
import com.mycila.testing.core.plugin.DefaultTestPlugin;

public class Spring31TestPlugin extends DefaultTestPlugin {
	
	public static final String TESTCONTEXTMANAGER = "org.springframework.test.context.TestContextManager";
   	public static final String TESTCONTEXT = "org.springframework.test.context.TestContext";
   	public static final String APPLICATIONCONTEXT = "org.springframework.context.ApplicationContext";

   	@Override
    public void prepareTestInstance(TestContext context) throws Exception {
   		try {
   			final TestContextManager manager = new TestContextManager(context.introspector().testClass());
   			final org.springframework.test.context.TestContext ctx = manager.testContext();
   			context.attributes().set(TESTCONTEXTMANAGER, manager);
   			context.attributes().set(TESTCONTEXT, ctx);
   			setupContextLoader(ctx, new MycilaContextLoader(context));
   			manager.prepareTestInstance(context.introspector().instance());
   			context.attributes().set(APPLICATIONCONTEXT, manager.testContext().getApplicationContext());
	    } catch (Exception e) {
	        throw new RuntimeException(e.getMessage(), e);
	    }
	}
	
	@Override
	public void afterTest(TestExecution testExecution) throws Exception {
	    TestContextManager manager = testExecution.context().attributes().get(TESTCONTEXTMANAGER);
	    manager.afterTestMethod(
	            testExecution.context().introspector().instance(),
	            testExecution.method(),
	            testExecution.throwable());
	}
	
	@Override
	public void beforeTest(TestExecution testExecution) throws Exception {
	    TestContextManager manager = testExecution.context().attributes().get(TESTCONTEXTMANAGER);
	    manager.beforeTestMethod(
	            testExecution.context().introspector().instance(),
	            testExecution.method());
	}
	
	private void setupContextLoader(org.springframework.test.context.TestContext ctx, MycilaContextLoader loader) throws Exception {
		// spring 3.1 support
		Field mergedContextConfigurationF = ctx.getClass().getDeclaredField("mergedContextConfiguration");
		mergedContextConfigurationF.setAccessible(true);
		MergedContextConfiguration mergedContextConfiguration = (MergedContextConfiguration) mergedContextConfigurationF.get(ctx);
		
		// execute spring 3.1 ported functionality (set locations and context loader)
	    Field locations = mergedContextConfiguration.getClass().getDeclaredField("locations");
	    locations.setAccessible(true);
	    locations.set(mergedContextConfiguration, loader.contextLocations());
	    
	    Field contextLoader = mergedContextConfiguration.getClass().getDeclaredField("contextLoader");
	    contextLoader.setAccessible(true);
	    contextLoader.set(mergedContextConfiguration, loader);
	    
		// execute spring 3.0.x legacy functionality
		Field contextCache = ctx.getClass().getDeclaredField("contextCache");
	    contextCache.setAccessible(true);
	    Constructor<?> ctor = Class.forName("org.springframework.test.context.ContextCache").getDeclaredConstructor();
	    ctor.setAccessible(true);
	    contextCache.set(ctx, ctor.newInstance());
	}
	
	private static class TestContextManager extends org.springframework.test.context.TestContextManager {
	    public TestContextManager(Class<?> testClass) {
	        super(testClass);
	    }
	
	    public org.springframework.test.context.TestContext testContext() {
	        return super.getTestContext();
	    }
	}
}	

