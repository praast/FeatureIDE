package br.ufal.ic.colligens.models;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.eclipse.cdt.core.model.CModelException;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IIncludeReference;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.ui.internal.util.BundleUtility;
import org.prop4j.Node;
import org.prop4j.NodeWriter;

import br.ufal.ic.colligens.activator.Colligens;
import de.fosd.typechef.FrontendOptions;
import de.fosd.typechef.FrontendOptionsWithConfigFiles;
import de.fosd.typechef.TypeChefFrontend;
import de.fosd.typechef.lexer.options.OptionException;
import de.ovgu.featureide.fm.core.FeatureModel;
import de.ovgu.featureide.fm.core.editing.NodeCreator;
import de.ovgu.featureide.fm.core.io.FeatureModelReaderIFileWrapper;
import de.ovgu.featureide.fm.core.io.UnsupportedModelException;
import de.ovgu.featureide.fm.core.io.xml.XmlFeatureModelReader;
import de.ovgu.featureide.fm.ui.FMUIPlugin;

@SuppressWarnings("restriction")
public class TypeChef {

	private IProject project;
	private FrontendOptions frontendOptions;
	private boolean isFinish = false;
	private List<FileProxy> fileProxies;

	private IProgressMonitor monitor = null;

	/**
	 * 
	 */
	private void prepareFeatureModel() {
		File inputFile = new File(project.getLocation().toOSString()
				+ System.getProperty("file.separator") + "model.xml");
		File outputFile = new File(Colligens.getDefault().getConfigDir()
				.getAbsolutePath()
				+ System.getProperty("file.separator") + "cnf.fm");
		BufferedWriter print = null;
		try {
			print = new BufferedWriter(new FileWriter(outputFile));
			FeatureModel fm = new FeatureModel();
			FeatureModelReaderIFileWrapper fmReader = new FeatureModelReaderIFileWrapper(
					new XmlFeatureModelReader(fm));
			fmReader.readFromFile(inputFile);
			Node nodes = NodeCreator.createNodes(fm.clone()).toCNF();
			StringBuilder cnf = new StringBuilder();
			cnf.append(nodes.toString(NodeWriter.javaSymbols));
			print.write(cnf.toString());
		} catch (FileNotFoundException e) {
			Colligens.getDefault().logError(e);
		} catch (UnsupportedModelException e) {
			Colligens.getDefault().logError(e);
		} catch (IOException e) {
			Colligens.getDefault().logError(e);
		} finally {
			if (print != null) {
				try {
					print.close();
				} catch (IOException e) {
					FMUIPlugin.getDefault().logError(e);
				}
			}
		}
	}

