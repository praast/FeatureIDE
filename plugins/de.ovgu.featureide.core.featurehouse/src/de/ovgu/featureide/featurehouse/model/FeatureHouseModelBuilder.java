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
package de.ovgu.featureide.featurehouse.model;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;

import de.ovgu.cide.fstgen.ast.FSTNode;
import de.ovgu.cide.fstgen.ast.FSTNonTerminal;
import de.ovgu.cide.fstgen.ast.FSTTerminal;
import de.ovgu.cide.fstgen.ast.FSTVisitor;
import de.ovgu.featureide.core.IFeatureProject;
import de.ovgu.featureide.core.fstmodel.FSTClassFragment;
import de.ovgu.featureide.core.fstmodel.FSTFeature;
import de.ovgu.featureide.core.fstmodel.FSTModel;
import de.ovgu.featureide.core.fstmodel.FSTRole;
import de.ovgu.featureide.featurehouse.FeatureHouseComposer;

/**
 * This builder builds the {@link FSTModel} for FeatureHouse projects, 
 * by parsing the FeatureHouse internal FSTModel.
 * 
 * @author Jens Meinicke
 */
public class FeatureHouseModelBuilder implements FHNodeTypes {

	private FSTModel model;

	private IFeatureProject featureProject;

	private LinkedList<FSTClassFragment> classFragmentStack = new LinkedList<FSTClassFragment>();
	private FSTRole currentRole = null;
	private IFile currentFile = null;

	boolean completeModel = false;

	private FSTFeature currentFeature;

	public FeatureHouseModelBuilder(IFeatureProject featureProject) {
		if (featureProject == null) {
			return;
		}
		model = new FSTModel(featureProject);
		featureProject.setFSTModel(model);
		this.featureProject = featureProject;
	}
	
	public IFile getCurrentFile() {
		return currentFile;
	}
	
	public FSTRole getCurrentRole() {
		return currentRole;
	}
	
	public FSTClassFragment getCurrentClassFragment() {
		FSTClassFragment currentClassFragment =  classFragmentStack.peek();
		return (currentClassFragment == null) ? currentRole : classFragmentStack.peek();
	}
	
	public boolean hasCurrentClassFragment() {
		return currentRole != null || classFragmentStack.peek() != null;
	}

	/**
	 * Builds the model out of the FSTNodes of the FeatureHouse composer
	 * @param nodes The fstNodes
	 * @param completeModel <code>true</code> for completions mode: old methods will not be overwritten
	 */
	@SuppressWarnings("unchecked")
	public void buildModel(ArrayList<FSTNode> nodes, boolean completeModel) {

		this.completeModel = completeModel;
		if (!completeModel) {
			model.reset();
		}
		
		for (FSTNode node : (ArrayList<FSTNode>)nodes.clone()) {
			if (NODE_TYPE_FEATURE.equals(node.getType())) {
				caseAddFeature(node);
			} else if (NODE_TYPE_CLASS.equals(node.getType())) {
				caseAddClass(node);
			} else if(NODE_COMPILATIONUNIT.equals(node.getType())){
				caseCompileUnit(node);
			} else if (JAVA_NODE_CLASS_DECLARATION.equals(node.getType())) {
				caseClassDeclaration(node);
			} else if (C_NODE_SEQUENCE_CODEUNIT_TOPLEVEL.equals(node.getType())) {
				caseClassDeclaration(node);
			} else if (CSHARP_NODE_CLASS_MEMBER_DECLARATION.equals(node.getType())) {
				caseClassDeclaration(node);
			} else if (HASKELL_NODE_DEFINITIONS.equals(node.getType())) {
				caseClassDeclaration(node);
			} else if (HASKELL_NODE_DATA_DECLARATION.equals(node.getType())) {
				caseClassDeclaration(node);
			}
		}
	}
	
	private void caseCompileUnit(FSTNode node) {
		node.accept(new FSTVisitor(){
			 public boolean visit(FSTTerminal terminal){
				 if (JAVA_NODE_IMPORTDECLARATION.equals(terminal.getType())) {
					ClassBuilder.getClassBuilder(FeatureHouseModelBuilder.this.currentFile, FeatureHouseModelBuilder.this)
						.caseAddImport(terminal);
				 } else if(JAVA_NODE_PACKAGEDECLARATION.equals(terminal.getType())){
					 ClassBuilder.getClassBuilder(FeatureHouseModelBuilder.this.currentFile, FeatureHouseModelBuilder.this)
						.casePackage(terminal);
				 }
				return true;
			 }
		});	
	}
	
	private void caseAddFeature(FSTNode node) {
		currentFeature = model.addFeature(node.getName());
	}
	
