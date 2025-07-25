/**************************************************************************
 OmegaT - Computer Assisted Translation (CAT) tool
          with fuzzy matching, translation memory, keyword search,
          glossaries, and translation leveraging into updated projects.

 Copyright (C) 2000-2006 Keith Godfrey and Maxym Mykhalchuk
               2009 Didier Briel
               2011 John Moran, Didier Briel
               2012 Didier Briel
               2016 Aaron Madlon-Kay
               2019 Briac Pilpre
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

package org.omegat.gui.preferences.view;

import static org.openide.awt.Mnemonics.setLocalizedText;

import javax.swing.JComponent;

import org.omegat.gui.editor.SegmentBuilder;
import org.omegat.gui.preferences.BasePreferencesController;
import org.omegat.util.OStrings;
import org.omegat.util.Preferences;

/**
 * @author Maxym Mykhalchuk
 * @author Didier Briel
 * @author John Moran
 * @author Aaron Madlon-Kay
 */
public class EditingBehaviorController extends BasePreferencesController {

    private EditingBehaviorPanel panel;

    @Override
    public JComponent getGui() {
        if (panel == null) {
            initGui();
            initFromPrefs();
        }
        return panel;
    }

    @Override
    public String toString() {
        return OStrings.getString("PREFS_TITLE_EDITING_BEHAVIOR");
    }

    private void initGui() {
        panel = new EditingBehaviorPanel();
        initLabels();
        panel.insertFuzzyCheckBox.addActionListener(e -> {
            panel.similarityLabel.setEnabled(panel.insertFuzzyCheckBox.isSelected());
            panel.similaritySpinner.setEnabled(panel.insertFuzzyCheckBox.isSelected());
            panel.prefixLabel.setEnabled(panel.insertFuzzyCheckBox.isSelected());
            panel.prefixText.setEnabled(panel.insertFuzzyCheckBox.isSelected());
        });
    }

    /**
     * Set localized labels from Bundle.
     */
    private void initLabels() {
        panel.descriptionTextArea.setText(OStrings.getString("GUI_WORKFLOW_DESCRIPTION"));
        setLocalizedText(panel.defaultRadio, OStrings.getString("WF_OPTION_INSERT_SOURCE"));
        setLocalizedText(panel.leaveEmptyRadio, OStrings.getString("WF_OPTION_INSERT_NOTHTHING"));
        setLocalizedText(panel.insertFuzzyCheckBox, OStrings.getString("WF_OPTION_INSERT_FUZZY_MATCH"));
        setLocalizedText(panel.similarityLabel,
                OStrings.getString("GUI_WORKFLOW_OPTION_Minimal_Similarity"));
        setLocalizedText(panel.prefixLabel, OStrings.getString("WF_OPTION_INSERT_FUZZY_PREFIX"));
        setLocalizedText(panel.convertNumbers, OStrings.getString("WF_OPTION_REPLACE_NUMBERS"));
        setLocalizedText(panel.allowTranslationEqualToSource, OStrings.getString(
                "WF_OPTION_ALLOW_TRANS_EQ_TO_SRC"));
        setLocalizedText(panel.exportCurrentSegment, OStrings.getString("WF_OPTION_EXPORT__CURRENT_SEGMENT"));
        setLocalizedText(panel.stopOnAlternativeTranslation, OStrings.getString(
                "WF_OPTION_GOTO_NEXT_UNTRANSLATED"));
        setLocalizedText(panel.allowTagEditing, OStrings.getString("WF_TAG_EDITING"));
        setLocalizedText(panel.tagValidateOnLeave, OStrings.getString("WG_TAG_VALIDATE_ON_LEAVE"));
        setLocalizedText(panel.cbSaveAutoStatus, OStrings.getString("WG_SAVE_AUTO_STATUS"));
        setLocalizedText(panel.cbSaveOrigin, OStrings.getString("WG_SAVE_ORIGIN"));
        setLocalizedText(panel.initialSegCountLabel, OStrings.getString("WG_INITIAL_SEGMENT_LOAD_COUNT"));
        panel.initialSegCountSpinner.setToolTipText(OStrings.getString(
                "WG_INITIAL_SEGMENT_LOAD_COUNT_TOOLTIP"));
        setLocalizedText(panel.paraMarkLabel, OStrings.getString("WG_PARA_MARK"));
        setLocalizedText(panel.singleClickActivationCheckBox, OStrings.getString(
                "WF_OPTION_ALLOW_SELECT_SINGLE_CLICK"));
    }

