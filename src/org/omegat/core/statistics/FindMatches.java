/**************************************************************************
 OmegaT - Computer Assisted Translation (CAT) tool
          with fuzzy matching, translation memory, keyword search,
          glossaries, and translation leveraging into updated projects.

 Copyright (C) 2000-2006 Keith Godfrey and Maxym Mykhalchuk
               2008 Alex Buloichik
               2012 Thomas Cordonnier, Martin Fleurke
               2013 Aaron Madlon-Kay, Alex Buloichik
               2024 Hiroshi Miura
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

package org.omegat.core.statistics;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.omegat.core.Core;
import org.omegat.core.data.EntryKey;
import org.omegat.core.data.ExternalTMFactory;
import org.omegat.core.data.ExternalTMX;
import org.omegat.core.data.IProject;
import org.omegat.core.data.ITMXEntry;
import org.omegat.core.data.PrepareTMXEntry;
import org.omegat.core.data.SourceTextEntry;
import org.omegat.core.events.IStopped;
import org.omegat.core.matching.FuzzyMatcher;
import org.omegat.core.matching.ISimilarityCalculator;
import org.omegat.core.matching.LevenshteinDistance;
import org.omegat.core.matching.NearString;
import org.omegat.core.segmentation.Rule;
import org.omegat.tokenizer.ITokenizer;
import org.omegat.util.Language;
import org.omegat.util.OConsts;
import org.omegat.util.OStrings;
import org.omegat.util.PatternConsts;
import org.omegat.util.Preferences;
import org.omegat.util.Token;

/**
 * Class to find matches by specified criteria.
 *
 * Since we can use stemmers to prepare tokens, we should use 3-pass comparison
 * of similarity. Similarity will be calculated in 3 steps:
 *
 * 1. Split original segment into word-only tokens using stemmer (with stop
 * words list), then compare tokens.
 *
 * 2. Split original segment into word-only tokens without stemmer, then compare
 * tokens.
 *
 * 3. Split original segment into not-only-words tokens (including numbers and
 * tags) without stemmer, then compare tokens.
 *
 * This class is not thread safe ! Must be used in the one thread only.
 *
 * @author Maxym Mykhalchuk
 * @author Alex Buloichik (alex73mail@gmail.com)
 * @author Martin Fleurke
 * @author Aaron Madlon-Kay
 */
public class FindMatches {

    /**
     * According to gettext source code, PO fuzzy entries are created above 60%
     * <a href=
     * "https://sourceforge.net/p/omegat/feature-requests/1258/">RFE#1258</a>
     */
    static final int PENALTY_FOR_FUZZY = 40;
    private static final int PENALTY_FOR_REMOVED = 5;
    private static final int SUBSEGMENT_MATCH_THRESHOLD = 85;

    private static final Pattern SEARCH_FOR_PENALTY = Pattern.compile("penalty-(\\d+)");

    private static final String ORPHANED_FILE_NAME = OStrings.getString("CT_ORPHAN_STRINGS");

    private final ISimilarityCalculator distance = new LevenshteinDistance();

    /**
     * the removePattern that was configured by the user.
     */
    private final Pattern removePattern = PatternConsts.getRemovePattern();

    private final IProject project;
    private final ITokenizer tok;
    private final Locale srcLocale;
    private final int maxCount;

    /** Result list. */
    private List<NearString> result;

    private final boolean searchExactlyTheSame;
    private String srcText;

    /**
     * Text that was removed by the removePattern from the source text.
     */
    private String removedText;

    /** Tokens for original string, with and without stems. */
    private Token[] strTokensStem, strTokensNoStem;

    /** Tokens for original string, includes numbers and tags. */
    private Token[] strTokensAll;

    // This finder used for search separate segment matches
    private FindMatches separateSegmentMatcher;

    private final int fuzzyMatchThreshold;

    private final boolean applyThreshold;

    /**
     * @param searchExactlyTheSame
     *            allows to search similarities with the same text as source
     *            segment. This mode used only for separate sentence match in
     *            paragraph project, i.e. where source is just part of current
     *            source.
     */
    public FindMatches(IProject project, int maxCount, boolean allowSeparateSegmentMatch,
            boolean searchExactlyTheSame) {
        this(project, maxCount, allowSeparateSegmentMatch, searchExactlyTheSame, true);
    }

    public FindMatches(IProject project, int maxCount, boolean allowSeparateSegmentMatch,
            boolean searchExactlyTheSame, boolean applyThreshold) {
        this.project = project;
        this.tok = project.getSourceTokenizer();
        this.srcLocale = project.getProjectProperties().getSourceLanguage().getLocale();
        this.maxCount = maxCount;
        this.searchExactlyTheSame = searchExactlyTheSame;
        if (allowSeparateSegmentMatch && !project.getProjectProperties().isSentenceSegmentingEnabled()) {
            separateSegmentMatcher = new FindMatches(project, 1, false, true);
        }
        this.fuzzyMatchThreshold = Preferences.getPreferenceDefault(Preferences.EXT_TMX_FUZZY_MATCH_THRESHOLD,
                OConsts.FUZZY_MATCH_THRESHOLD);
        this.applyThreshold = applyThreshold;
    }

