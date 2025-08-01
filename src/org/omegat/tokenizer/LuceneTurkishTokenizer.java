/**************************************************************************
 OmegaT - Computer Assisted Translation (CAT) tool
          with fuzzy matching, translation memory, keyword search,
          glossaries, and translation leveraging into updated projects.

 Copyright (C) 2013 Aaron Madlon-Kay
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
 **************************************************************************/
package org.omegat.tokenizer;

import java.io.IOException;
import java.io.StringReader;

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tr.TurkishAnalyzer;

/**
 * The LuceneTurkishTokenizer provides tokenization for Turkish text.
 * This tokenizer utilizes Lucene's TurkishAnalyzer to process text into tokens,
 * supporting optional stemming and stop words removal based on the provided configuration.
 * It is designed to integrate with the Lucene framework for text analysis.
 * <p>
 * This class extends the functionality of the BaseTokenizer and overrides
 * the required method for obtaining the token stream for Turkish language processing.
 *
 * @author Aaron Madlon-Kay
 */
@Tokenizer(languages = { "tr" }, isDefault = true)
public class LuceneTurkishTokenizer extends BaseTokenizer {
    @SuppressWarnings("resource")
    @Override
    protected TokenStream getTokenStream(final String strOrig, final boolean stemsAllowed,
            final boolean stopWordsAllowed) throws IOException {
        if (stemsAllowed) {
            CharArraySet stopWords = stopWordsAllowed ? TurkishAnalyzer.getDefaultStopSet() : CharArraySet.EMPTY_SET;
            return new TurkishAnalyzer(stopWords).tokenStream("", new StringReader(strOrig));
        } else {
            return getStandardTokenStream(strOrig);
        }
    }
}
