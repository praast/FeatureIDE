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
package de.ovgu.featureide.featurehouse;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.sat4j.specs.TimeoutException;

import composer.rules.meta.FeatureModelInfo;
import de.ovgu.featureide.fm.core.Feature;
import de.ovgu.featureide.fm.core.FeatureDependencies;
import de.ovgu.featureide.fm.core.FeatureModel;
import de.ovgu.featureide.fm.core.FeatureModelAnalyzer;

/**
 * TODO description
 * 
 * @author Matthias Praast
 */
public class FeatureIDEModelInfo implements FeatureModelInfo {
	
	private FeatureModel featureModel;
	private FeatureModelAnalyzer featureModelAnalyzer;
	private FeatureDependencies featureDependencies;
	private List<Feature> coreFeatures;

	
	FeatureIDEModelInfo(){
		
	}
	
	FeatureIDEModelInfo(FeatureModel fm){
		featureModel = fm;
		featureModelAnalyzer = fm.getAnalyser();
		featureDependencies = featureModelAnalyzer.getDependencies();
	}
	
	private LinkedList<Feature> nameListToFeatureList(List<String> names){
		LinkedList<Feature> features = new LinkedList<Feature>();
		for (String featureName : names){
			features.add(featureModel.getFeature(featureName));
		}
		return features;
	}
	
	private Set<Feature> nameListToFeatureSet(List<String> names){
		Set<Feature> features = new HashSet<Feature>();
		for (String featureName : names){
			features.add(featureModel.getFeature(featureName));
		}
		return features;
	}
	
	private List<Feature> getCoreFeatures(){
		if (coreFeatures == null)
			coreFeatures = featureModelAnalyzer.getCoreFeatures();
		
		return coreFeatures;
	}
	
	private List<Set<Feature>> computeAllSubSets(Set<Feature> features){
		LinkedList<Set<Feature>> result = new LinkedList<Set<Feature>>();
		
		for (Feature feature : features){
			int setCount = result.size();
			for (int i = 0; i < setCount; i++){
				Set<Feature> newSet = new HashSet<Feature>();
				newSet.addAll(result.get(i));
				newSet.add(feature);
				result.add(newSet);
			}
			Set<Feature> featureSet = new HashSet<Feature>();
			featureSet.add(feature);
			result.add(featureSet);
		}
		
		return result;
	}
	
	/* (non-Javadoc)
	 * @see composer.rules.meta.FeatureModelInfo#hasValidProduct(java.util.List, java.util.List)
	 */
	@Override
	public boolean hasValidProduct(List<String> selectedFeatures, List<String> rejectedFeatures) {
		Set<Feature> selected = nameListToFeatureSet(selectedFeatures);
		List<Set<Feature>> rejected = new LinkedList<Set<Feature>>();
		try{
			// check if selectedt FEatures fit together
			if (!featureModelAnalyzer.exists(selected))
				return false;
			
			if (rejectedFeatures.isEmpty())
				return true;
			
			rejected.add(nameListToFeatureSet(rejectedFeatures));			
			
			boolean missing = featureModelAnalyzer.mayBeMissing(selected, rejected);
			
			return missing;
			
			// check if a Selected Feature implies a rejected
			
			
		} catch (TimeoutException te){
			return true;
		}
	}

	/* (non-Javadoc)
	 * @see composer.rules.meta.FeatureModelInfo#isObligatory(java.lang.String)
	 */
	@Override
	public boolean isObligatory(String featureName) {
		for (Feature feature : getCoreFeatures()){
			if (feature.getName().equals(featureName))
				return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see composer.rules.meta.FeatureModelInfo#isObligatoryForMethod(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean isObligatoryForMethod(String methodName, String featureName) {
		if (isObligatory(featureName))
			return true;
		
		return false;
	}

	/* (non-Javadoc)
	 * @see composer.rules.meta.FeatureModelInfo#isFeatureImplied(java.lang.String, java.util.List, java.util.List)
	 */
	@Override
	public boolean isFeatureImplied(String featureName,
			List<String> selectedFeatures, List<String> rejectedFeatures) {
		
		if (selectedFeatures.isEmpty() && rejectedFeatures.isEmpty())
			return false;
			
		List<Set<Feature>> selectedSets = computeAllSubSets(nameListToFeatureSet(selectedFeatures));
		Set<Feature> implied = new HashSet<Feature>();
		implied.add(featureModel.getFeature(featureName));
		
		for (Set<Feature> featureSet : selectedSets){
			try{
				if (featureModelAnalyzer.checkImplies(featureSet, implied))
					return true;
			} catch (TimeoutException t){
				
			}
		}
		
		// TODO: check whether the absence of a Feature or a Number of Features implies
		//       that the Feature featureName has to be selected (e.g. because all other Features of an alternative are
		//       rejected (then the Parent must be selected) )
		
		return false;
	}

	/* (non-Javadoc)
	 * @see composer.rules.meta.FeatureModelInfo#isNotFeatureImplied(java.lang.String, java.util.List, java.util.List)
	 */
	@Override
	public boolean isNotFeatureImplied(String featureName,
			List<String> selectedFeatures, List<String> rejectedFeatures) {

		if (selectedFeatures.isEmpty() && rejectedFeatures.isEmpty())
			return false;
		
		Feature feature = featureModel.getFeature(featureName); 

		
		// always(featureName,rejectedFeature) -> true
		for (Feature rejectedFeature : nameListToFeatureSet(rejectedFeatures)){
			if (featureDependencies.isAlways(feature, rejectedFeature))
				return true;
		}
		
		// never(selectedFeature,featureName)  -> true
		for (Feature selectedFeature : nameListToFeatureSet(selectedFeatures)){
			if (featureDependencies.never(selectedFeature).contains(feature))
				return true;
		}
		
		return false;
	}

}
