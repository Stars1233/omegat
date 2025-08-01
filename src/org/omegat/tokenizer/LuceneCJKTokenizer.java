/**************************************************************************
 OmegaT - Computer Assisted Translation (CAT) tool
          with fuzzy matching, translation memory, keyword search,
          glossaries, and translation leveraging into updated projects.

 Copyright (C) 2008 Alex Buloichik (alex73mail@gmail.com)
               2013 Aaron Madlon-Kay
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
import org.apache.lucene.analysis.cjk.CJKAnalyzer;
import org.apache.lucene.analysis.cjk.CJKBigramFilter;
import org.apache.lucene.analysis.cjk.CJKWidthFilter;
import org.apache.lucene.analysis.core.LowerCaseFilter;

/**
 * Tokenizer implementation specifically designed for processing CJK (Chinese, Japanese, Korean) text.
 * It extends the BaseTokenizer to provide tokenization functionality tailored for CJK languages.
 * <p>
 * This tokenizer utilizes specific analysis tools like {@link CJKAnalyzer}, {@link CJKBigramFilter},
 * {@link CJKWidthFilter}, and {@link LowerCaseFilter} to process and create tokens suitable for
 * CJK text indexing and searching.
 *
 * @author Alex Buloichik (alex73mail@gmail.com)
 * @author Aaron Madlon-Kay
 */
@Tokenizer(languages = { "zh", "ja", "ko" })
public class LuceneCJKTokenizer extends BaseTokenizer {
    @SuppressWarnings("resource")
    @Override
    protected TokenStream getTokenStream(final String strOrig, final boolean stemsAllowed,
            final boolean stopWordsAllowed) throws IOException {
        if (stemsAllowed) {
            CharArraySet stopWords = stopWordsAllowed ? CJKAnalyzer.getDefaultStopSet() : CharArraySet.EMPTY_SET;
            return new CJKAnalyzer(stopWords).tokenStream("", new StringReader(strOrig));
        } else {
            return new LowerCaseFilter(new CJKBigramFilter(new CJKWidthFilter(super.getStandardTokenStream(strOrig))));
        }
    }
}
