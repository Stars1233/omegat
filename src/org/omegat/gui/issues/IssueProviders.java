/**************************************************************************
 OmegaT - Computer Assisted Translation (CAT) tool 
          with fuzzy matching, translation memory, keyword search, 
          glossaries, and translation leveraging into updated projects.

 Copyright (C) 2016 Aaron Madlon-Kay
               Home page: http://www.omegat.org/
               Support center: http://groups.yahoo.com/group/OmegaT/

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
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 **************************************************************************/

package org.omegat.gui.issues;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.omegat.util.Preferences;

/**
 * A class for aggregating issue providers. Extensions and scripts can add their
 * providers here with {@link #addIssueProvider(IIssueProvider)}.
 * 
 * @author Aaron Madlon-Kay
 *
 */
public class IssueProviders {

    static final String ISSUE_IDS_DELIMITER = ",";

    static final List<IIssueProvider> ISSUE_PROVIDERS = new ArrayList<>();
    static {
        addIssueProvider(new SpellingIssueProvider());
    }

    private IssueProviders() {
    }

    public static void addIssueProvider(IIssueProvider provider) {
        ISSUE_PROVIDERS.add(provider);
    }

    static Set<String> getDisabledProviderIds() {
        String disabled = Preferences.getPreference(Preferences.ISSUE_PROVIDERS_DISABLED);
        return Stream.of(disabled.split(ISSUE_IDS_DELIMITER)).collect(Collectors.toSet());
    }

    static List<IIssueProvider> getEnabledProviders() {
        Set<String> disabled = getDisabledProviderIds();
        return ISSUE_PROVIDERS.stream().filter(p -> !disabled.contains(p.getId())).collect(Collectors.toList());
    }

    public static void setProviderEnabled(String id, boolean enabled) {
        Set<String> disabled = getDisabledProviderIds();
        if (enabled) {
            disabled.remove(id);
        } else {
            disabled.add(id);
        }
        Preferences.setPreference(Preferences.ISSUE_PROVIDERS_DISABLED, String.join(ISSUE_IDS_DELIMITER, disabled));
    }
}