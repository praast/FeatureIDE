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
package de.ovgu.featureide.fm.ui.editors.featuremodel;

import org.eclipse.draw2d.geometry.Point;

import de.ovgu.featureide.fm.core.FeatureModel;

/**
 * TODO Represents the legend in the FeatureModel
 * 
 * @author Fabian Benduhn
 */
public class Legend {
	Point pos;
	FeatureModel model;

	public Legend(FeatureModel model) {
		this.model = model;

		this.pos = new Point(model.getLegendPos().x, model.getLegendPos().y);
	}

	public FeatureModel getModel(){
		return model;
	}
	public void update() {
		model.redrawDiagram();

	}

	public Point getPos() {
		return pos;
	}

	public void setPos(Point pos) {
		this.pos = pos;
		model.setLegendPos(pos.x, pos.y);

	}

	/**
	 * 
	 */
	public void changeVisibility() {
		// TODO Auto-generated method stub
		
	}

}
