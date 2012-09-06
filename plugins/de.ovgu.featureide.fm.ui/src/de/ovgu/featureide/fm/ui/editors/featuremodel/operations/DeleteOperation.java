/* FeatureIDE - An IDE to support feature-oriented software development
 * Copyright (C) 2005-2012  FeatureIDE team, University of Magdeburg
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
package de.ovgu.featureide.fm.ui.editors.featuremodel.operations;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.operations.AbstractOperation;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.gef.ui.parts.GraphicalViewerImpl;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import de.ovgu.featureide.fm.core.Constraint;
import de.ovgu.featureide.fm.core.ConstraintAttribute;
import de.ovgu.featureide.fm.core.Feature;
import de.ovgu.featureide.fm.core.FeatureDependencies;
import de.ovgu.featureide.fm.core.FeatureModel;
import de.ovgu.featureide.fm.ui.FMUIPlugin;
import de.ovgu.featureide.fm.ui.editors.DeleteOperationAlternativeDialog;
import de.ovgu.featureide.fm.ui.editors.featuremodel.GUIDefaults;
import de.ovgu.featureide.fm.ui.editors.featuremodel.editparts.ConstraintEditPart;
import de.ovgu.featureide.fm.ui.editors.featuremodel.editparts.FeatureEditPart;
import de.ovgu.featureide.fm.ui.editors.featuremodel.editparts.LegendEditPart;

/**
 * Operation with functionality to delete multiple elements from the feature
 * model editor. Enables Undo/Redo.
 * 
 * @author Fabian Benduhn
 */
public class DeleteOperation extends AbstractOperation implements GUIDefaults {

	
	public final static String[] textualSymbols = new String[] { "iff",
		"implies", "or", "and", "not" };
	
