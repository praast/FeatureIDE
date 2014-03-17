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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import org.prop4j.NodeWriter;

import composer.rules.meta.FeatureModelInfo;
import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTNonTerminal;
import de.ovgu.featureide.fm.core.Feature;
import de.ovgu.featureide.fm.core.FeatureModel;
import de.ovgu.featureide.fm.core.configuration.Configuration;
import de.ovgu.featureide.fm.core.configuration.SelectableFeature;
import de.ovgu.featureide.fm.core.configuration.Selection;
import de.ovgu.featureide.fm.core.configuration.SelectionNotPossibleException;
import de.ovgu.featureide.fm.core.editing.NodeCreator;

/**
 * TODO description
 * 
 * @author Matthias Praast
 */
public class FeatureIDEModelInfo implements FeatureModelInfo {
	
	private FeatureModel featureModel;
	private Configuration currentConfig;
	private List<String> coreFeatureNames;
	private boolean validSelect = true;
	private boolean validReject = true;
	// (className, (methodName, featureName))
	private HashMap<String, HashMap<String, List<Feature>>> rootsForMethod = new HashMap<String, HashMap<String,List<Feature>>>();

	private boolean obligatory = true;
	private boolean obligatoryMethod = true;
	private boolean fm = true;
	
	FeatureIDEModelInfo(){
		
	}
	
	FeatureIDEModelInfo(FeatureModel fm){
		featureModel = fm;
		currentConfig = new Configuration(featureModel);
	}

	/* (non-Javadoc)
	 * @see composer.rules.meta.FeatureModelInfo#isObligatory(java.lang.String)
	 */
	@Override
	public boolean isCoreFeature(String featureName) {
		if (!obligatory)
			return false;
		if (coreFeatureNames == null){
			Configuration newConfig = new Configuration(featureModel);
			coreFeatureNames = new LinkedList<String>();
			for (Feature feature : newConfig.getSelectedFeatures())
				coreFeatureNames.add(feature.getName());
		}
		
		return coreFeatureNames.contains(featureName);
	}


	/* (non-Javadoc)
	 * @see composer.rules.meta.FeatureModelInfo#isObligatory(java.lang.String)
	 */
	@Override
	public boolean isCoreFeature(String featureName,boolean useSelection) {
		if (!obligatory)
			return false;
		if (!useSelection)
			return isCoreFeature(featureName);
		
		for (Feature feature : currentConfig.getSelectedFeatures())
			if (feature.getName().equals(featureName))
				return true;
		
		return false;
	}

	/* (non-Javadoc)
	 * @see composer.rules.meta.FeatureModelInfo#isObligatoryForMethod(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean isMethodCoreFeature(String className, String methodName, String featureName) {
		if (!obligatoryMethod)
			return false;
		HashMap<String, List<Feature>> methodFeatures = rootsForMethod.get(className);
		if (methodFeatures == null)
			return false;
		List<Feature> features = methodFeatures.get(methodName);
		if (features == null)
			return false;
		for (Feature rootFeature : features){
			if (!rootFeature.getName().equals(featureName)){
				Configuration config = new Configuration(featureModel);
				config.setManual(rootFeature.getName(), Selection.SELECTED);
				if (config.getSelectablefeature(featureName).getAutomatic() != Selection.SELECTED)
					return false;
			} 
		}

		return true;
		
	}

	/* (non-Javadoc)
	 * @see composer.rules.meta.FeatureModelInfo#isObligatoryForMethod(java.lang.String, java.lang.String)
	 */
	@Override
	public boolean isMethodCoreFeature(String className, String methodName, String featureName, boolean useSelection) {
		if (!obligatoryMethod)
			return false;
		if (!useSelection)
			return isMethodCoreFeature(className, methodName, featureName);
		
		HashMap<String, List<Feature>> methodFeatures = rootsForMethod.get(className);
		if (methodFeatures == null)
			return false;
		List<Feature> features = methodFeatures.get(methodName);
		if (features == null)
			return false;
		for (Feature rootFeature : features){
			if (!rootFeature.getName().equals(featureName)){
				Configuration config = new Configuration(featureModel);
				for (Feature feat : currentConfig.getSelectedFeatures())
					config.setManual(feat.getName(),Selection.SELECTED);
				for (Feature feat : currentConfig.getUnSelectedFeatures())
					config.setManual(feat.getName(),Selection.UNSELECTED);
				config.setManual(rootFeature.getName(), Selection.SELECTED);
				if (config.getSelectablefeature(featureName).getAutomatic() != Selection.SELECTED)
					return false;
			} 
		}

		return true;
	}