    @Override
    protected void initFromPrefs() {
        // Double negation? then prefInsertSource is the contrary of Preferences.DONT_INSERT_SOURCE_TEXT 
        boolean prefInsertSource = !Preferences.isPreferenceDefault(Preferences.DONT_INSERT_SOURCE_TEXT,
                SegmentBuilder.DONT_INSERT_SOURCE_TEXT_DEFAULT);
        panel.defaultRadio.setSelected(prefInsertSource);
        panel.leaveEmptyRadio.setSelected(!prefInsertSource);

        panel.insertFuzzyCheckBox.setSelected(Preferences.isPreferenceDefault(Preferences.BEST_MATCH_INSERT, true));
        panel.similaritySpinner.setValue(Preferences.getPreferenceDefault(Preferences.BEST_MATCH_MINIMAL_SIMILARITY,
                Preferences.BEST_MATCH_MINIMAL_SIMILARITY_DEFAULT));
        if (!Preferences.existsPreference(Preferences.BEST_MATCH_EXPLANATORY_TEXT)) {
            panel.prefixText.setText(OStrings.getString("WF_DEFAULT_PREFIX"));
        } else {
            panel.prefixText.setText(Preferences.getPreference(Preferences.BEST_MATCH_EXPLANATORY_TEXT));
        }

        panel.allowTranslationEqualToSource.setSelected(
                Preferences.isPreferenceDefault(Preferences.ALLOW_TRANS_EQUAL_TO_SRC, true));
        panel.singleClickActivationCheckBox.setSelected(
                Preferences.isPreferenceDefault(Preferences.SINGLE_CLICK_SEGMENT_ACTIVATION, false));
        panel.exportCurrentSegment.setSelected(Preferences.isPreference(Preferences.EXPORT_CURRENT_SEGMENT));
        panel.stopOnAlternativeTranslation
                .setSelected(Preferences.isPreference(Preferences.STOP_ON_ALTERNATIVE_TRANSLATION));
        panel.convertNumbers.setSelected(Preferences.isPreferenceDefault(Preferences.CONVERT_NUMBERS, true));
        panel.allowTagEditing.setSelected(Preferences.isPreference(Preferences.ALLOW_TAG_EDITING));
        panel.tagValidateOnLeave.setSelected(Preferences.isPreference(Preferences.TAG_VALIDATE_ON_LEAVE));
        panel.cbSaveAutoStatus.setSelected(Preferences.isPreference(Preferences.SAVE_AUTO_STATUS));
        panel.cbSaveOrigin.setSelected(Preferences.isPreference(Preferences.SAVE_ORIGIN));
        panel.initialSegCountSpinner.setValue(Preferences.getPreferenceDefault(
                Preferences.EDITOR_INITIAL_SEGMENT_LOAD_COUNT, Preferences.EDITOR_INITIAL_SEGMENT_LOAD_COUNT_DEFAULT));
        panel.paraMarkText.setText(Preferences.getPreferenceDefault(
                Preferences.MARK_PARA_TEXT, Preferences.MARK_PARA_TEXT_DEFAULT));
        updateEnabledness();
    }

