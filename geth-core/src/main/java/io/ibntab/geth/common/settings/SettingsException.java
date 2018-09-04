package io.ibntab.geth.common.settings;

import io.ibntab.geth.exception.GethException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * @author ke.meng created on 2018/8/22
 */
public abstract class SettingsException extends GethException {

	private static final Pattern SPLIT_LINES = Pattern.compile("\\r?\\n");

	/**
	 * Error line number, if defined.
	 *
	 * @return Error line number, if defined.
	 */
	public abstract Integer line();

	/**
	 * Column position, if defined.
	 *
	 * @return Column position, if defined.
	 */
	public abstract Integer position();

	/**
	 * @return Input stream used to read the source content.
	 *
	 * Input stream used to read the source content.
	 */
	public abstract String input();

	/**
	 * The source file name if defined.
	 *
	 * @return The source file name if defined.
	 */
	public abstract String sourceName();

	/**
	 * Extracts interesting lines to be displayed to the user.
	 *
	 * @param border number of lines to use as a border
	 * @return the extracted lines
	 */
	public InterestingLines interestingLines(int border) {
		try {
			if(input() == null || line() == null) {
				return null;
			}

			String[] lines = SPLIT_LINES.split(input(), 0);
			int firstLine = Math.max(0, line() - 1 - border);
			int lastLine = Math.min(lines.length - 1, line() - 1 + border);
			List<String> focusOn = new ArrayList<String>();
			for(int i = firstLine; i <= lastLine; i++) {
				focusOn.add(lines[i]);
			}
			return new InterestingLines(firstLine + 1, focusOn.toArray(new String[0]), line() - firstLine - 1);
		} catch(Throwable e) {
			e.printStackTrace();
			return null;
		}
	}

	public String toString() {
		return super.toString() + " in " + sourceName() + ":" + line();
	}

	public static class InterestingLines {

		public final int firstLine;
		public final int errorLine;
		public final String[] focus;

		private InterestingLines(int firstLine, String[] focus, int errorLine){
			this.firstLine = firstLine;
			this.errorLine = errorLine;
			this.focus = focus;
		}

	}
}
