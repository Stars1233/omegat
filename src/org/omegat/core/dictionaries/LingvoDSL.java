/**************************************************************************
 OmegaT - Computer Assisted Translation (CAT) tool
          with fuzzy matching, translation memory, keyword search,
          glossaries, and translation leveraging into updated projects.

 Copyright (C) 2010 Alex Buloichik
               2015 Aaron Madlon-Kay
               2021 Aaron Madlon-Kay, Dmitri Gabinski, Hiroshi Miura
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

package org.omegat.core.dictionaries;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import tokyo.northside.dsl4j.DslArticle;
import tokyo.northside.dsl4j.DslDictionary;
import tokyo.northside.dsl4j.DslResult;
import tokyo.northside.dsl4j.data.LanguageCode;
import tokyo.northside.dsl4j.data.LanguageName;
import tokyo.northside.dsl4j.visitor.DslVisitor;

import org.omegat.core.Core;
import org.omegat.core.CoreEvents;
import org.omegat.core.events.IApplicationEventListener;
import org.omegat.util.Preferences;

/**
 * Dictionary implementation for Lingvo DSL format.
 * <p>
 * Lingvo DSL format described in Lingvo help. See also links below.
 *
 * @author Alex Buloichik (alex73mail@gmail.com)
 * @author Aaron Madlon-Kay
 * @author Hiroshi Miura
 * @see <a href="https://github.com/eb4j/dsl4j">DSL4j library</a>
 * @see <a href="http://lingvo.helpmax.net/en/troubleshooting/dsl-compiler/">DSL
 *      Documentation (English)</a>
 * @see <a href="http://www.dsleditor.narod.ru/art_03.htm">DSL documentation
 *      (Russian)</a>
 */
public class LingvoDSL implements IDictionaryFactory {

    /**
     * Plugin loader.
     */
    public static void loadPlugins() {
        CoreEvents.registerApplicationEventListener(new LingvoDSLApplicationEventListener());
    }

    /**
     * Plugin unloader.
     */
    public static void unloadPlugins() {
    }

    /**
     * registration of dictionary factory.
     */
    static class LingvoDSLApplicationEventListener implements IApplicationEventListener {
        @Override
        public void onApplicationStartup() {
            Core.getDictionaries().addDictionaryFactory(new LingvoDSL());
        }

        @Override
        public void onApplicationShutdown() {
        }
    }

    @Override
    public final boolean isSupportedFile(final File file) {
        return file.getPath().endsWith(".dsl") || file.getPath().endsWith(".dsl.dz");
    }

    @Override
    public final IDictionary loadDict(final File file) throws Exception {
        Path dictPath = Paths.get(file.toURI());
        Path indexPath = Paths.get(dictPath + ".idx");
        return new LingvoDSLDict(dictPath, indexPath, false);
    }

    static class LingvoDSLDict implements IDictionary {
        private final Path dictPath;
        private final Path indexPath;
        private final boolean validateIndexAbsPath;

        private DslDictionary data;
        private HtmlVisitor htmlVisitor;

        /**
         * Initialize LingvoDSL Dictionary driver.
         * 
         * @param dictPath
         *            *.dsl file object.
         * @param indexPath
         *            index cache file.
         * @throws Exception
         *             when loading dictionary failed.
         */
        LingvoDSLDict(final Path dictPath, final Path indexPath, final boolean validateIndexAbsPath) {
            this.dictPath = dictPath;
            this.indexPath = indexPath;
            this.validateIndexAbsPath = validateIndexAbsPath;
        }

        private void loadDictionary() throws IOException {
            data = DslDictionary.loadDictionary(dictPath, indexPath, validateIndexAbsPath);
            htmlVisitor = new HtmlVisitor(dictPath.getParent().toString(),
                    Preferences.isPreferenceDefault(Preferences.DICTIONARY_CONDENSED_VIEW, false));
        }

        /**
         * read article with exact match.
         * 
         * @param word
         *            The word to look up in the dictionary
         *
         * @return list of results.
         */
        @Override
        public List<DictionaryEntry> readArticles(final String word) throws IOException {
            return readEntries(word, lookup(word));
        }

        /**
         * read article with predictive match.
         * 
         * @param word
         *            The word to look up in the dictionary
         *
         * @return list of results.
         */
        @Override
        public List<DictionaryEntry> readArticlesPredictive(final String word) throws IOException {
            return readEntries(word, lookupPredictive(word));
        }

        private List<DictionaryEntry> readEntries(final String word, final DslResult dslResult) {
            List<DictionaryEntry> list = new ArrayList<>();
            for (Map.Entry<String, String> e : dslResult.getEntries(htmlVisitor)) {
                DictionaryEntry dictionaryEntry = new DictionaryEntry(word, e.getKey(), e.getValue());
                list.add(dictionaryEntry);
            }
            return list;
        }