    @Override
    public void restoreDefaults() {
        panel.defaultRadio.setSelected(!SegmentBuilder.DONT_INSERT_SOURCE_TEXT_DEFAULT);
        panel.leaveEmptyRadio.setSelected(SegmentBuilder.DONT_INSERT_SOURCE_TEXT_DEFAULT);

        panel.insertFuzzyCheckBox.setSelected(true);
        panel.similaritySpinner.setValue(Preferences.BEST_MATCH_MINIMAL_SIMILARITY_DEFAULT);
        panel.prefixText.setText(OStrings.getString("WF_DEFAULT_PREFIX"));

        panel.allowTranslationEqualToSource.setSelected(true);
        panel.singleClickActivationCheckBox.setSelected(false);
        panel.exportCurrentSegment.setSelected(false);
        panel.stopOnAlternativeTranslation.setSelected(false);
        panel.convertNumbers.setSelected(true);
        panel.allowTagEditing.setSelected(false);
        panel.tagValidateOnLeave.setSelected(false);
        panel.cbSaveAutoStatus.setSelected(false);
        panel.cbSaveOrigin.setSelected(false);
        panel.initialSegCountSpinner.setValue(Preferences.EDITOR_INITIAL_SEGMENT_LOAD_COUNT_DEFAULT);
        panel.paraMarkText.setText(Preferences.MARK_PARA_TEXT_DEFAULT);

        updateEnabledness();
    }

    private void updateEnabledness() {
        panel.similarityLabel.setEnabled(panel.insertFuzzyCheckBox.isSelected());
        panel.similaritySpinner.setEnabled(panel.insertFuzzyCheckBox.isSelected());
        panel.prefixLabel.setEnabled(panel.insertFuzzyCheckBox.isSelected());
        panel.prefixText.setEnabled(panel.insertFuzzyCheckBox.isSelected());
    }

    @Override
    public void persist() {
        Preferences.setPreference(Preferences.DONT_INSERT_SOURCE_TEXT, panel.leaveEmptyRadio.isSelected());

        Preferences.setPreference(Preferences.BEST_MATCH_INSERT, panel.insertFuzzyCheckBox.isSelected());
        if (panel.insertFuzzyCheckBox.isSelected()) {
            int val = Math.max(0, Math.min(100, (Integer) panel.similaritySpinner.getValue()));
            Preferences.setPreference(Preferences.BEST_MATCH_MINIMAL_SIMILARITY, val);
            Preferences.setPreference(Preferences.BEST_MATCH_EXPLANATORY_TEXT, panel.prefixText.getText());
        }

        Preferences.setPreference(Preferences.ALLOW_TRANS_EQUAL_TO_SRC,
                panel.allowTranslationEqualToSource.isSelected());
        Preferences.setPreference(Preferences.EXPORT_CURRENT_SEGMENT, panel.exportCurrentSegment.isSelected());
        Preferences.setPreference(Preferences.SINGLE_CLICK_SEGMENT_ACTIVATION,
                panel.singleClickActivationCheckBox.isSelected());
        Preferences.setPreference(Preferences.STOP_ON_ALTERNATIVE_TRANSLATION,
                panel.stopOnAlternativeTranslation.isSelected());
        Preferences.setPreference(Preferences.CONVERT_NUMBERS, panel.convertNumbers.isSelected());
        Preferences.setPreference(Preferences.ALLOW_TAG_EDITING, panel.allowTagEditing.isSelected());
        Preferences.setPreference(Preferences.TAG_VALIDATE_ON_LEAVE, panel.tagValidateOnLeave.isSelected());
        Preferences.setPreference(Preferences.SAVE_AUTO_STATUS, panel.cbSaveAutoStatus.isSelected());
        Preferences.setPreference(Preferences.SAVE_ORIGIN, panel.cbSaveOrigin.isSelected());

        int segCount = Math.max(0, (Integer) panel.initialSegCountSpinner.getValue());
        Preferences.setPreference(Preferences.EDITOR_INITIAL_SEGMENT_LOAD_COUNT, segCount);
        Preferences.setPreference(Preferences.MARK_PARA_TEXT, panel.paraMarkText.getText());
    }
}