    public List<NearString> search(String searchText, boolean requiresTranslation, boolean fillSimilarityData,
            IStopped stop) throws StoppedException {
        result = new ArrayList<>(OConsts.MAX_NEAR_STRINGS + 1);
        srcText = searchText;
        removedText = "";
        // remove part that is to be removed according to user settings.
        // Rationale: it might be a big string influencing the 'editing
        // distance', while it is not really part
        // of the translatable text
        if (removePattern != null) {
            StringBuilder removedBuffer = new StringBuilder();
            Matcher removeMatcher = removePattern.matcher(srcText);
            while (removeMatcher.find()) {
                removedBuffer.append(removeMatcher.group());
            }
            srcText = removeMatcher.replaceAll("");
            removedText = removedBuffer.toString();
        }
        // get tokens for original string which includes non-word tokens
        strTokensStem = tokenizeStem(srcText);
        strTokensNoStem = tokenizeNoStem(srcText);
        strTokensAll = tokenizeAll(srcText);

        // travel by project entries, including orphaned
        if (project.getProjectProperties().isSupportDefaultTranslations()) {
            project.iterateByDefaultTranslations((source, trans) -> {
                checkStopped(stop);
                if (!searchExactlyTheSame && source.equals(searchText)) {
                    // skip original==original entry comparison
                    return;
                }
                if (requiresTranslation && trans.translation == null) {
                    return;
                }
                String fileName = project.isOrphaned(source) ? ORPHANED_FILE_NAME : null;
                PrepareTMXEntry entry = new PrepareTMXEntry(trans);
                entry.source = source;
                processEntry(null, entry, fileName, NearString.MATCH_SOURCE.MEMORY, false, 0);
            });
        }
        project.iterateByMultipleTranslations((source, trans) -> {
            checkStopped(stop);
            if (!searchExactlyTheSame && source.sourceText.equals(searchText)) {
                // skip original==original entry comparison
                return;
            }
            if (requiresTranslation && trans.translation == null) {
                return;
            }
            String fileName = project.isOrphaned(source) ? ORPHANED_FILE_NAME : null;
            PrepareTMXEntry entry = new PrepareTMXEntry(trans);
            entry.source = source.sourceText;
            processEntry(source, entry, fileName, NearString.MATCH_SOURCE.MEMORY, false, 0);
        });
        /*
         * Penalty applied for fuzzy matches in another language (if no match in
         * the target language was found).
         */
        int foreignPenalty = Preferences.getPreferenceDefault(Preferences.PENALTY_FOR_FOREIGN_MATCHES,
                Preferences.PENALTY_FOR_FOREIGN_MATCHES_DEFAULT);
        // travel by translation memories
        for (Map.Entry<String, ExternalTMX> en : project.getTransMemories().entrySet()) {
            int penalty = 0;
            Matcher matcher = SEARCH_FOR_PENALTY.matcher(en.getKey());
            if (matcher.find()) {
                penalty = Integer.parseInt(matcher.group(1));
            }
            for (ITMXEntry tmen : en.getValue().getEntries()) {
                checkStopped(stop);
                if (tmen.getSourceText() == null) {
                    // Not all TMX entries have a source; in that case there can
                    // be no meaningful match, so skip.
                    continue;
                }
                if (requiresTranslation && tmen.getTranslationText() == null) {
                    continue;
                }
                int tmenPenalty = penalty;
                if (tmen.hasPropValue(ExternalTMFactory.TMXLoader.PROP_FOREIGN_MATCH, "true")) {
                    tmenPenalty += foreignPenalty;
                }
                processEntry(null, tmen, en.getKey(), NearString.MATCH_SOURCE.TM, false, tmenPenalty);
            }
        }
        // travel by all entries for check source file translations
        for (SourceTextEntry ste : project.getAllEntries()) {
            checkStopped(stop);
            if (ste.getSourceTranslation() != null) {
                PrepareTMXEntry entry = new PrepareTMXEntry();
                entry.source = ste.getSrcText();
                entry.translation = ste.getSourceTranslation();
                processEntry(ste.getKey(), entry, ste.getKey().file, NearString.MATCH_SOURCE.MEMORY,
                        ste.isSourceTranslationFuzzy(), 0);
            }
        }
        if (separateSegmentMatcher != null) {
            // split paragraph even when segmentation disabled, then find
            // matches for every segment
            List<StringBuilder> spaces = new ArrayList<>();
            List<Rule> brules = new ArrayList<>();
            Language sourceLang = project.getProjectProperties().getSourceLanguage();
            Language targetLang = project.getProjectProperties().getTargetLanguage();
            List<String> segments = Core.getSegmenter().segment(sourceLang, srcText, spaces, brules);
            if (segments.size() > 1) {
                List<String> fsrc = new ArrayList<>(segments.size());
                List<String> ftrans = new ArrayList<>(segments.size());
                // multiple segments
                for (String onesrc : segments) {
                    // find match for a separate segment
                    List<NearString> segmentMatch = separateSegmentMatcher.search(onesrc, requiresTranslation,
                            false, stop);
                    if (!segmentMatch.isEmpty()
                            && segmentMatch.get(0).scores[0].score >= SUBSEGMENT_MATCH_THRESHOLD) {
                        fsrc.add(segmentMatch.get(0).source);
                        ftrans.add(segmentMatch.get(0).translation);
                    } else {
                        fsrc.add("");
                        ftrans.add("");
                    }
                }
                // glue found sources and translations
                PrepareTMXEntry entry = new PrepareTMXEntry();
                entry.source = Core.getSegmenter().glue(sourceLang, sourceLang, fsrc, spaces, brules);
                entry.translation = Core.getSegmenter().glue(sourceLang, targetLang, ftrans, spaces, brules);
                processEntry(null, entry, "", NearString.MATCH_SOURCE.TM, false, 0);
            }
        }
        // fill similarity data only for a result
        if (fillSimilarityData) {
            for (NearString near : result) {
                near.attr = FuzzyMatcher.buildSimilarityData(strTokensAll, tokenizeAll(near.source));
            }
        }
        return result;
    }