        DslResult lookup(final String word) throws IOException {
            if (data == null) {
                try {
                    loadDictionary();
                } catch (Exception e) {
                    return new DslResult(Collections.emptyList());
                }
            }
            return data.lookup(word);
        }

        DslResult lookupPredictive(final String word) throws IOException {
            if (data == null) {
                try {
                    loadDictionary();
                } catch (Exception e) {
                    return new DslResult(Collections.emptyList());
                }
            }
            return data.lookupPredictive(word);
        }
    }

    /**
     * Simple HTML filter for LingvoDSL parser.
     */
    public static class HtmlVisitor extends DslVisitor<String> {

        private static final String[] IMAGE_EXTS = new String[] { "png", "jpg", "PNG", "JPG" };

        private final boolean condensedView;
        private final File basePath;

        private StringBuilder sb;
        private boolean delayText;
        private String previousText;
        private boolean inDetails;

        /**
         * Constructor with media path.
         * 
         * @param dirPath
         *            media base path.
         * @throws IOException
         *             when given directory not found.
         */
        public HtmlVisitor(final String dirPath, final boolean condensedView) throws IOException {
            File dir = new File(dirPath);
            if (!dir.isDirectory()) {
                throw new IOException("Directory not found!");
            }
            basePath = dir;
            delayText = false;
            inDetails = false;
            this.condensedView = condensedView;
        }

        /**
         * Start of accept.
         * <p>
         * super#visit(ElementSequence) call this.
         * </p>
         */
        @Override
        public void start() {
            sb = new StringBuilder();
        }

        /**
         * End of accept.
         * <p>
         * super#visit(ElementSequence) call this.
         * </p>
         */
        @Override
        public void finish() {
        }

        /**
         * Visit a tag.
         *
         * @param tag
         *            to visit.
         */
        @Override
        public void visit(final DslArticle.Tag tag) {
            if (inDetails && condensedView) {
                return;
            }
            if (tag.isTagName("b")) {
                sb.append("<strong>");
            } else if (tag.isTagName("br")) {
                sb.append("<br/>");
            } else if (tag.isTagName("u")) {
                sb.append("<span style='text-decoration:underline'>");
            } else if (tag.isTagName("i")) {
                sb.append("<span style='font-style: italic'>");
            } else if (tag.isTagName("sup")) {
                sb.append("<sup>");
            } else if (tag.isTagName("sub")) {
                sb.append("<sub>");
            } else if (tag.isTagName("c")) {
                if (tag.hasAttribute()) {
                    sb.append("<span style=\"color: ").append(tag.getAttribute().getValue()).append("\">");
                } else {
                    sb.append("<span style=\"color: green\">");
                }
            } else if (tag.isTagName("'")) {
                sb.append("<span style=\"color: red\">");
            } else if (tag.isTagName("url") || tag.isTagName("s") || tag.isTagName("video")) {
                delayText = true;
            } else if (tag.isTagName("lang")) {
                if (tag.hasAttribute() && tag.getAttribute().getKey().equals("id")) {
                    int i = Integer.parseInt(tag.getAttribute().getValue());
                    if (LanguageCode.containsCode(i)) {
                        sb.append("<span class=\"lang_").append(LanguageCode.getLanguageCode(i)).append("\">");
                        return;
                    }
                } else if (tag.hasAttribute() && tag.getAttribute().getKey().equals("name")
                        && LanguageName.containsLanguage(tag.getAttribute().getValue())) {
                    sb.append("<span class=\"lang_").append(LanguageName.getLanguageCode(tag.getAttribute().getValue()))
                            .append("\">");
                    return;
                }
                sb.append("<span>");
            } else if (tag.isTagName("*")) {
                inDetails = true;
                if (!condensedView) {
                    sb.append("<span class=\"details\">");
                }
            } else {
                if (condensedView) {
                    if (tag.isTagName("m")) {
                        sb.append("<span>");
                    } else if (tag.isTagName("m1")) {
                        sb.append("<span class=\"paragraph-start\">\u00b6</span><span>");
                    } else if (tag.isTagName("m2")) {
                        sb.append("<span class=\"paragraph-start\">\u204b</span><span>");
                    } else if (tag.isTagName("m3") || tag.isTagName("m4") || tag.isTagName("m5")
                            || tag.isTagName("m6") || tag.isTagName("m7") || tag.isTagName("m8")
                            || tag.isTagName("m9")) {
                        sb.append("<span class=\"paragraph-start\">\u00a7</span><span>");
                    }
                } else {
                    if (tag.isTagName("m")) {
                        sb.append("<div>");
                    } else if (tag.isTagName("m1")) {
                        sb.append("<div style=\"text-indent: 30px\">");
                    } else if (tag.isTagName("m2")) {
                        sb.append("<div style=\"text-indent: 60px\">");
                    } else if (tag.isTagName("m3")) {
                        sb.append("<div style=\"text-indent: 90px\">");
                    } else if (tag.isTagName("m4")) {
                        sb.append("<div style=\"text-indent: 90px\">");
                    } else if (tag.isTagName("m5")) {
                        sb.append("<div style=\"text-indent: 90px\">");
                    } else if (tag.isTagName("m6")) {
                        sb.append("<div style=\"text-indent: 90px\">");
                    } else if (tag.isTagName("m7")) {
                        sb.append("<div style=\"text-indent: 90px\">");
                    } else if (tag.isTagName("m8")) {
                        sb.append("<div style=\"text-indent: 90px\">");
                    } else if (tag.isTagName("m9")) {
                        sb.append("<div style=\"text-indent: 90px\">");
                    }
                }
            }
            // no output for t
        }

