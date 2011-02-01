package nl.vpro.util;

import org.junit.Test;
import static org.junit.Assert.assertEquals;



public class HTMLStripperTest {

	@Test
	public void testStripperSimple() {
		HTMLStripper stripper = new HTMLStripper();

		String input = "<p>Mobiel maakt slapeloos</p><p>Mysterieuze CO2 wolken op Mars</p><p>Kies onbewust</p><p>Vondst eeuwenoude Inca-vesting</p>";
		String expected = "Mobiel maakt slapeloos\n\nMysterieuze CO2 wolken op Mars\n\nKies onbewust\n\nVondst eeuwenoude Inca-vesting";
		String output = stripper.StripHTML(input);
		assertEquals(expected, output);
	}

}
