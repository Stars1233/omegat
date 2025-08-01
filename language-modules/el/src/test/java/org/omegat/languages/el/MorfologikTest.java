/*******************************************************************************
  OmegaT - Computer Assisted Translation (CAT) tool
           with fuzzy matching, translation memory, keyword search,
           glossaries, and translation leveraging into updated projects.

  Copyright (C) 2024 Hiroshi Miura
                Home page: https://www.omegat.org/
                Support center: https://omegat.org/support

  This file is part of OmegaT.

  OmegaT is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  OmegaT is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with this program.  If not, see <https://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.omegat.languages.el;

import org.junit.Test;

import org.omegat.languages.LanguageModuleTestBase;
import org.omegat.spellchecker.morfologik.MorfologikSpellchecker;


public class MorfologikTest extends LanguageModuleTestBase {

    private static final String LANGUAGE = "el_GR";
    private static final String GOOD = "Καλημέρα";

    @Test
    public void testDictionary() throws Exception {
        testDictionaryHelper(new MorfologikSpellchecker(), LANGUAGE, GOOD, null);
    }
}