        private String getMediaUrl() {
            return new File(basePath, previousText).toURI().toString();
        }

        private boolean isMediaImage() {
            String ext = FilenameUtils.getExtension(previousText);
            for (String e : IMAGE_EXTS) {
                if (e.equals(ext)) {
                    return true;
                }
            }
            return false;
        }

        /**
         * Visit an EndTag.
         *
         * @param tag to visit.
         */
        @Override
        public void visit(final DslArticle.EndTag tag) {
            if (!handleDetailsEnd(tag) && !handleDelayedText(tag)) {
                appendFormattingTags(tag);
            }
        }

        private boolean handleDetailsEnd(final DslArticle.EndTag tag) {
            if (tag.isTagName("*")) {
                inDetails = false;
                if (!condensedView) {
                    sb.append("</span>");
                }
                return true;
            }
            return inDetails && condensedView;
        }

        private boolean handleDelayedText(final DslArticle.EndTag tag) {
            if (!delayText || previousText == null) {
                return false;
            }
            if (tag.isTagName("video")) {
                appendLink(getMediaUrl(), previousText);
            } else if (tag.isTagName("s")) {
                if (isMediaImage()) {
                    sb.append("<img src=\"").append(getMediaUrl()).append("\" />");
                } else {
                    appendLink(getMediaUrl(), previousText);
                }
            } else if (tag.isTagName("url")) {
                appendLink(previousText, previousText);
            }
            delayText = false;
            previousText = null;
            return true;
        }

        private void appendFormattingTags(final DslArticle.EndTag endTag) {
            if (endTag.isTagName("b")) {
                sb.append("</strong>");
            } else if (endTag.isTagName("u") || endTag.isTagName("i") || endTag.isTagName("c")
                    || endTag.isTagName("'") || endTag.isTagName("lang")) {
                sb.append("</span>");
            } else if (endTag.isTagName("t")) {
                sb.append("&nbsp;");
            } else if (endTag.isTagName("sup")) {
                sb.append("</sup>");
            } else if (endTag.isTagName("sub")) {
                sb.append("</sub>");
            } else if (endTag.isTagName("m")) {
                if (condensedView) {
                    sb.append("</span>&nbsp;");
                } else {
                    sb.append("</div>");
                }
            }
        }

        private void appendLink(final String url, final String text) {
            if (isMediaImage()) {
                sb.append("<img src=\"").append(url).append("\" />");
            } else {
                sb.append("<a href=\"").append(url).append("\">").append(text).append("</a>");
            }
        }

        /**
         * Return result.
         *
         * @return result.
         */
        @Override
        public String getObject() {
            if (sb == null) {
                // should not happened, but check null to avoid findbugs error.
                throw new IllegalStateException("sb is null in getObject() method!");
            }
            return sb.toString();
        }

        /**
         * Visit a text.
         *
         * @param t
         *            Text object to process.
         */
        @Override
        public void visit(final DslArticle.Text t) {
            if (inDetails && condensedView) {
                return;
            }
            previousText = t.getText();
            if (!delayText) {
                sb.append(t);
            }
        }

        /**
         * Visit an Attribute.
         *
         * @param a
         *            Attribute object to visit.
         */
        @Override
        public void visit(final DslArticle.Attribute a) {
        }

        /**
         * Visit a NewLine.
         *
         * @param n
         *            NewLine object to visit.
         */
        @Override
        public void visit(final DslArticle.Newline n) {
            if (inDetails && condensedView) {
                return;
            }
            sb.append("\n");
        }
    }
}