	/**
	 * @param fileProxy
	 * @throws OptionException
	 */
	private void start(FileProxy fileProxy) throws OptionException {

		ArrayList<String> paramters = new ArrayList<String>();

		paramters.add(fileProxy.getFileToAnalyse());

		paramters.add("--parserstatistics");
		paramters.add("--lexOutput");
		paramters.add(Colligens.getDefault().getConfigDir().getAbsolutePath()
				+ System.getProperty("file.separator") + "lexOutput.c");

		String typeChefPreference = Colligens.getDefault().getPreferenceStore()
				.getString("TypeChefPreference");

		paramters.add(typeChefPreference);

		if (Colligens.getDefault().getPreferenceStore()
				.getBoolean("USE_INCLUDES")) {
			// Project C includes
			ICProject project = CoreModel
					.getDefault()
					.getCModel()
					.getCProject(
							PlatformHeader.getFile(fileProxy.getFileReal())
									.getProject().getName());

			try {
				IIncludeReference includes[] = project.getIncludeReferences();
				for (int i = 0; i < includes.length; i++) {
					paramters.add("-I");
					paramters.add(includes[i].getElementName());
				}
			} catch (CModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			paramters.add("-h");
			paramters.add(Colligens.getDefault().getConfigDir()
					.getAbsolutePath()
					+ System.getProperty("file.separator")
					+ "projects"
					+ System.getProperty("file.separator")
					+ project.getProject().getName() + "_platform.h");

		}

		if (Colligens.getDefault().getPreferenceStore().getBoolean("USE_STUBS")) {
			paramters.add("-h");
			paramters.add(Colligens.getDefault().getConfigDir()
					.getAbsolutePath()
					+ System.getProperty("file.separator")
					+ "projects"
					+ System.getProperty("file.separator")
					+ project.getProject().getName() + "_stubs.h");
		}

		paramters.add("-w");

		if (Colligens.getDefault().getPreferenceStore()
				.getBoolean("FEATURE_MODEL")) {
			prepareFeatureModel(); // General processing options String
			paramters.add("--featureModelFExpr");
			paramters.add(Colligens.getDefault().getConfigDir()
					.getAbsolutePath()
					+ System.getProperty("file.separator") + "cnf.fm");
		}

		frontendOptions = new FrontendOptionsWithConfigFiles();

		frontendOptions.parseOptions((String[]) paramters
				.toArray(new String[paramters.size()]));

		frontendOptions.setPrintToStdOutput(false);
	}

	/**
	 * @return
	 */
	public boolean isFinish() {
		return isFinish;
	}

	/**
	 * @param resourceList
	 * @throws TypeChefException
	 */
	public void run(List<IResource> resourceList) throws TypeChefException {
		this.isFinish = false;

		fileProxies = resourceToFileProxy(resourceList);

		PlatformHeader platformHeader = new PlatformHeader();

		try {
			if (fileProxies.isEmpty()) {
				monitor = null;
				throw new TypeChefException("Not a valid file found C");
			}

			if (Colligens.getDefault().getPreferenceStore()
					.getBoolean("USE_INCLUDES")) {
				platformHeader.plarform(fileProxies.get(0).getResource()
						.getProject().getName());
			}
			if (Colligens.getDefault().getPreferenceStore()
					.getBoolean("USE_STUBS")) {
				// Monitor Update
				monitorSubTask("Generating stubs...");
				platformHeader.stubs(fileProxies.get(0).getResource()
						.getProject().getName());
			}

			for (FileProxy fileProxy : fileProxies) {
				// Monitor Update
				monitorWorked(1);
				monitorSubTask(fileProxy.getFullPath());
				// end Monitor
				if (monitorIsCanceled()) {
					this.isFinish = true;
					break;
				}

				try {

					this.start(fileProxy);

					TypeChefFrontend typeChefFrontend = new TypeChefFrontend();

					typeChefFrontend.processFile(frontendOptions, fileProxy);

					this.isFinish = true;
				} catch (Exception e) {
					e.printStackTrace();
					// If the analysis is not performed correctly,
					// and the analysis made ​​from the command line
					this.startCommandLineMode(fileProxy);

					this.isFinish = true;
				}

			}
		} catch (PlatformException e1) {
			monitor = null;
			e1.printStackTrace();
			Colligens.getDefault().logError(e1);
		}
		monitor = null;
	}

	/**
	 * @param list
	 * @return
	 */
	private List<FileProxy> resourceToFileProxy(List<IResource> list) {
		List<FileProxy> fileProxies = new LinkedList<FileProxy>();
		// pega um dos arquivos para descobrir qual projeto esta sendo
		// verificado...
		if (project == null && !list.isEmpty()) {
			project = list.get(0).getProject();
			// System.err.println(project.toString());
		}
		for (IResource resouce : list) {
			FileProxy fileProxy = new FileProxy(resouce);
			fileProxies.add(fileProxy);
		}

		return fileProxies;
	}

	/**
	 * @return
	 */
	public List<FileProxy> getFilesLog() {
		List<FileProxy> list = new LinkedList<FileProxy>();

		for (FileProxy fileProxy : fileProxies) {
			if (!fileProxy.getLogs().isEmpty()) {
				list.add(fileProxy);
			}
		}
		return list;
		// return xmlParser.getLogs();
	}

	/**
	 * @param fileProxy
	 * @throws TypeChefException
	 */
	private void startCommandLineMode(FileProxy fileProxy)
			throws TypeChefException {
		XMLParserTypeChef xmlParser = new XMLParserTypeChef();

		ArrayList<String> args = new ArrayList<String>();
		args.add(fileProxy.getFileToAnalyse());

		String typeChefPreference = Colligens.getDefault().getPreferenceStore()
				.getString("TypeChefPreference");

		URL url = BundleUtility.find(Colligens.getDefault().getBundle(), "lib/"
				+ "TypeChef-0.3.5.jar");
		try {
			url = FileLocator.toFileURL(url);
		} catch (IOException e) {
			Colligens.getDefault().logError(e);
		}
		Path pathToTypeChef = new Path(url.getFile());

		if (Colligens.getDefault().getPreferenceStore()
				.getBoolean("FEATURE_MODEL")) {
			prepareFeatureModel(); // General processing options String
			args.add(0, Colligens.getDefault().getConfigDir().getAbsolutePath()
					+ System.getProperty("file.separator") + "cnf.fm");
			args.add(0, "--featureModelFExpr");
		}

		if (Colligens.getDefault().getPreferenceStore()
				.getBoolean("USE_INCLUDES")) {
			// // Project C includes
			ICProject project = CoreModel
					.getDefault()
					.getCModel()
					.getCProject(
							PlatformHeader.getFile(fileProxy.getFileReal())
									.getProject().getName());

			try {
				IIncludeReference includes[] = project.getIncludeReferences();
				for (int i = 0; i < includes.length; i++) {
					args.add(0, includes[i].getElementName());
					args.add(0, "-I");
				}
			} catch (CModelException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			args.add(0, Colligens.getDefault().getConfigDir().getAbsolutePath()
					+ System.getProperty("file.separator") + "projects"
					+ System.getProperty("file.separator")
					+ project.getProject().getName() + "_platform.h");
			args.add(0, "-h");
		}

		if (Colligens.getDefault().getPreferenceStore().getBoolean("USE_STUBS")) {
			args.add(0, Colligens.getDefault().getConfigDir().getAbsolutePath()
					+ System.getProperty("file.separator") + "projects"
					+ System.getProperty("file.separator")
					+ project.getProject().getName() + "_stubs.h");
			args.add(0, "-h");
		}

		args.add(0, typeChefPreference);

		// saved in the' temp directory
		String outputFilePath = Colligens.getDefault().getConfigDir()
				.getAbsolutePath()
				+ System.getProperty("file.separator") + "output";

		try {
			RandomAccessFile arq = new RandomAccessFile(outputFilePath, "rw");
			arq.close();
			arq = new RandomAccessFile(outputFilePath + ".xml", "rw");
			arq.close();
		} catch (Exception e) {
			e.printStackTrace();
		}

		args.add(0, "--errorXML=" + outputFilePath + ".xml");

		args.add(0, Colligens.getDefault().getConfigDir().getAbsolutePath()
				+ System.getProperty("file.separator") + "lexOutput.c");
		args.add(0, "--lexOutput");
		args.add(0, "--lexNoStdout");
		args.add(0, "-w");
		args.add(0, pathToTypeChef.toOSString());
		args.add(0, "-jar");
		args.add(0, "java");

		for (String s : args) {
			System.err.print(s + " ");
		}

		ProcessBuilder processBuilder = new ProcessBuilder(args);

		BufferedReader input = null;
		BufferedReader error = null;
		try {
			Process process = processBuilder.start();
			input = new BufferedReader(new InputStreamReader(
					process.getInputStream(), Charset.availableCharsets().get(
							"UTF-8")));
			error = new BufferedReader(new InputStreamReader(
					process.getErrorStream(), Charset.availableCharsets().get(
							"UTF-8")));
			boolean x = true;
			while (x) {
				try {
					String line;
					while ((line = input.readLine()) != null) {
						System.out.println(line);
						Colligens.getDefault().logWarning(line);
					}
					try {
						process.waitFor();
					} catch (InterruptedException e) {
						System.out.println(e.toString());
						Colligens.getDefault().logError(e);
					}
					int exitValue = process.exitValue();
					if (exitValue != 0) {
						throw new TypeChefException(
								"TypeChef did not run correctly.");
					}
					x = false;
				} catch (IllegalThreadStateException e) {
					System.out.println(e.toString());
					Colligens.getDefault().logError(e);
				}
			}
		} catch (IOException e) {
			System.out.println(e.toString());
			Colligens.getDefault().logError(e);
			throw new TypeChefException("TypeChef did not run correctly.");
		} finally {
			try {
				if (input != null)
					input.close();
			} catch (IOException e) {
				Colligens.getDefault().logError(e);
			} finally {
				if (error != null)
					try {
						error.close();
					} catch (IOException e) {
						Colligens.getDefault().logError(e);
					}
			}
		}
		xmlParser.setFile(fileProxy);
		xmlParser.processFile();
	}

	public void setMonitor(IProgressMonitor monitor) {
		this.monitor = monitor;
	}

	private boolean monitorIsCanceled() {
		return monitor != null ? monitor.isCanceled() : false;
	}

	private void monitorWorked(int value) {
		if (monitor == null)
			return;
		monitor.worked(value);
	}

	private void monitorSubTask(String label) {
		if (monitor == null)
			return;
		monitor.subTask(label);
	}

}