	/* (non-Javadoc)
	 * @see composer.rules.meta.FeatureModelInfo#selectFeature(java.lang.String)
	 */
	@Override
	public void selectFeature(String featureName) {
		if (!fm)
			return;
		try{
			currentConfig.setManual(featureName, Selection.SELECTED);
		} catch (SelectionNotPossibleException ex){
			validSelect = false;
		}
	}

	/* (non-Javadoc)
	 * @see composer.rules.meta.FeatureModelInfo#rejectFeature(java.lang.String)
	 */
	@Override
	public void eliminateFeature(String featureName) {
		if (!fm)
			return;
		try{
			currentConfig.setManual(featureName, Selection.UNSELECTED);	
		} catch (SelectionNotPossibleException ex){
			validReject = false;
		}	
	}

	/* (non-Javadoc)
	 * @see composer.rules.meta.FeatureModelInfo#resetSelections()
	 */
	@Override
	public void resetSelections() {
		if (!fm)
			return;
		for (Feature feature : currentConfig.getSelectedFeatures())
			currentConfig.setManual(feature.getName(), Selection.UNDEFINED);
		validSelect = true;
		
	}

	/* (non-Javadoc)
	 * @see composer.rules.meta.FeatureModelInfo#resetRejections()
	 */
	@Override
	public void resetEliminations() {
		if (!fm)
			return;
		for (Feature feature : currentConfig.getUnSelectedFeatures())
			currentConfig.setManual(feature.getName(), Selection.UNDEFINED);
		validReject = true;
	}

	/* (non-Javadoc)
	 * @see composer.rules.meta.FeatureModelInfo#reset()
	 */
	@Override
	public void reset() {
		if (!fm)
			return;
		currentConfig.resetValues();
		validSelect = true;
		validReject = true;
	}

	/* (non-Javadoc)
	 * @see composer.rules.meta.FeatureModelInfo#isValidSelection()
	 */
	@Override
	public boolean isValidSelection() {
		if (!fm)
			return true;
		return validSelect && validReject && currentConfig.number() > 0;
	}

