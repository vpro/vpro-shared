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

	@Test
	public void testStripperComplex() {
		HTMLStripper stripper = new HTMLStripper();

		String input = "<p>De 22q11DS poli<br />In het Universitair Medisch Centrum Utrecht bestaat een multidisciplinaire <a onclick=\"window.open(this.href);return false;\" title=\"22q11-Poli UMC Utrecht\" href=\"http://www.umcutrecht.nl/subsite/vcfs/\">polikliniek</a> voor kinderen met het 22q11DS/velocardiofaciaal syndroom.&#160;&nbsp;</p><p>In samenwerking met het Albert Schweitzer ziekenhuis in Dordrecht is halverwege 2004 een nieuw bestralingstoestel aangeschaft, de <a onclick=\"window.open(this.href);return false;\" title=\"Cyberknife in Erasmus MC Rotterdam\" href=\"http://www.erasmusmc.nl/radiotherapie/patientenzorg/behandelmethoden/uitwendigebestraling/128774/\">Cyberknife</a>, een robot met een vrij kleine versnellerbuis aan zijn robotarm. Sinds maart 2005 worden patiënten hiermee behandeld. Na Vicenza (Italië) is dit de tweede Cyberknife die in Europa in gebruik is genomen. </p><p>Onderwerpen die in de tv-serie Het Academisch Ziekenhuis aan bod komen zijn onder meer baanbrekende  behandelmethodes voor kanker en andere levensbedreigende aandoeningen, geavanceerde diagnostiek bij onder meer tumoren, healing environments, nieuwe operatietechnieken op het gebied van kijkoperaties en transplantatiechirurgie.</p>";

		String expected = "De 22q11DS poli\n"
				+ "In het Universitair Medisch Centrum Utrecht bestaat een multidisciplinaire  polikliniek voor kinderen met het 22q11DS/velocardiofaciaal syndroom.  \n\n"
				+

				"In samenwerking met het Albert Schweitzer ziekenhuis in Dordrecht is halverwege 2004 een nieuw bestralingstoestel aangeschaft, de  Cyberknife, een robot met een vrij kleine versnellerbuis aan zijn robotarm. Sinds maart 2005 worden patiënten hiermee behandeld. Na Vicenza (Italië) is dit de tweede Cyberknife die in Europa in gebruik is genomen.\n\n"
				+

				"Onderwerpen die in de tv-serie Het Academisch Ziekenhuis aan bod komen zijn onder meer baanbrekende behandelmethodes voor kanker en andere levensbedreigende aandoeningen, geavanceerde diagnostiek bij onder meer tumoren, healing environments, nieuwe operatietechnieken op het gebied van kijkoperaties en transplantatiechirurgie.";

		String output = stripper.StripHTML(input);
		assertEquals(expected, output);

	}
}