    /**
     * Compare one entry with the original entry.
     *
     * @param key
     *            entry to compare
     * @param entry
     *            PrepareTMXEntry entry to process.
     * @param comesFrom
     *            match source
     * @param fuzzy
     *            is it fuzzy or not
     * @param penalty
     *            penalty score
     * @param tmxName
     *            tmx name
     */
    public void processEntry(EntryKey key, ITMXEntry entry, String tmxName,
                              NearString.MATCH_SOURCE comesFrom, boolean fuzzy, int penalty) {
        // remove part that is to be removed prior to tokenize
        String realSource = entry.getSourceText();
        int realPenaltyForRemoved = 0;
        if (removePattern != null) {
            StringBuilder entryRemovedText = new StringBuilder();
            Matcher removeMatcher = removePattern.matcher(realSource);
            while (removeMatcher.find()) {
                entryRemovedText.append(removeMatcher.group());
            }
            realSource = removeMatcher.replaceAll("");
            // calculate penalty if something has been removed, otherwise
            // different strings get 100% match.
            if (!entryRemovedText.toString().equals(removedText)) {
                // penalty for different 'removed'-part
                realPenaltyForRemoved = PENALTY_FOR_REMOVED;
            }
        }

        Token[] candTokens = tokenizeStem(realSource);

        // First percent value - with stemming if possible
        int similarityStem = FuzzyMatcher.calcSimilarity(distance, strTokensStem, candTokens);

        similarityStem -= penalty;
        if (fuzzy) {
            // penalty for fuzzy
            similarityStem -= PENALTY_FOR_FUZZY;
        }
        similarityStem -= realPenaltyForRemoved;

        // check if we have a chance by first percentage only
        if (noChanceToAdd(similarityStem, Integer.MAX_VALUE, Integer.MAX_VALUE)) {
            return;
        }

        Token[] candTokensNoStem = tokenizeNoStem(realSource);
        // Second percent value - without stemming
        int similarityNoStem = FuzzyMatcher.calcSimilarity(distance, strTokensNoStem, candTokensNoStem);
        similarityNoStem -= penalty;
        if (fuzzy) {
            // penalty for fuzzy
            similarityNoStem -= PENALTY_FOR_FUZZY;
        }
        similarityNoStem -= realPenaltyForRemoved;

        // check if we have a chance by first and second percentages
        if (noChanceToAdd(similarityStem, similarityNoStem, Integer.MAX_VALUE)) {
            return;
        }

        Token[] candTokensAll = tokenizeAll(realSource);
        // Third percent value - with numbers, tags, etc.
        int simAdjusted = FuzzyMatcher.calcSimilarity(distance, strTokensAll, candTokensAll);
        simAdjusted -= penalty;
        if (fuzzy) {
            // penalty for fuzzy
            simAdjusted -= PENALTY_FOR_FUZZY;
        }
        simAdjusted -= realPenaltyForRemoved;

        // check if we have chance by first, second and third percentages
        if (noChanceToAdd(similarityStem, similarityNoStem, simAdjusted)) {
            return;
        }

        // BUGS#1236 - stat display does not use threshold config check
        if (applyThreshold && similarityStem < fuzzyMatchThreshold
                && similarityNoStem < fuzzyMatchThreshold && simAdjusted < fuzzyMatchThreshold) {
            return;
        }

        addNearString(key, entry, comesFrom, fuzzy, new NearString.Scores(similarityStem, similarityNoStem,
                simAdjusted), tmxName);
    }