	/* (non-Javadoc)
	 * @see composer.rules.meta.FeatureModelInfo#isSelectable(java.lang.String)
	 */
	@Override
	public boolean canBeSelected(String featureName) {
		if (!fm)
			return true;
		SelectableFeature feature = currentConfig.getSelectablefeature(featureName);
		Selection oldManual = feature.getManual();
		try{
			currentConfig.setManual(feature, Selection.SELECTED);
			currentConfig.setManual(feature, oldManual);
			return true;
		} catch (SelectionNotPossibleException ex){
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see composer.rules.meta.FeatureModelInfo#isRejectable(java.lang.String)
	 */
	@Override
	public boolean canBeEliminated(String featureName) {
		if (!fm)
			return true;
		SelectableFeature feature = currentConfig.getSelectablefeature(featureName);
		Selection oldManual = feature.getManual();
		try{
			currentConfig.setManual(feature, Selection.UNSELECTED);
			currentConfig.setManual(feature, oldManual);
			return true;
		} catch (SelectionNotPossibleException ex){
			return false;
		}
	}

	/* (non-Javadoc)
	 * @see composer.rules.meta.FeatureModelInfo#isSelectionImplied(java.lang.String)
	 */
	@Override
	public boolean isAlwaysSelected(String featureName) {
		if (!fm)
			return false;
		return currentConfig.getSelectablefeature(featureName).getSelection() == Selection.SELECTED;
	}

	/* (non-Javadoc)
	 * @see composer.rules.meta.FeatureModelInfo#isRejectionImplied(java.lang.String)
	 */
	@Override
	public boolean isAlwaysEliminated(String featureName) {
		if (!fm)
			return false;
		return currentConfig.getSelectablefeature(featureName).getSelection() == Selection.UNSELECTED;
	}

	/* (non-Javadoc)
	 * @see composer.rules.meta.FeatureModelInfo#addFeatureNodes(java.util.List)
	 */
	@Override
	public void addFeatureNodes(List<FSTNonTerminal> features) {
		if (!obligatoryMethod)
			return;
		rootsForMethod = new HashMap<String, HashMap<String,List<Feature>>>();
		for (FSTNonTerminal featureNode : features){
			String featureName = getFeatureName(featureNode);
			
			// get all method-Nodes
			for (FSTNode methodNode : getMethodNodes(featureNode)){
				String className = getClassName(methodNode);
				String methodName = getMethodName(methodNode);
				
				HashMap<String, List<Feature>> methodFeature = rootsForMethod.get(className);
				if (methodFeature == null) {
					methodFeature = new HashMap<String, List<Feature>>();
					rootsForMethod.put(className, methodFeature);
				}
				
				List<Feature> featureList = methodFeature.get(methodName);
				if (featureList == null){
					featureList = new LinkedList<Feature>();
					methodFeature.put(methodName, featureList);
				}
				
				if (!featureList.contains(featureName))
					addToFeatureList(featureModel.getFeature(featureName),featureList);
			}
		}
	}

	/**
	 * @param methodNode
	 * @return
	 */
	private String getMethodName(FSTNode methodNode) {
		if (methodNode == null)
			return "";
		if (methodNode.getType().contains("MethodDeclaration")){
			String name = methodNode.getName();
			if (name.contains("(")){
				name = name.substring(0,name.indexOf("(")).trim();
			}
			return name; 
		}
		return getMethodName(methodNode.getParent());
	}

	/**
	 * @param methodNode
	 * @return
	 */
	private String getClassName(FSTNode methodNode) {
		if (methodNode == null)
			return "";
		if (methodNode.getType().contains("ClassDeclaration"))
			return methodNode.getName();
		return getClassName(methodNode.getParent());
	}

	/**
	 * @param featureNode
	 * @return
	 */
	private List<FSTNode> getMethodNodes(FSTNode featureNode) {
		LinkedList<FSTNode> result = new LinkedList<FSTNode>();
		if (featureNode == null)
			return result;
		if (featureNode.getType().contains("MethodDeclaration")) {
			result.add(featureNode);
			return result;
		}
		if (!(featureNode instanceof FSTNonTerminal))
			return result;
		
		for (FSTNode child : ((FSTNonTerminal)featureNode).getChildren())
			result.addAll(getMethodNodes(child));
		
		return result;
	}

	/**
	 * @param featureNode
	 * @return
	 */
	private String getFeatureName(FSTNode featureNode) {
		return featureNode.getName();
	}

	/* (non-Javadoc)
	 * @see composer.rules.meta.FeatureModelInfo#clearFeatureNodes()
	 */
	@Override
	public void clearFeatureNodes() {
		rootsForMethod.clear();
	}
	
	private void addToFeatureList(Feature feature, List<Feature> featureList){
		if (!featureList.contains(feature))
			featureList.add(feature);
	}

	/* (non-Javadoc)
	 * @see composer.rules.meta.FeatureModelInfo#getValidClause()
	 */
	@Override
	public String getValidClause() {
		String clause = NodeCreator.createNodes(featureModel.clone()).toCNF().toString(NodeWriter.javaSymbols).toLowerCase(Locale.ENGLISH);
		for (String feature : featureModel.getFeatureNames())
			clause = clause.replaceAll(feature.toLowerCase(), "FM.FeatureModel." + feature.toLowerCase());
		return clause;
	}

}