	private void caseAddClass(FSTNode node) {
		String name = node.getName();
		String className = name.substring(
				name.lastIndexOf(File.separator) + 1);
		currentFile = getFile(name);
		if (!canCompose()) {
			return;
		}
		currentRole = model.addRole(currentFeature.getName(), className, currentFile);
		classFragmentStack.clear();
		classFragmentStack.push(currentRole);
	}

	private boolean canCompose() {
		return  FeatureHouseComposer.EXTENSIONS
				.contains(currentFile.getFileExtension()) &&
				currentFile.exists();
	}

	private void caseClassDeclaration(FSTNode node) {
		if (node instanceof FSTNonTerminal && canCompose()) {
			List<FSTNode> children = ((FSTNonTerminal) node).getChildren();
			for (FSTNode child : children) {
				String type = child.getType();
				ClassBuilder classBuilder = ClassBuilder.getClassBuilder(currentFile, this);
				
				if (child instanceof FSTTerminal) {
					FSTTerminal terminal = (FSTTerminal) child;
					if (JAVA_NODE_DECLARATION_TYPE1.equals(type)
							|| JAVA_NODE_DECLARATION_TYPE2.equals(type)) {
						classBuilder.caseClassDeclarationType(terminal);
					} else if (JAVA_NODE_MODIFIERS.equals(type)) {
						classBuilder.caseModifiers(terminal);
					} else if (JAVA_NODE_EXTENDSLIST.equals(type)) {
						classBuilder.caseExtendsList(terminal);
					} else if (JAVA_NODE_IMPLEMENTATIONLIST.equals(type)) {
						classBuilder.caseImplementsList(terminal);
					} else if (JAVA_NODE_FIELD.equals(type)) {
						classBuilder.caseFieldDeclaration(terminal);
					} else if (JAVA_NODE_FIELD_2.equals(type)) {
						classBuilder.caseFieldDeclaration(terminal);
					} else if (JAVA_NODE_FIELD_3.equals(type)) {
						classBuilder.caseFieldDeclaration(terminal);
					} else if (JAVA_NODE_METHOD.equals(type)) {
						classBuilder.caseMethodDeclaration(terminal);
					} else if (JAVA_NODE_CONSTRUCTOR.equals(type)) {
						classBuilder.caseConstructorDeclaration(terminal);
					} else if (C_NODE_FUNC.equals(type)) {
						classBuilder.caseMethodDeclaration(terminal);
					} else if (C_NODE_STATEMENT.equals(type)) {
						classBuilder.caseFieldDeclaration(terminal);
					} else if (C_NODE_STMTL.equals(type)) {
						classBuilder.caseFieldDeclaration(terminal);
					} else if (CSHARP_NODE_CLAASS_MEMBER_DECLARATION_END
							.equals(type)) {
						if (CSHARP_NODE_COMPOSITON_METHOD.equals(terminal
								.getCompositionMechanism())) {
							classBuilder.caseMethodDeclaration(terminal);
						} else if (CSHARP_NODE_COMPOSITION_FIELD
								.equals(terminal.getCompositionMechanism())) {
							classBuilder.caseFieldDeclaration(terminal);
						} else if (CSHARP_NODE_COMPOSITION_CONSTRUCTOR
								.equals(terminal.getCompositionMechanism())) {
							classBuilder.caseConstructorDeclaration(terminal);
						}
					} else if (HASKELL_NODE_DECLARATION.equals(type)) {
						classBuilder.caseMethodDeclaration(terminal);
					} else if (HASKELL_NODE_SIMPLE_TYPE.equals(type)) {
						classBuilder.caseFieldDeclaration(terminal);
					} else if (JML_SPEC_CASE_SEQ.equals(type)) {
						classBuilder.caseJMLSpecCaseSeq(terminal);
					}
				} else if (child instanceof FSTNonTerminal) {
					if (JAVA_NODE_INNER_CLASS_TYPE.equals(type)) {
						String name = child.getName();
						String className = name.substring(name.lastIndexOf(File.separator) + 1);
						
						FSTClassFragment newFragment = new FSTClassFragment(className);
						classFragmentStack.peek().add(newFragment);
						classFragmentStack.push(newFragment);
					} else {
						classFragmentStack.push(classFragmentStack.peek());
					}
					caseClassDeclaration(child);
				}
			}
			FSTClassFragment curClassFragment = classFragmentStack.pop();
			if (classFragmentStack.isEmpty() 
					&& curClassFragment.getPackage() == null) {
//				curClassFragment.setPackage(currentFile.getParent().getName());
			}
		}
	}

	private IFile getFile(String name) {
		String projectName = featureProject.getProjectName();
		name = name.substring(name.indexOf(projectName + System.getProperty("file.separator") + featureProject.getSourceFolder().getName()) + projectName.length() + 1);
		return featureProject.getProject().getFile(new Path(name));
	}
}