	private static final String LABEL = "Delete";
	private Object viewer;
	private FeatureModel featureModel;
	private List<AbstractOperation> operations;
	private HashMap<Feature, List<Feature>> removalMap;
	/**
	 * 
	 */
	public DeleteOperation(Object viewer, FeatureModel featureModel) {
		super(LABEL);
		this.viewer = viewer;
		this.featureModel = featureModel;
		this.operations = new LinkedList<AbstractOperation>();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.commands.operations.AbstractOperation#execute(org.eclipse
	 * .core.runtime.IProgressMonitor, org.eclipse.core.runtime.IAdaptable)
	 */
	@Override
	public IStatus execute(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {

		doDelete();
		/*
		if (notDeleted.size() > 0)
		{
			Feature start =  notDeleted.getFirst();
			notDeleted.remove(start);
			FeatureDependencies fd = new FeatureDependencies(featureModel);
			List<Feature> equivalent = new LinkedList<Feature>();
			for (Feature f : notDeleted)
			{

				if (fd.always(f).contains(start) && fd.always(start).contains(f))
				{
					equivalent.add(f);
				}
			}
			notDeleted.removeAll(equivalent);
			if (notDeleted.isEmpty())
			{
				//Complete Atomic set
				List<Feature> atomicSet = new LinkedList<Feature>();
				for (Feature f2 : fd.always(start))
				{
					if (fd.always(f2).contains(start))
					{
						atomicSet.add(f2);
					}
				}
				equivalent.add(start);
				atomicSet.removeAll(equivalent);				
				
				HashMap<Feature, List<Feature>> s = new HashMap<Feature, List<Feature>>();
				for (Feature f : equivalent)
				{
					s.put(f, atomicSet);
				}
				new DeleteOperationAlternativeDialog(featureModel, s);				
			}
		}
		
		if (notDeleted.size() > 0)
		{
			String notDeletedFeatures = "";
			for (Feature f : notDeleted)
			{
				notDeletedFeatures += "\"" + f.getName() + "\", ";
					
			}
			notDeletedFeatures = notDeletedFeatures.substring(0, notDeletedFeatures.length() -2);
			MessageDialog dialog = new MessageDialog(new Shell(), 
					" Delete Error ", FEATURE_SYMBOL, 
					"" +  "The following features are contained in constraints:" +
					 "\n" + notDeletedFeatures + "\n" + 
					"Select only one feature in order to replace it with an equivalent one.",
					MessageDialog.ERROR, new String[] { IDialogConstants.OK_LABEL }, 0);
			dialog.open();
		}
*/		
		featureModel.handleModelDataChanged();
		return Status.OK_STATUS;
	}

	
	public void doDelete()
	{
		doDelete(null);
	}
	/**
	 *  
	 */
	public void doDelete(List<Feature> toDelete) {
		AbstractOperation op = null;
		
		IStructuredSelection selection;
		if (viewer instanceof GraphicalViewerImpl)
		selection = (IStructuredSelection) ((GraphicalViewerImpl) viewer).getSelection();
		else 
			selection = (IStructuredSelection) ((TreeViewer) viewer).getSelection();

		
		removalMap = new HashMap<Feature, List<Feature>>();
		Iterator<?> iter;	
/*		if (toDelete.isEmpty())
		{*/
			iter = selection.iterator();
/*		}else
		{
			iter = toDelete.iterator();
		}*/
		List<Feature> alreadyDeleted = new LinkedList<Feature>();
		while (iter.hasNext()) {
			
			op = null;
			
			Object editPart = iter.next();

			if (editPart instanceof ConstraintEditPart) {

				Constraint constraint = ((ConstraintEditPart) editPart).getConstraintModel();
				op = new ConstraintDeleteOperation(constraint, featureModel);
				for (Constraint resetConstraint : featureModel.getConstraints()){
					resetConstraint.setConstraintAttribute(ConstraintAttribute.NORMAL,true);
				}
				executeOperation(op);
			}
			if (editPart instanceof LegendEditPart) {
				op = new LegendHideOperation(featureModel);

				executeOperation(op);

			}
			if (editPart instanceof Feature){
				Feature feature = ((Feature) editPart);
				
				if (feature.getRelevantConstraints().isEmpty()) {
//////////////////////////////////////////////////////////////////////////////////////////					
					op = new FeatureDeleteOperation(featureModel, feature, true);
					
////////////////////////////////////////////////////////////////////////////////////////					
					executeOperation(op);
				} else {
					
					MessageDialog dialog = new MessageDialog(new Shell(), 
							" Delete Error ", FEATURE_SYMBOL, 
							"\"" + feature.getName() + "\" is contained in constraints. "
							+ '\n' + '\n' + 
							"Unable to delete this feature until all relevant constraints are removed.",
							MessageDialog.ERROR, new String[] { IDialogConstants.OK_LABEL }, 0);
					
					dialog.open();
				}				
			}
			if (editPart instanceof Constraint){
				Constraint constraint = ((Constraint) editPart);
				op = new ConstraintDeleteOperation(constraint, featureModel);

				executeOperation(op);
			}
			if (editPart instanceof FeatureEditPart) {
				Feature feature = ((FeatureEditPart) editPart).getFeature();
				
				
				if (feature.getRelevantConstraints().isEmpty()) {
					op = new FeatureDeleteOperation(featureModel, feature, true);
					executeOperation(op);
					alreadyDeleted.add(feature);
				} else {

						FeatureDependencies fd = new FeatureDependencies(featureModel);
						List<Feature> equivalent = new LinkedList<Feature>();
						for (Feature f2 : fd.always(feature))
						{
							if (fd.always(f2).contains(feature))
							{
								equivalent.add(f2);
							}
						}

						//removalMap = new HashMap<Feature, List<Feature>>();
						removalMap.put(feature, equivalent);
							//new DeleteOperationAlternativeDialog(featureModel, s);
				}
				

			}
			if (op != null) operations.add(op);
		}
		
		//Nur eins, keine Ersetzung m�glich
		
		if (!removalMap.isEmpty())
		{
			boolean hasDeletableFeature = false;
			List<Feature> notDeletable = new LinkedList<Feature>();
			List<Feature> toBeDeleted = new LinkedList<Feature>();
			Iterator<Entry<Feature, List<Feature>>> removalIterator = removalMap.entrySet().iterator();
	
			/*
			while (removalIterator.hasNext())
			{			 
				toBeDeleted.add(removalIterator.next().getKey());
			}*/
			toBeDeleted.addAll(removalMap.keySet());
			
			removalIterator = removalMap.entrySet().iterator();
			while (removalIterator.hasNext())
			{
				
				Entry<Feature, List<Feature>> entry = removalIterator.next();
				Feature feature =  entry.getKey();
				List<Feature> featureList = entry.getValue();		
				featureList.removeAll(toBeDeleted);
				featureList.removeAll(alreadyDeleted);
				if (featureList.isEmpty())
				{
					notDeletable.add(feature);
				}else
				{
					hasDeletableFeature = true;
				}
			}
			if (hasDeletableFeature)
			{
				new DeleteOperationAlternativeDialog(featureModel, removalMap);	
			}else
			{
				String notDeletedFeatures = "";
				for (Feature f : notDeletable)
				{
					notDeletedFeatures += "\"" + f.getName() + "\", ";
						
				}
				notDeletedFeatures = notDeletedFeatures.substring(0, notDeletedFeatures.length() -2);
				MessageDialog dialog = new MessageDialog(new Shell(), 
						" Delete Error ", FEATURE_SYMBOL, 
						"" +  "The following features are contained in constraints:" +
						 "\n" + notDeletedFeatures + "\n" + 
						"Select only one feature in order to replace it with an equivalent one.",
						MessageDialog.ERROR, new String[] { IDialogConstants.OK_LABEL }, 0);
				dialog.open();			
			}
		}
	}

	/**
	 * @param op
	 *            operation to be executed
	 */
	private void executeOperation(AbstractOperation op) {
		try {
			PlatformUI.getWorkbench().getOperationSupport()
					.getOperationHistory().execute(op, null, null);

		} catch (ExecutionException e) {
			FMUIPlugin.getDefault().logError(e);

		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.commands.operations.AbstractOperation#redo(org.eclipse
	 * .core.runtime.IProgressMonitor, org.eclipse.core.runtime.IAdaptable)
	 */
	@Override
	public IStatus redo(IProgressMonitor monitor, IAdaptable info)
			throws ExecutionException {

		List<AbstractOperation> ops = new LinkedList<AbstractOperation>();
		ops.addAll(operations);
		Collections.reverse(operations);
		while (!ops.isEmpty()) {
			for (AbstractOperation op : operations) {
				try {

					op.redo(monitor, info);
					ops.remove(op);

				} catch (Exception e) {

				}

			}
		}
		featureModel.handleModelDataChanged();
		featureModel.redrawDiagram();

		return Status.OK_STATUS;

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.eclipse.core.commands.operations.AbstractOperation#undo(org.eclipse
	 * .core.runtime.IProgressMonitor, org.eclipse.core.runtime.IAdaptable)
	 */
	@Override
	public IStatus undo(IProgressMonitor arg0, IAdaptable arg1)
			throws ExecutionException {
		List<AbstractOperation> ops = new LinkedList<AbstractOperation>();
		ops.addAll(operations);
		Collections.reverse(operations);
		while (!ops.isEmpty()) {
			for (AbstractOperation op : operations) {

				if (op.canUndo()) {
					op.undo(arg0, arg1);
					ops.remove(op);
				}
			}
		}
		featureModel.handleModelDataLoaded();
		return Status.OK_STATUS;
	}

}
