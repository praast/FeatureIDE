/* FeatureIDE - An IDE to support feature-oriented software development
 * Copyright (C) 2005-2010  FeatureIDE Team, University of Magdeburg
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * See http://www.fosd.de/featureide/ for further information.
 */

import junit.framework.JUnit4TestAdapter;

import org.junit.Test;
import org.prop4j.Prop4JTest;

/**
 * The FeatureIDE test suite that should contain all available JUnit tests.
 *
 * TODO @Fabian please avoid the default package (Thomas)
 * TODO @Fabian do we need this class? (Thomas)
 * 
 * @author Thomas Thuem
 * @author Fabian Benduhn
 */
public class FMCoreTest {
	
	@Test
	public void test(){
		System.out.println("it works");
	}
	
	public static junit.framework.Test suite() {
		return new JUnit4TestAdapter(Prop4JTest.class);
	}

}