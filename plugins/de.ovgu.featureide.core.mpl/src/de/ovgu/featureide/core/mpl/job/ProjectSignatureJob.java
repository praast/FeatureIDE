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
package de.ovgu.featureide.core.mpl.job;

import de.ovgu.featureide.core.mpl.signature.ProjectSignatures.SignatureIterator;
import de.ovgu.featureide.core.mpl.signature.ProjectStructure;
import de.ovgu.featureide.core.mpl.signature.filter.ISignatureFilter;
import de.ovgu.featureide.core.mpl.signature.fuji.FujiClassCreator;

/**
 * Constructs a {@link ProjectStructure}.
 * 
 * @author Sebastian Krieter
 */
public class ProjectSignatureJob extends AChainJob {
	private final ISignatureFilter filter;
	private final ProjectStructure projectSig;

	public ProjectSignatureJob(ProjectStructure projectSig, ISignatureFilter filter) {
		super("Loading Project Signature");
		this.filter = filter;
		this.projectSig = projectSig;
	}
	
	@Override
	protected boolean work() {
		SignatureIterator it = interfaceProject.getProjectSignatures().createIterator();
		it.addFilter(filter);
		projectSig.construct(it, new FujiClassCreator());
		return true;
	}
}
