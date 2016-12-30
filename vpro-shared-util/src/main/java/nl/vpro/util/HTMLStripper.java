package nl.vpro.util;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import javax.swing.text.MutableAttributeSet;
import javax.swing.text.html.HTML;
import javax.swing.text.html.HTMLEditorKit;

public class HTMLStripper {

	final HTMLEditorKit.Parser parser = new ParserGetter().getParser();

	// disallowed body tags
	final AllowedPattern allowBody = new AllowedPattern("(?i)script|embed|object|frameset|iframe", true);

	protected enum State {
		DEFAULT, SCRIPT, ERROR;
	}

	public String StripHTML(String input) {

		StringWriter output = new StringWriter();
		TagStripper callback = new TagStripper(output);

		StringReader sr = new StringReader(input);
		try {
			parser.parse(sr, callback, true);
		} catch (IOException ioe) {
			return null;
		}
		return output.toString();
	}

	protected static class ParserGetter extends HTMLEditorKit {
		private static final long serialVersionUID = 1L;

		// purely to make this method public
		public HTMLEditorKit.Parser getParser() {
			return super.getParser();
		}
	}

	protected class TagStripper extends HTMLEditorKit.ParserCallback {
		StringBuffer output = new StringBuffer();

		private final Writer out;
		boolean addNewlines = true;
		boolean PisDoubleNewline = true;
		List<HTML.Tag> stack = new ArrayList<HTML.Tag>();
		int removeBody = 0;
		State state = State.DEFAULT;
		StringBuilder spaceBuffer = new StringBuilder();
		int wrote = 0;

		public TagStripper(Writer out) {
			this.out = out;
		}

		@Override
		public void handleText(char[] text, int position) {
			try {
				// System.out.println("Handling " + new String(text) + " for " +
				// position + " " + stack);
				if (removeBody != 0) {
					return;
				}
				if (state == State.SCRIPT) {
					// sigh, the parser is pretty incomprehenisible
					// It give a very odd handleText event after a script tag.
					state = State.DEFAULT;
					return;
				}
				space();
				// no need to wrap in string first.
				if (text[0] == '>') {
					out.write(text, 1, text.length - 1);
					wrote += text.length - 2;
				} else {
					out.write(text);
					wrote += text.length;
				}
			} catch (IOException ioe) {

			}
		}

		protected void space() throws IOException {
			out.write(spaceBuffer.toString());
			wrote += spaceBuffer.length();
			spaceBuffer.setLength(0);
		}

		@Override
		public void handleStartTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
			// System.out.println("Start tag " + tag + " for " + position);
			stack.add(0, tag);
			if (tag == HTML.Tag.SCRIPT) {
				state = State.SCRIPT;
			}
			if (!allowBody.allows(tag.toString())) {
				removeBody++;
			}
			if (removeBody == 0) {
				if (tag == HTML.Tag.P && addNewlines) {
				} else {
					if (wrote > 0) {
						spaceBuffer.append(' ');
					}
				}
			}
		}

		@Override
		public void handleEndTag(HTML.Tag tag, int position) {
			// System.out.println("End tag " + tag + " at " + position);
			stack.remove(0);
			if (removeBody == 0) {
				if (tag == HTML.Tag.P && addNewlines) {
					if (PisDoubleNewline) {
						spaceBuffer.append("\n\n");
					} else {
						spaceBuffer.append("\n");
					}
				}
			}
			if (!allowBody.allows(tag.toString())) {
				removeBody--;
			}
		}

		@Override
		public void handleSimpleTag(HTML.Tag tag, MutableAttributeSet attributes, int position) {
			// stack.remove(0);
			// System.out.println("SIMPLE TAG " + tag);
			if (removeBody == 0) {
				if (tag == HTML.Tag.BR && addNewlines) {
					spaceBuffer.append('\n');
				} else {
					if (tag.breaksFlow()) {
						spaceBuffer.append(' ');
					}
				}
			}
		}

		@Override
		public void handleError(String mes, int position) {
			// System.out.println("Error " + mes + " at " + position);

			state = State.ERROR;
		}

		@Override
		public void handleComment(char[] data, int pos) {
			// System.out.println("Comment at " + pos + " for " + new
		}

		@Override
		public void handleEndOfLineString(String eol) {
			// System.out.println("EOL " + eol);
		}

		@Override
		public void flush() {
			try {
				out.flush();
			} catch (IOException e) {

			}
		}
	}

	private class AllowedPattern {
		private final Pattern pattern;
		private boolean inverse = false;

		public AllowedPattern(String pattern, boolean inverse) {
			this.pattern = Pattern.compile(pattern);
			this.inverse = inverse;
		}

		boolean allows(String p) {
			boolean match = pattern.matcher(p).matches() ? true : false;
			if (inverse)
				match = !match;

			return match;
		}

		public String toString() {
			return "!" + pattern.toString();
		}
	}
}
