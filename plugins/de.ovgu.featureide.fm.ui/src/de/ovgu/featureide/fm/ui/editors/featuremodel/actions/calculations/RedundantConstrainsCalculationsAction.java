/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2013  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 * 
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://www.fosd.de/featureide/ for further information.
 */
package de.ovgu.featureide.fm.ui.editors.featuremodel.actions.calculations;

import org.eclipse.gef.ui.parts.GraphicalViewerImpl;
import org.eclipse.jface.action.Action;

import de.ovgu.featureide.fm.core.FeatureModel;

/**
 * 
 * @author Jens Meinicke
 */
public class RedundantConstrainsCalculationsAction extends Action {

	private final FeatureModel featureModel;
	
	public RedundantConstrainsCalculationsAction(GraphicalViewerImpl viewer, FeatureModel featureModel) {
		super("Calculate Redundant Constraints");
		this.featureModel = featureModel;	
		setChecked(featureModel.calculateRedundantConstraints);
	}

	@Override
	public void run() {
		if (featureModel.calculateRedundantConstraints) {
			featureModel.calculateRedundantConstraints = false;
		} else {
			featureModel.calculateRedundantConstraints = true;
			featureModel.calculateFeatures = true;
			featureModel.calculateConstraints = true;
		}
		featureModel.handleModelDataChanged();
	}
		
}