    /**
     * Check if entries have a chance to be added to a result list. If true,
     * there is no sense to calculate other parameters.
     *
     * @param simStem
     *            similarity with stemming
     * @param simNoStem
     *            similarity without stemming
     * @param simExactly
     *            exactly similarity
     * @return true if we have no chance.
     */
    private boolean noChanceToAdd(int simStem, int simNoStem, int simExactly) {
        if (result.size() < maxCount) {
            return false;
        }
        NearString st = result.get(result.size() - 1);
        int chance = Integer.compare(st.scores[0].score, simStem);
        if (chance == 0) {
            chance = Integer.compare(st.scores[0].scoreNoStem, simNoStem);
        }
        if (chance == 0) {
            chance = Integer.compare(st.scores[0].adjustedScore, simExactly);
        }
        return chance == 1;
    }

    /**
     * Add near string into the result list. Near strings sorted by "similarity,
     * simAdjusted"
     */
    private void addNearString(EntryKey key, ITMXEntry entry, NearString.MATCH_SOURCE comesFrom, boolean fuzzy,
                               NearString.Scores scores, String tmxName) {
        final String source = entry.getSourceText();
        final String translation = entry.getTranslationText();
        // find position for new data
        int pos = 0;
        for (int i = 0; i < result.size(); i++) {
            NearString st = result.get(i);
            if (source.equals(st.source) && Objects.equals(translation, st.translation)) {
                // Consolidate identical matches from different sources into a
                // single NearString with multiple project entries.
                result.set(i, NearString.merge(st, key, entry, comesFrom, fuzzy, scores, null, tmxName));
                return;
            }
            if (st.scores[0].score < scores.score) {
                break;
            }
            if (st.scores[0].score == scores.score) {
                if (st.scores[0].scoreNoStem < scores.scoreNoStem) {
                    break;
                }
                if (st.scores[0].scoreNoStem == scores.scoreNoStem) {
                    if (st.scores[0].adjustedScore < scores.adjustedScore) {
                        break;
                    }
                    // Patch contributed by Antonio Vilei
                    // text with the same case has precedence
                    if (scores.score == 100 && !st.source.equals(srcText) && source.equals(srcText)) {
                        break;
                    }
                }
            }
            pos = i + 1;
        }
        result.add(pos, new NearString(key, entry, comesFrom, fuzzy, scores, null, tmxName));
        if (result.size() > maxCount) {
            result.remove(result.size() - 1);
        }
    }

    /*
     * Methods for tokenize strings with caching.
     */
    Map<String, Token[]> tokenizeStemCache = new HashMap<>();
    Map<String, Token[]> tokenizeNoStemCache = new HashMap<>();
    Map<String, Token[]> tokenizeAllCache = new HashMap<>();

    Token[] tokenizeStem(String str) {
        Token[] tokens = tokenizeStemCache.get(str);
        if (tokens == null) {
            tokens = tok.tokenizeWords(str, ITokenizer.StemmingMode.MATCHING);
            tokenizeStemCache.put(str, tokens);
        }
        return tokens;
    }

    Token[] tokenizeNoStem(String str) {
        // No-stemming token comparisons are intentionally case-insensitive
        // for matching purposes.
        str = str.toLowerCase(srcLocale);
        Token[] tokens = tokenizeNoStemCache.get(str);
        if (tokens == null) {
            tokens = tok.tokenizeWords(str, ITokenizer.StemmingMode.NONE);
            tokenizeNoStemCache.put(str, tokens);
        }
        return tokens;
    }

    Token[] tokenizeAll(String str) {
        // Verbatim token comparisons are intentionally case-insensitive.
        // for matching purposes.
        str = str.toLowerCase(srcLocale);
        Token[] tokens = tokenizeAllCache.get(str);
        if (tokens == null) {
            tokens = tok.tokenizeVerbatim(str);
            tokenizeAllCache.put(str, tokens);
        }
        return tokens;
    }

    private void checkStopped(IStopped stop) throws StoppedException {
        if (stop.isStopped()) {
            throw new StoppedException();
        }
    }

    /**
     * The Process will throw this exception if it stopped. All callers must
     * catch it and just skip.
     */
    @SuppressWarnings("serial")
    public static class StoppedException extends RuntimeException {
    }
}
