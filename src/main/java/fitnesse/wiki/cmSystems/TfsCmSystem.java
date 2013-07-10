package fitnesse.wiki.cmSystems;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import fitnesse.testsystems.CommandRunner;

public class TfsCmSystem {

	static List<String> ignoredPaths = new ArrayList<String>();

	// hook for test case
	protected static Method executeMethod;

	protected static String login = "";
	protected static boolean verbose = false;

	static {
		ignoredPaths.add("/RecentChanges/");
		ignoredPaths.add("/ErrorLogs/");

		try {
			executeMethod = TfsCmSystem.class.getDeclaredMethod(
					"executeTfsCommand", String.class, String.class);
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		}

	}

	public static void cmEdit(String file, String payload) throws Exception {
		Log("INFO: cmEdit(" + file + ")");
		parsePayload(payload);

		if (isIgnored(file)) {
			return;
		}

		Map<String, String> properties = getProperties(file);

		if (isUnknown(properties)) {
			return;
		}

		if (isOpened(properties) || isOpenForAdd(properties)) {
			return;
		}

		if (isOpenForDelete(properties)) {
			execute("cmEdit", "tf undo " + file + " /noprompt");
		}

		execute("cmEdit", "tf edit " + file);
	}

	public static void cmUpdate(String file, String payload) throws Exception {
		Log("INFO: cmUpdate(" + file + ")");
		parsePayload(payload);

		if (isIgnored(file)) {
			return;
		}

		Map<String, String> properties = getProperties(file);

		if (isUnknown(properties)) {
			execute("cmUpdate", "tf add " + file);
			return;
		}

		if (isOpenForDelete(properties)) {
			execute("cmUpdate", "tf undo " + file + " /noprompt");
			if (isFolder(properties))
				execute("cmUpdate", "tf edit " + file + "*");
			else
				execute("cmUpdate", "tf edit " + file + "*");
			return;
		}

		if (isDeletedOnServer(properties)) {
			execute("cmUpdate", "tf add " + file);
			return;
		}

		if (!isOpened(properties) && !isOpenForAdd(properties))
			execute("cmUpdate", "tf edit " + file);

	}

	public static void cmDelete(String directory, String payload)
			throws Exception {
		Log("INFO: cmDelete(" + directory + ")");
		parsePayload(payload);
		
		if (isIgnored(directory)) {
			return;
		}

		Map<String, String> properties = getProperties(directory);

		if (isUnknown(properties)) {
			return;
		}

		if (isOpenForDelete(properties)) {
			return;
		}

		execute("cmDelete", "tf undo " + directory + " /recursive /noprompt");

		if (!isOpenForAdd(properties)) {
			execute("cmDelete", "tf delete " + directory);
		}
	}

	public static void cmPreDelete(String fileName, String string) {
		// Not needed for Team Foundation server
	}

	private static String execute(String method, String command)
			throws Exception {
		return (String) executeMethod.invoke(null, method, command + " " + login);
	}

	protected static String executeTfsCommand(String method, String command)
			throws Exception {
		Log("INFO: executing command(" + command + ")");
		CommandRunner runner = new CommandRunner(command, "");
		runner.run();
		if (runner.getError().length() > 0 || runner.getExitCode() != 0) {
			System.err.println(method + " command: " + command);
			System.err.println(method + " exit code: " + runner.getExitCode());
			System.err.println(method + " out:" + runner.getOutput());
			System.err.println(method + " err:" + runner.getError());
		}
		return runner.getOutput();
	}

	private static Map<String, String> getProperties(String filePath)
			throws Exception {
		Map<String, String> properties = new HashMap<String, String>();

		String fstatOutput = execute("getProperties", "tf properties "
				+ filePath);

		for (String line : fstatOutput.split("\n")) {
			String[] tokenizedLine = line.split(": ");
			if (tokenizedLine.length > 1)
				properties
						.put(tokenizedLine[0].trim(), tokenizedLine[1].trim());
			else {
				String status = tokenizedLine[0].trim();
				if (status.startsWith("No items match"))
					properties.put("No items match", filePath);
				else
					properties.put(tokenizedLine[0].trim(), "");
			}
		}
		return properties;
	}

	private static boolean isIgnored(String filePath) {
		for (String ignoredItem : ignoredPaths) {
			if (filePath.contains(ignoredItem))
				return true;
		}
		return false;
	}

	private static boolean isOpened(Map<String, String> properties) {
		return ("edit".equals(properties.get("Change")));
	}

	private static boolean isFolder(Map<String, String> properties) {
		return "folder".equals(properties.get("Type"));
	}

	private static boolean isOpenForAdd(Map<String, String> properties) {
		return "add".equals(properties.get("Change"));
	}

	private static boolean isOpenForDelete(Map<String, String> properties) {
		return "delete".equals(properties.get("Change"));
	}

	private static boolean isUnknown(Map<String, String> properties) {
		return properties.containsKey("No items match");
	}

	private static boolean isDeletedOnServer(Map<String, String> properties) {
		return properties.containsKey("Deletion ID")
				&& !"0".equals(properties.get("Deletion ID"));
	}
	
	private static void parsePayload(String payload) {
		int index = payload.indexOf("/login:");
		if(index >= 0) {
			login = payload.substring(index, payload.indexOf(" ", index));
		}	
		
		index = payload.indexOf("/verbose");
		
		if(index >= 0) {
			verbose = true;
		}
	}

	private static void Log(String message) {
		if(verbose)
			System.out.println(message);
	}
}
