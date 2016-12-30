package nl.vpro.web.filter.compat;

import javax.servlet.ServletRequest;

/**
 * Stripped-down version of the ServletRequestUtils class in Spring Framework
 * 2.5, so this module doesn't depend on Spring 2.5 (www-main is still on Spring
 * 1.x).
 * 
 * @author nils
 * 
 */
public abstract class ServletRequestUtils {

	private static final BooleanParser BOOLEAN_PARSER = new BooleanParser();

	/**
	 * Get a boolean parameter, with a fallback value. Never throws an
	 * exception. Can pass a distinguished value as default to enable checks of
	 * whether it was supplied.
	 * <p>
	 * Accepts "true", "on", "yes" (any case) and "1" as values for true; treats
	 * every other non-empty value as false (i.e. parses leniently).
	 * 
	 * @param request
	 *            current HTTP request
	 * @param name
	 *            the name of the parameter
	 * @param defaultVal
	 *            the default value to use as fallback
	 */
	public static boolean getBooleanParameter(ServletRequest request,
			String name, boolean defaultVal) {
		if (request.getParameter(name) == null) {
			return defaultVal;
		}
		try {
			return getRequiredBooleanParameter(request, name);
		} catch (Exception ex) {
			return defaultVal;
		}
	}

	/**
	 * Get a boolean parameter, throwing an exception if it isn't found or isn't
	 * a boolean.
	 * <p>
	 * Accepts "true", "on", "yes" (any case) and "1" as values for true; treats
	 * every other non-empty value as false (i.e. parses leniently).
	 * 
	 * @param request
	 *            current HTTP request
	 * @param name
	 *            the name of the parameter
	 */
	public static boolean getRequiredBooleanParameter(ServletRequest request,
			String name) throws Exception {
		return BOOLEAN_PARSER.parseBoolean(name, request.getParameter(name));
	}

	private abstract static class ParameterParser {

		protected final Object parse(String name, String parameter)
				throws Exception {
			validateRequiredParameter(name, parameter);
			try {
				return doParse(parameter);
			} catch (NumberFormatException ex) {
				throw new Exception("Required " + getType() + " parameter '"
						+ name + "' with value of '" + parameter
						+ "' is not a valid number", ex);
			}
		}

		protected final void validateRequiredParameter(String name,
				Object parameter) throws Exception {
			if (parameter == null) {
				throw new Exception("Parameter " + name + " of type "
						+ getType() + " missing");
			}
		}

		protected abstract String getType();

		protected abstract Object doParse(String parameter)
				throws NumberFormatException;
	}

	private static class BooleanParser extends ParameterParser {

		protected String getType() {
			return "boolean";
		}

		protected Object doParse(String parameter) throws NumberFormatException {
			return (parameter.equalsIgnoreCase("true")
					|| parameter.equalsIgnoreCase("on")
					|| parameter.equalsIgnoreCase("yes")
					|| parameter.equals("1") ? Boolean.TRUE : Boolean.FALSE);
		}

		public boolean parseBoolean(String name, String parameter)
				throws Exception {
			return ((Boolean) parse(name, parameter)).booleanValue();
		}

		public boolean[] parseBooleans(String name, String[] values)
				throws Exception {
			validateRequiredParameter(name, values);
			boolean[] parameters = new boolean[values.length];
			for (int i = 0; i < values.length; i++) {
				parameters[i] = parseBoolean(name, values[i]);
			}
			return parameters;
		}
	}
}
