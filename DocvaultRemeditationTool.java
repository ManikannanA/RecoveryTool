package net.nwie.awdtool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

public class DocvaultRemeditationTool {

	private static Logger log = Logger.getLogger(DocvaultRemeditationTool.class);

	private static final String sourceFilePath = "\\\\ohcolnas0220.nwie.net\\awd_input_test\\AWD\\docvault\\recovery";
	private static final String successFilePath = "\\\\ohcolnas0220.nwie.net\\awd_input_test\\AWD\\docvault\\success";
	private static final String searchFilePath = "\\\\ohcolnas0220.nwie.net\\awd_input_test\\AWD\\server\\success";
	private static final String processFilePath = "\\\\ohcolnas0220.nwie.net\\awd_input_test\\AWD\\docvault";

	/*
	  // production Path 
	   public static final String sourceFilePath = "\\\\ohlewnas0220\\awd_input_prod\\docvault\\recovery"; private static final
	   public static final String successFilePath =  "\\\\ohlewnas0220\\awd_input_prod\\docvault\\success"; private static final
	   public static final String searchFilePath =  "\\\\ohlewnas0220.nwideweb.net\\awd_input_prod\\AWFUL1599\\success"; private
	   public static final String processFilePath = "\\\\ohlewnas0220\\awd_input_prod\\docvault";
	   
	 */

	public static void main(String[] args) throws Exception {

		String path = System.getProperty("user.dir");
		PropertyConfigurator.configure(path + "\\log4j.properties");

		File sourceDir = new File(sourceFilePath);

		File[] nameFilterfiles = sourceDir.listFiles(nameFilter);
		moveFile(nameFilterfiles, successFilePath, false);
		log.info("csr-void files successfully moved to success folder ");

		File[] sizeFilterfiles = sourceDir.listFiles(sizeFilter);
		moveFile(sizeFilterfiles, successFilePath, false);
		log.info("File Size less than 1KB or equivalent files successfully moved to success folder ");

		File[] searchFilterfiles = sourceDir.listFiles(searchFilter);
		searchFile(searchFilterfiles);
		log.info("Files searching is successfully done in 1599 success folder ");

		File[] reprocessFiles = sourceDir.listFiles(processFilter);
		moveFile(reprocessFiles, processFilePath, true);
		log.info("Recovering process is completed... ");
	}

	private static void searchFile(File[] files) throws Exception {
		if (files.length == 0) {
			log.info("There is no files to process in 1599 Server...");
		} else {
			for (File aFile : files) {
				try {
					if (aFile.getName().contains("fn-awd")) {
						log.info("Started processing a file " + aFile.getName());
						File searchDir = new File(searchFilePath);
						File[] searchFilterfiles = searchDir.listFiles();
						log.info("Filtering files length " + searchFilterfiles.length);
						try {
							deleteLine(aFile.getAbsolutePath(), searchFilterfiles);
						} catch (Exception e) {
							throw new Exception("Exception while deleting a line in file: " + e.getMessage());
						}
					}
				} catch (Exception e) {
					throw new Exception("Exception while processing docvault file: " + e.getMessage());
				}
			}
		}
	}

	public static void deleteLine(String fileName, File[] searchFilterfiles) {
		try {
			BufferedReader file = new BufferedReader(new FileReader(fileName));
			String line;
			String input = "";
			while ((line = file.readLine()) != null) {
				log.info("Reading a Line in a file: " + line);
				Pattern p = Pattern.compile(".*\\{ *(.*) *\\}.*");
				Matcher m = p.matcher(line);
				m.find();
				String guidString = m.group(1);
				log.debug("GUID String: " + guidString);
				for (File xmlFile : searchFilterfiles) {
					String guidMatchContents = convertDocumentToString(xmlFile);
					if (guidMatchContents.contains(guidString)) {
						line = "";
						log.info("Matched line is deleted from the file...");
					}
				}
				if (line != "") {
					input += line + '\n';
				}
				FileOutputStream outStream = new FileOutputStream(fileName);
				outStream.write(input.getBytes());
				outStream.close();
			}
			file.close();
		} catch (Exception e) {
			log.info("Exception while reading a file: " + e.getMessage());
		}
	}

	private static String convertDocumentToString(File file)
			throws ParserConfigurationException, SAXException, IOException {

		// an instance of factory that gives a document builder
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		// an instance of builder to parse the specified xml file
		DocumentBuilder db = dbf.newDocumentBuilder();
		Document doc = db.parse(file);

		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer transformer;
		try {
			transformer = tf.newTransformer();

			StringWriter writer = new StringWriter();
			transformer.transform(new DOMSource(doc), new StreamResult(writer));
			String output = writer.getBuffer().toString();
			return output;
		} catch (TransformerException e) {
			log.info("Exception while converting Document to Stringfile: " + e.getMessage());
		}
		return null;
	}

	private static void moveFile(File[] files, String filePath, boolean fileRename) {
		File destDir = new File(filePath);
		if (files.length == 0) {
			log.info("There are no files to move from recovery folder!.");
		} else {
			for (File aFile : files) {
				try {
					if (!destDir.exists()) {
						destDir.mkdir();
					}
					String destFilename = fileRename ? aFile.getName().substring(16, 38) + "_Offshore"
							: aFile.getName();
					aFile.renameTo(new File(destDir + "\\" + destFilename));
					log.debug("File Name and length " + aFile.getName() + " - " + aFile.length());
				} catch (Exception e) {
					log.info("Exception while moving a file: " + e.getMessage());
				}

			}
		}
	}

	static FilenameFilter nameFilter = new FilenameFilter() {
		public boolean accept(File file, String name) {
			log.info("Name Filter filter is processing....");
			if (name.contains("csr-void")) {
				// filters files whose extension is csr-void
				return true;
			} else {
				return false;
			}
		}
	};

	static FileFilter sizeFilter = new FileFilter() {
		public boolean accept(File file) {
			log.info("Size Filter files is processing....");
			if (file.isFile() && file.length() <= 1 * 1024) {
				// filters files whose size less than or equal to 1MB
				return true;
			} else {
				return false;
			}
		}
	};

	static FileFilter searchFilter = new FileFilter() {
		public boolean accept(File file) {
			log.info("Search Filter files is processing....");
			if (file.isFile() && file.length() > 1 * 1024) {
				// filters files whose size more than or equal to 1MB
				return true;
			} else {
				return false;
			}
		}
	};

	static FilenameFilter processFilter = new FilenameFilter() {
		public boolean accept(File file, String name) {
			log.info("Process Filter files is processing....");
			if (name.contains("fn-awd")) {
				// filters files whose extension is csr-void
				return true;
			} else {
				return false;
			}
		}
	};
}
